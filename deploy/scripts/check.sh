#!/usr/bin/env bash

# Read-only operational verification, except for an optional synthetic
# login/logout session when DT_SMOKE_USERNAME and DT_SMOKE_PASSWORD are set.

set -Eeuo pipefail
umask 077

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="${DT_REPO_ROOT:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
ENV_FILE="${DT_ENV_FILE:-${REPO_ROOT}/deploy/.env}"
COMPOSE_FILE="${DT_COMPOSE_FILE:-${REPO_ROOT}/deploy/compose.yaml}"
BACKEND_SERVICE="${DT_BACKEND_SERVICE:-backend}"
BASE_URL="${DT_CHECK_BASE_URL:-http://127.0.0.1}"
HOST_HEADER="${DT_CHECK_HOST_HEADER:-dt-controltower-01}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-}"
POSTGRES_DB="${POSTGRES_DB:-}"
FAILURES=0
WARNINGS=0

usage() {
  cat <<'USAGE'
Usage: check.sh [--env-file PATH] [--compose-file PATH] [--service NAME]
                [--base-url URL] [--host-header HOST]

For authenticated read-only API checks, export DT_SMOKE_USERNAME and
DT_SMOKE_PASSWORD. Values are never printed and the temporary cookie jar is
removed on exit.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file) ENV_FILE="${2:?missing value for --env-file}"; shift 2 ;;
    --compose-file) COMPOSE_FILE="${2:?missing value for --compose-file}"; shift 2 ;;
    --service) BACKEND_SERVICE="${2:?missing value for --service}"; shift 2 ;;
    --base-url) BASE_URL="${2:?missing value for --base-url}"; shift 2 ;;
    --host-header) HOST_HEADER="${2:?missing value for --host-header}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'ERROR: unsupported argument: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

pass() { printf 'PASS: %s\n' "$1"; }
fail() { printf 'FAIL: %s\n' "$1" >&2; FAILURES=$((FAILURES + 1)); }
warn() { printf 'WARN: %s\n' "$1" >&2; WARNINGS=$((WARNINGS + 1)); }
safe_http() { curl --fail --silent --show-error --max-time 10 -H "Host: ${HOST_HEADER}" "$@"; }
read_env_value() {
  local key="$1"
  awk -F= -v wanted="${key}" '$1 == wanted {sub(/^[^=]*=/, ""); print; exit}' "${ENV_FILE}" 2>/dev/null || true
}

cookie_jar="$(mktemp)"
csrf_json="$(mktemp)"
trap 'rm -f "${cookie_jar:-}" "${csrf_json:-}"' EXIT

if safe_http "${BASE_URL}/api/v1/health" >/dev/null; then pass 'public health endpoint'; else fail 'public health endpoint'; fi
if safe_http "${BASE_URL}/" | grep -qi '<html'; then pass 'SPA entry point'; else fail 'SPA entry point'; fi
if safe_http "${BASE_URL}/api/v1/auth/csrf" >/dev/null; then pass 'API routing through NGINX'; else fail 'API routing through NGINX'; fi

