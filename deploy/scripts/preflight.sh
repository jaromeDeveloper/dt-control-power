#!/usr/bin/env bash

# Read-only deployment readiness check. It never downloads, builds, migrates,
# starts, stops, repairs or rewrites anything.

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="${DT_REPO_ROOT:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
ENV_FILE="${DT_ENV_FILE:-${REPO_ROOT}/deploy/.env}"
COMPOSE_FILE="${DT_COMPOSE_FILE:-${REPO_ROOT}/deploy/compose.yaml}"
BACKEND_SERVICE="${DT_BACKEND_SERVICE:-backend}"
EXPECTED_BRANCH="${DT_DEPLOY_BRANCH:-master}"
ERRORS=0
WARNINGS=0
CURRENT_BACKEND_CONTAINER=""

usage() {
  cat <<'USAGE'
Usage: preflight.sh [--env-file PATH] [--compose-file PATH] [--service NAME]

Performs read-only checks for the manually operated I8 deployment.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file) ENV_FILE="${2:?missing value for --env-file}"; shift 2 ;;
    --compose-file) COMPOSE_FILE="${2:?missing value for --compose-file}"; shift 2 ;;
    --service) BACKEND_SERVICE="${2:?missing value for --service}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'ERROR: unsupported argument: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

fail() { printf 'FAIL: %s\n' "$1" >&2; ERRORS=$((ERRORS + 1)); }
warn() { printf 'WARN: %s\n' "$1" >&2; WARNINGS=$((WARNINGS + 1)); }
pass() { printf 'PASS: %s\n' "$1"; }
have() { command -v "$1" >/dev/null 2>&1; }

read_env_value() {
  local key="$1"
  awk -F= -v wanted="${key}" '$1 == wanted {sub(/^[^=]*=/, ""); print; exit}' "${ENV_FILE}" 2>/dev/null || true
}

printf 'DT Control Power deployment preflight (read-only)\n'
printf 'timestamp_utc=%s\nrepo_root=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${REPO_ROOT}"

for command in git docker npm curl nginx systemctl stat df free ss; do
  if have "${command}"; then pass "command available: ${command}"; else fail "command missing: ${command}"; fi
done
if have sudo && sudo -n true >/dev/null 2>&1; then
  pass "passwordless sudo available for the manual deployment commands"
else
  warn "passwordless sudo unavailable; deploy will require an interactive sudo credential"
fi

if [[ ! -d "${REPO_ROOT}/.git" ]]; then
  fail "repository metadata not found at ${REPO_ROOT}"
