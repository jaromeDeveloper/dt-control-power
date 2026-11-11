#!/usr/bin/env bash

# DT Control Power I8.1 - Ubuntu inventory (read-only)
# Optional, non-secret selectors:
#   DT_PG_CONTAINER, DT_DB_NAME, DT_PG_ROLE, DT_REPO_ROOT
# This script never reads .env files or prints container environments.

set -u
set -o pipefail

readonly TARGET_DB="${DT_DB_NAME:-observability}"
readonly TARGET_ROLE="${DT_PG_ROLE:-observability}"
readonly REQUESTED_PG_CONTAINER="${DT_PG_CONTAINER:-}"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="${DT_REPO_ROOT:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
readonly MIGRATIONS_DIR="${REPO_ROOT}/backend/src/main/resources/db/migration"

if [[ ! "${TARGET_DB}" =~ ^[A-Za-z_][A-Za-z0-9_]{0,62}$ ]]; then
  echo "ERROR: DT_DB_NAME contains unsupported characters." >&2
  exit 2
fi
if [[ ! "${TARGET_ROLE}" =~ ^[A-Za-z_][A-Za-z0-9_]{0,62}$ ]]; then
  echo "ERROR: DT_PG_ROLE contains unsupported characters." >&2
  exit 2
fi

if sudo -n true >/dev/null 2>&1; then
  SUDO=(sudo -n)
else
  SUDO=()
fi

section() {
  printf '\n## %s\n' "$1"
}

command_available() {
  command -v "$1" >/dev/null 2>&1
}

safe_run() {
  local label="$1"
  shift
  printf '\n### %s\n' "${label}"
  if ! "$@"; then
    printf 'UNAVAILABLE: command failed or current user lacks read permission.\n'
  fi
}

section "Execution context"
printf 'timestamp_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
printf 'hostname=%s\n' "$(hostname 2>/dev/null || printf 'unknown')"
printf 'repo_root=%s\n' "${REPO_ROOT}"
printf 'effective_user=%s\n' "$(id -un 2>/dev/null || printf 'unknown')"
printf 'passwordless_sudo_for_read_commands=%s\n' "$([[ ${#SUDO[@]} -gt 0 ]] && printf 'yes' || printf 'no')"

section "Host resources and network"
safe_run "OS" bash -c 'source /etc/os-release 2>/dev/null; printf "name=%s\nversion=%s\n" "${PRETTY_NAME:-unknown}" "${VERSION_ID:-unknown}"'
safe_run "CPU" bash -c 'printf "logical_cpus=%s\n" "$(nproc)"; lscpu | grep -E "^(Model name|CPU\(s\)|Thread|Core|Socket|Virtualization):"'
safe_run "Memory and swap" free -h
if command_available swapon; then
  safe_run "Swap devices" "${SUDO[@]}" swapon --show --bytes
fi
safe_run "Filesystem capacity" df -hT -x tmpfs -x devtmpfs
safe_run "Filesystem inodes" df -hi -x tmpfs -x devtmpfs
if command_available ip; then
  safe_run "IP addresses" ip -brief address show
  safe_run "Routes" ip route show
fi
if command_available ufw; then
  safe_run "UFW" "${SUDO[@]}" ufw status verbose
else
  printf '\n### UFW\nUNAVAILABLE: ufw command not installed or not in PATH.\n'
fi
if command_available ss; then
  safe_run "Listening ports 80/443/8080/5432" bash -c "${SUDO[*]} ss -lntp 2>/dev/null | awk 'NR==1 || /:80([^0-9]|$)|:443([^0-9]|$)|:8080([^0-9]|$)|:5432([^0-9]|$)/'"
fi

section "Docker inventory"
if ! command_available docker; then
  printf 'BLOCKER: docker command is unavailable.\n'
  DOCKER_AVAILABLE=false
else
  DOCKER_AVAILABLE=true
  safe_run "Docker versions" docker version --format 'client={{.Client.Version}} server={{.Server.Version}}'
  safe_run "Compose version" docker compose version
  safe_run "Containers" docker ps -a --no-trunc --format 'name={{.Names}} image={{.Image}} state={{.State}} status={{.Status}} ports={{.Ports}}'
  safe_run "Networks" docker network ls --format 'name={{.Name}} driver={{.Driver}} scope={{.Scope}}'
  safe_run "Volumes" docker volume ls --format 'name={{.Name}} driver={{.Driver}}'
  safe_run "Docker storage" docker system df