if [[ -n "${DT_SMOKE_USERNAME:-}" && -n "${DT_SMOKE_PASSWORD:-}" ]]; then
  if ! command -v python3 >/dev/null 2>&1; then
    fail 'python3 is required for secret-safe authenticated smoke checks'
  elif safe_http -c "${cookie_jar}" "${BASE_URL}/api/v1/auth/csrf" >"${csrf_json}"; then
    csrf_header="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["headerName"])' "${csrf_json}" 2>/dev/null || true)"
    csrf_token="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["token"])' "${csrf_json}" 2>/dev/null || true)"
    if [[ -z "${csrf_header}" || -z "${csrf_token}" || ! "${csrf_header}" =~ ^[A-Za-z0-9_-]+$ ]]; then
      fail 'CSRF response contract'
    elif python3 -c 'import json,os; print(json.dumps({"username":os.environ["DT_SMOKE_USERNAME"],"password":os.environ["DT_SMOKE_PASSWORD"]}))' |
        safe_http -b "${cookie_jar}" -c "${cookie_jar}" -H 'Content-Type: application/json' -H "${csrf_header}: ${csrf_token}" \
          --data-binary @- "${BASE_URL}/api/v1/auth/login" >/dev/null; then
      pass 'synthetic login'
      if safe_http -b "${cookie_jar}" "${BASE_URL}/api/v1/areas" >/dev/null; then pass 'authenticated areas API'; else fail 'authenticated areas API'; fi
      if safe_http -b "${cookie_jar}" "${BASE_URL}/api/v1/dashboard/summary" >/dev/null; then pass 'authenticated dashboard API'; else fail 'authenticated dashboard API'; fi
      fresh_csrf="$(safe_http -b "${cookie_jar}" -c "${cookie_jar}" "${BASE_URL}/api/v1/auth/csrf" || true)"
      logout_header="$(printf '%s' "${fresh_csrf}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["headerName"])' 2>/dev/null || true)"
      logout_token="$(printf '%s' "${fresh_csrf}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])' 2>/dev/null || true)"
      if [[ "${logout_header}" =~ ^[A-Za-z0-9_-]+$ ]] && safe_http -b "${cookie_jar}" -X POST -H "${logout_header}: ${logout_token}" "${BASE_URL}/api/v1/auth/logout" >/dev/null; then
        pass 'synthetic logout'
      else
        warn 'synthetic logout could not be confirmed; temporary local cookies will still be deleted'
      fi
    else
      fail 'synthetic login'
    fi
  else
    fail 'CSRF bootstrap for authenticated checks'
  fi
elif [[ -n "${DT_SMOKE_USERNAME:-}" || -n "${DT_SMOKE_PASSWORD:-}" ]]; then
  fail 'both DT_SMOKE_USERNAME and DT_SMOKE_PASSWORD must be exported together'
else
  warn 'authenticated API checks skipped; smoke credentials were not exported'
fi

postgres_container="${POSTGRES_CONTAINER:-$(read_env_value POSTGRES_CONTAINER)}"
db_url="$(read_env_value DB_URL)"
db_username="$(read_env_value DB_USERNAME)"
db_password="$(read_env_value DB_PASSWORD)"
postgres_db="${POSTGRES_DB:-$(printf '%s' "${db_url}" | sed -E 's#^jdbc:postgresql://[^/]+/([^?]+).*$#\1#')}"
if [[ -z "${postgres_container}" || -z "${postgres_db}" || -z "${db_username}" || -z "${db_password}" ]]; then
  fail 'PostgreSQL selectors or application credentials are missing from the protected env file'
elif docker inspect "${postgres_container}" >/dev/null 2>&1; then
  if printf '%s\n' "${db_password}" | docker exec -i "${postgres_container}" sh -c \
      'IFS= read -r PGPASSWORD; export PGPASSWORD; exec psql -X -U "$1" -d "$2" -Atqc "SELECT 1"' \
      sh "${db_username}" "${postgres_db}" 2>/dev/null | grep -qx '1'; then
    pass 'PostgreSQL read-only connectivity query'
  else
    fail 'PostgreSQL read-only connectivity query'
  fi
else
  fail 'existing PostgreSQL container could not be inspected'
fi

if systemctl is-active --quiet oneagent 2>/dev/null || systemctl list-units --type=service --state=active --no-legend 2>/dev/null | grep -Eqi '(oneagent|dynatrace)'; then
  pass 'OneAgent service active'
else
  warn 'OneAgent service active state not confirmed'
fi

compose=(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}")
container_id="$("${compose[@]}" ps -q "${BACKEND_SERVICE}" 2>/dev/null || true)"
host_pid=""
if [[ -n "${container_id}" ]]; then host_pid="$(docker inspect --format '{{.State.Pid}}' "${container_id}" 2>/dev/null || true)"; fi
if [[ "${host_pid}" =~ ^[1-9][0-9]*$ ]] && grep -Eqi '(dynatrace|oneagent)' "/proc/${host_pid}/maps" 2>/dev/null; then
  pass 'OneAgent injection evidence found in backend process maps'
else
  warn 'OneAgent injection evidence not visible; confirm the backend process in Dynatrace'
fi

printf 'summary_failures=%d summary_warnings=%d\n' "${FAILURES}" "${WARNINGS}"
(( FAILURES == 0 )) || exit 1