else
  current_branch="$(git -C "${REPO_ROOT}" branch --show-current 2>/dev/null || true)"
  [[ "${current_branch}" == "${EXPECTED_BRANCH}" ]] \
    && pass "deployment branch is ${EXPECTED_BRANCH}" \
    || fail "expected branch ${EXPECTED_BRANCH}; observed ${current_branch:-detached}"
  if [[ -z "$(git -C "${REPO_ROOT}" status --porcelain 2>/dev/null)" ]]; then
    pass "Git worktree is clean"
  else
    fail "Git worktree is not clean"
  fi
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  fail "environment file not found: ${ENV_FILE}"
else
  mode="$(stat -c '%a' "${ENV_FILE}" 2>/dev/null || true)"
  if [[ "${mode}" == "600" || "${mode}" == "400" ]]; then
    pass "environment file permissions are ${mode}"
  else
    fail "environment file must use mode 600 or 400; observed ${mode:-unknown}"
  fi
  for key in DB_URL DB_USERNAME DB_PASSWORD DEMO_ADMIN_PASSWORD DEMO_OPERATOR_PASSWORD \
      DEMO_AUDITOR_PASSWORD POSTGRES_NETWORK POSTGRES_CONTAINER UPLOADS_HOST_PATH \
      JAVA_TOOL_OPTIONS APP_VERSION; do
    value="$(read_env_value "${key}")"
    if [[ -z "${value}" ]]; then
      fail "required environment key is missing or empty: ${key}"
    elif [[ "${value}" == *replace-me* || "${value}" == *CHANGE_ME* ]]; then
      fail "placeholder value remains for ${key}"
    fi
  done
  uploads_path="$(read_env_value UPLOADS_HOST_PATH)"
  if [[ "${uploads_path}" != /* ]]; then
    uploads_path="$(dirname "${COMPOSE_FILE}")/${uploads_path}"
  fi
  if [[ ! -d "${uploads_path}" ]]; then
    fail "upload bind directory must already exist: ${uploads_path}"
  else
    read -r upload_uid upload_gid upload_mode < <(stat -c '%u %g %a' "${uploads_path}" 2>/dev/null || printf '0 0 0')
    upload_mode_value=$((8#${upload_mode}))
    if (( (upload_uid == 10001 && (upload_mode_value & 0200) != 0) \
        || (upload_gid == 10001 && (upload_mode_value & 0020) != 0) \
        || (upload_mode_value & 0002) != 0 )); then
      pass "private upload bind directory is writable by container UID/GID 10001"
    else
      fail "upload bind directory is not writable by container UID/GID 10001: ${uploads_path}"
    fi
  fi
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  fail "Compose file not found: ${COMPOSE_FILE}"
elif have docker; then
  if docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" config --quiet >/dev/null 2>&1; then
    pass "Compose configuration parses without printing resolved secrets"
    mapfile -t services < <(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" config --services 2>/dev/null)
    if [[ ${#services[@]} -eq 1 && "${services[0]}" == "${BACKEND_SERVICE}" ]]; then
      pass "Compose manages only backend service ${BACKEND_SERVICE}"
    else
      fail "Compose must manage only ${BACKEND_SERVICE}; observed: ${services[*]:-none}"
    fi
    CURRENT_BACKEND_CONTAINER="$(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps -q "${BACKEND_SERVICE}" 2>/dev/null || true)"
  else
    fail "Compose configuration is invalid or unresolved"
  fi
fi

if [[ -f "${ENV_FILE}" ]] && have docker; then
  postgres_network="$(read_env_value POSTGRES_NETWORK)"
  postgres_container="$(read_env_value POSTGRES_CONTAINER)"
  if [[ -n "${postgres_network}" ]] && docker network inspect "${postgres_network}" >/dev/null 2>&1; then
    pass "external PostgreSQL network exists"
  else
    fail "external PostgreSQL network is unresolved"
  fi
  if [[ -n "${postgres_container}" ]] && docker inspect "${postgres_container}" >/dev/null 2>&1; then
    running="$(docker inspect --format '{{.State.Running}}' "${postgres_container}" 2>/dev/null || true)"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}not-configured{{end}}' "${postgres_container}" 2>/dev/null || true)"
    [[ "${running}" == "true" ]] && pass "PostgreSQL container is running" || fail "PostgreSQL container is not running"
    [[ "${health}" == "healthy" || "${health}" == "not-configured" ]] \
      && pass "PostgreSQL health is ${health}" \
      || fail "PostgreSQL health is ${health:-unknown}"

    db_url="$(read_env_value DB_URL)"
    db_username="$(read_env_value DB_USERNAME)"
    db_name="$(printf '%s' "${db_url}" | sed -E 's#^jdbc:postgresql://[^/]+/([^?]+).*$#\1#')"
    if [[ ! "${db_name}" =~ ^[A-Za-z_][A-Za-z0-9_]{0,62}$ \
        || ! "${db_username}" =~ ^[A-Za-z_][A-Za-z0-9_]{0,62}$ ]]; then
      fail "database name or username cannot be safely validated"
    else
      user_table_count="$(docker exec "${postgres_container}" psql -X -w -A -t \
        -U "${db_username}" -d "${db_name}" \
        -c "SELECT count(*) FROM pg_catalog.pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')" \
        2>/dev/null | tr -d '[:space:]' || true)"
      flyway_exists="$(docker exec "${postgres_container}" psql -X -w -A -t \
        -U "${db_username}" -d "${db_name}" \
        -c "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL" \
        2>/dev/null | tr -d '[:space:]' || true)"
      if [[ ! "${user_table_count}" =~ ^[0-9]+$ || ! "${flyway_exists}" =~ ^[tf]$ ]]; then
        fail "read-only Flyway/database inspection failed"
      elif [[ "${flyway_exists}" == "f" && "${user_table_count}" == "0" ]]; then
        pass "database is empty and ready for an explicitly authorized first Flyway migration"
      elif [[ "${flyway_exists}" == "f" ]]; then
        fail "database has application tables but no Flyway history; migration must stop"
      else
        failed_migrations="$(docker exec "${postgres_container}" psql -X -w -A -t \
          -U "${db_username}" -d "${db_name}" \
          -c "SELECT count(*) FROM public.flyway_schema_history WHERE NOT success" \
          2>/dev/null | tr -d '[:space:]' || true)"
        if [[ "${failed_migrations}" == "0" ]]; then
          pass "Flyway history exists without failed migrations; application startup must still validate checksums"
        else
          fail "Flyway history contains failed entries or could not be inspected"
        fi
      fi
      for version in 1 2 3 4 5; do
        compgen -G "${REPO_ROOT}/backend/src/main/resources/db/migration/V${version}__*.sql" >/dev/null \
          || fail "repository migration V${version} is missing"
      done
    fi
  else
    fail "PostgreSQL container is unresolved"
  fi
fi

if have nginx; then
  if sudo -n nginx -t >/dev/null 2>&1 || nginx -t >/dev/null 2>&1; then
    pass "currently installed NGINX configuration is valid"
  else
    warn "NGINX configuration could not be validated non-interactively; run sudo nginx -t manually"
  fi
fi

if have ss && ss -lnt 2>/dev/null | awk '{print $4}' | grep -Eq '(^|:)8080$'; then
  if [[ -n "${CURRENT_BACKEND_CONTAINER}" ]]; then
    pass "port 8080 is in use and the current Compose backend service exists"
  else
    fail "port 8080 is already listening and no current Compose backend service was identified"
  fi
else
  pass "port 8080 has no conflicting listener"
fi

available_kib="$(df -Pk "${REPO_ROOT}" 2>/dev/null | awk 'NR==2 {print $4}')"
if [[ "${available_kib:-0}" =~ ^[0-9]+$ && ${available_kib} -ge 2097152 ]]; then
  pass "at least 2 GiB is available on the repository filesystem"
else
  fail "less than 2 GiB is available or capacity is unknown"
fi

available_mem_kib="$(awk '/MemAvailable:/ {print $2}' /proc/meminfo 2>/dev/null || true)"
if [[ "${available_mem_kib:-0}" =~ ^[0-9]+$ && ${available_mem_kib} -ge 1048576 ]]; then
  pass "at least 1 GiB of memory is currently available"
else
  warn "less than 1 GiB of available memory or memory is unknown"
fi

if systemctl is-active --quiet oneagent 2>/dev/null || \
    systemctl list-units --type=service --state=active --no-legend 2>/dev/null | grep -Eqi '(oneagent|dynatrace)'; then
  pass "a Dynatrace/OneAgent service appears active"
else
  warn "OneAgent active state could not be confirmed"
fi

printf 'summary_errors=%d summary_warnings=%d\n' "${ERRORS}" "${WARNINGS}"
if (( ERRORS > 0 )); then
  printf 'VERDICT=BLOCKED\n' >&2
  exit 1
fi
printf 'VERDICT=READY_FOR_EXPLICITLY_AUTHORIZED_DEPLOYMENT\n'