fi

PG_CONTAINER=""
if [[ "${DOCKER_AVAILABLE}" == true ]]; then
  if [[ -n "${REQUESTED_PG_CONTAINER}" ]]; then
    if docker inspect "${REQUESTED_PG_CONTAINER}" >/dev/null 2>&1; then
      PG_CONTAINER="${REQUESTED_PG_CONTAINER}"
    else
      printf '\nBLOCKER: DT_PG_CONTAINER does not identify an existing container.\n'
    fi
  else
    mapfile -t PG_CANDIDATES < <(
      docker ps --format '{{.Names}}|{{.Image}}' 2>/dev/null |
        awk -F'|' 'tolower($2) ~ /(^|\/)postgres([:@]|$)/ {print $1}'
    )
    if [[ ${#PG_CANDIDATES[@]} -eq 1 ]]; then
      PG_CONTAINER="${PG_CANDIDATES[0]}"
    elif [[ ${#PG_CANDIDATES[@]} -gt 1 ]]; then
      printf '\nBLOCKER: multiple running PostgreSQL containers found; rerun with DT_PG_CONTAINER=<name>.\n'
      printf 'candidates=%s\n' "$(IFS=,; echo "${PG_CANDIDATES[*]}")"
    else
      printf '\nBLOCKER: no running PostgreSQL container was identified by image name.\n'
    fi
  fi
fi

section "Existing PostgreSQL container"
if [[ -z "${PG_CONTAINER}" ]]; then
  printf 'status=UNRESOLVED\n'
else
  printf 'container=%s\n' "${PG_CONTAINER}"
  docker inspect --format 'image={{.Config.Image}} image_id={{.Image}} running={{.State.Running}} status={{.State.Status}} started_at={{.State.StartedAt}} restart_policy={{.HostConfig.RestartPolicy.Name}} memory_limit_bytes={{.HostConfig.Memory}} nano_cpus={{.HostConfig.NanoCpus}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}not-configured{{end}}' "${PG_CONTAINER}" 2>/dev/null || true
  printf '\n### Published PostgreSQL ports\n'
  docker port "${PG_CONTAINER}" 2>/dev/null || printf 'none-or-unavailable\n'
  printf '\n### PostgreSQL mounts (no file contents)\n'
  docker inspect --format '{{range .Mounts}}type={{.Type}} source={{.Source}} destination={{.Destination}} rw={{.RW}}{{println}}{{end}}' "${PG_CONTAINER}" 2>/dev/null || true
  printf '\n### PostgreSQL networks and DNS aliases\n'
  docker inspect --format '{{range $name, $network := .NetworkSettings.Networks}}network={{$name}} ip={{$network.IPAddress}} aliases={{json $network.Aliases}}{{println}}{{end}}' "${PG_CONTAINER}" 2>/dev/null || true
fi

section "Flyway history (SELECT only)"
printf 'target_database=%s\n' "${TARGET_DB}"
printf 'target_role=%s\n' "${TARGET_ROLE}"
if [[ -d "${MIGRATIONS_DIR}" ]]; then
  printf '\n### Repository migrations\n'
  while IFS= read -r migration; do
    basename "${migration}"
    if command_available sha256sum; then
      printf '  source_sha256=%s\n' "$(sha256sum "${migration}" | awk '{print $1}')"
    fi
  done < <(find "${MIGRATIONS_DIR}" -maxdepth 1 -type f -name 'V*.sql' | sort -V)
else
  printf 'BLOCKER: repository migration directory not found.\n'
fi

DB_HISTORY=""
FLYWAY_SCHEMA=""
if [[ -n "${PG_CONTAINER}" ]]; then
  DB_EXISTS="$(docker exec -u postgres "${PG_CONTAINER}" psql -X -w -A -t -d postgres -U "${TARGET_ROLE}" -c "SELECT 1 FROM pg_database WHERE datname = '${TARGET_DB}'" 2>/dev/null | tr -d '[:space:]' || true)"
  if [[ "${DB_EXISTS}" != "1" ]]; then
    printf '\nBLOCKER: database was not found or read-only socket authentication failed.\n'
    printf 'Rerun with safe selectors DT_DB_NAME and DT_PG_ROLE if their names differ; do not pass a password.\n'
  else
    FLYWAY_SCHEMA="$(docker exec -u postgres "${PG_CONTAINER}" psql -X -w -A -t -d "${TARGET_DB}" -U "${TARGET_ROLE}" -c "SELECT table_schema FROM information_schema.tables WHERE table_name = 'flyway_schema_history' ORDER BY (table_schema = 'public') DESC, table_schema LIMIT 1" 2>/dev/null | tr -d '[:space:]' || true)"
    if [[ -z "${FLYWAY_SCHEMA}" ]]; then
      printf '\nflyway_schema_history=ABSENT\n'
      printf 'migration_decision=STOP; inspect the non-empty database and backup before first migration.\n'
    elif [[ ! "${FLYWAY_SCHEMA}" =~ ^[A-Za-z_][A-Za-z0-9_]{0,62}$ ]]; then
      printf '\nBLOCKER: flyway_schema_history uses a schema name that requires manual inspection.\n'
      FLYWAY_SCHEMA=""
    else
      printf '\nflyway_schema_history=%s.flyway_schema_history\n' "${FLYWAY_SCHEMA}"
      DB_HISTORY="$(docker exec -u postgres "${PG_CONTAINER}" psql -X -w -A -t -F '|' -d "${TARGET_DB}" -U "${TARGET_ROLE}" -c "SELECT COALESCE(version, ''), description, COALESCE(checksum::text, 'NULL'), CASE WHEN success THEN 'SUCCESS' ELSE 'FAILED' END FROM \"${FLYWAY_SCHEMA}\".flyway_schema_history WHERE type = 'SQL' ORDER BY installed_rank" 2>/dev/null || true)"
      if [[ -z "${DB_HISTORY}" ]]; then
        printf 'history_rows=NONE_OR_QUERY_FAILED\n'
      else
        printf 'version|description|stored_flyway_checksum|state\n%s\n' "${DB_HISTORY}"
      fi
    fi
  fi
fi

printf '\n### Repository versus database manifest\n'
if [[ -z "${DB_HISTORY}" ]]; then
  printf 'comparison=UNAVAILABLE\n'
else
  MANIFEST_MISMATCH=false
  while IFS= read -r migration; do
    filename="$(basename "${migration}")"
    version="${filename%%__*}"
    version="${version#V}"
    repo_description="${filename#*__}"
    repo_description="${repo_description%.sql}"
    repo_description="${repo_description//_/ }"
    db_row="$(printf '%s\n' "${DB_HISTORY}" | awk -F'|' -v wanted="${version}" '$1 == wanted {print; exit}')"
    if [[ -z "${db_row}" ]]; then
      printf 'V%s=PENDING repository_description="%s"\n' "${version}" "${repo_description}"
      continue
    fi
    db_description="$(printf '%s' "${db_row}" | cut -d'|' -f2)"
    db_state="$(printf '%s' "${db_row}" | cut -d'|' -f4)"
    if [[ "${db_description,,}" != "${repo_description,,}" || "${db_state}" != "SUCCESS" ]]; then
      printf 'V%s=MISMATCH repository_description="%s" database_description="%s" state=%s\n' "${version}" "${repo_description}" "${db_description}" "${db_state}"
      MANIFEST_MISMATCH=true
    else
      printf 'V%s=VERSION_AND_DESCRIPTION_MATCH checksum=REQUIRES_FLYWAY_VALIDATE\n' "${version}"
    fi
  done < <(find "${MIGRATIONS_DIR}" -maxdepth 1 -type f -name 'V*.sql' | sort -V)
  if [[ "${MANIFEST_MISMATCH}" == true ]]; then
    printf 'migration_decision=STOP; do not migrate or repair. Preserve DB backup and create a corrective migration after review.\n'
  else
    printf 'migration_decision=CHECKSUM_UNRESOLVED; run Flyway validate with approved target credentials before migrate.\n'
  fi
fi

section "NGINX inventory"
if ! command_available nginx; then
  printf 'BLOCKER: nginx command unavailable.\n'
else
  printf 'service_active=%s\n' "$(systemctl is-active nginx 2>/dev/null || printf 'unknown')"
  printf 'service_enabled=%s\n' "$(systemctl is-enabled nginx 2>/dev/null || printf 'unknown')"
  nginx -v 2>&1 | sed -E 's#(https?://)[^/@:]+:[^/@]+@#\1[REDACTED]@#g' || true
  printf '\n### Enabled site files\n'
  find -L /etc/nginx/sites-enabled -maxdepth 1 -type f -printf '%p -> %l\n' 2>/dev/null | sort || printf 'unavailable\n'
  printf '\n### Effective routing directives (filtered)\n'
  "${SUDO[@]}" nginx -T 2>&1 |
    grep -E '^[[:space:]]*(listen|server_name|root|location|proxy_pass|client_max_body_size|try_files)[[:space:]]' |
    sed -E 's#(https?://)[^/@:]+:[^/@]+@#\1[REDACTED]@#g' || printf 'unavailable\n'
fi

section "Dynatrace OneAgent"
mapfile -t DT_UNITS < <(systemctl list-unit-files --type=service --no-legend 2>/dev/null | awk 'tolower($1) ~ /(oneagent|dynatrace)/ {print $1}')
if [[ ${#DT_UNITS[@]} -eq 0 ]]; then
  printf 'service_units=NONE_FOUND\n'
else
  for unit in "${DT_UNITS[@]}"; do
    printf 'unit=%s enabled=%s active=%s\n' "${unit}" "$(systemctl is-enabled "${unit}" 2>/dev/null || printf 'unknown')" "$(systemctl is-active "${unit}" 2>/dev/null || printf 'unknown')"
  done
fi
if command_available dpkg-query; then
  dpkg-query -W -f='package=${binary:Package} version=${Version}\n' 2>/dev/null | grep -Ei 'dynatrace|oneagent' || printf 'package_version=NOT_FOUND_VIA_DPKG\n'
fi
ONEAGENTCTL="$(find /opt/dynatrace/oneagent -maxdepth 5 -type f -name oneagentctl -print -quit 2>/dev/null || true)"
if [[ -n "${ONEAGENTCTL}" ]]; then
  "${SUDO[@]}" "${ONEAGENTCTL}" --version 2>&1 | head -n 5 || printf 'oneagentctl_version=UNAVAILABLE\n'
else
  printf 'oneagentctl_version=NOT_FOUND\n'
fi
printf '\n### OneAgent processes\n'
ps -eo pid=,comm= | awk 'tolower($2) ~ /(oneagent|dynatrace)/ {print "pid=" $1 " command=" $2}' || true
printf '\n### Java process injection evidence\n'
JAVA_FOUND=false
while read -r pid command; do
  [[ -z "${pid:-}" ]] && continue
  JAVA_FOUND=true
  if grep -qiE 'oneagent|dynatrace' "/proc/${pid}/maps" 2>/dev/null; then
    printf 'pid=%s command=%s oneagent_library_mapped=yes\n' "${pid}" "${command}"
  else
    printf 'pid=%s command=%s oneagent_library_mapped=no-or-unreadable\n' "${pid}" "${command}"
  fi
done < <(ps -eo pid=,comm= | awk '$2 ~ /^java$/ {print $1, $2}')
if [[ "${JAVA_FOUND}" == false ]]; then
  printf 'java_processes=NONE_RUNNING\n'
fi
printf 'tenant_host_visibility=UNVERIFIED_WITHOUT_DYNATRACE_UI_OR_APPROVED_API_ACCESS\n'

section "Read-only deployment assessment"
printf '%s\n' \
  '- Connect Spring Boot to the existing PostgreSQL container through an existing Docker network and its verified DNS alias.' \
  '- Do not publish PostgreSQL or create another PostgreSQL service.' \
  '- Keep host NGINX as the only public listener; bind the future backend to loopback or a private Docker network.' \
  '- Keep OneAgent on the host; verify container/process injection before considering telemetry confirmed.' \
  '- Do not run Flyway migrate until backup, schema history and checksum validation are approved.'
printf '\nverdict=REQUIRES_HUMAN_REVIEW_OF_THIS_OUTPUT_BEFORE_I8.2\n'
