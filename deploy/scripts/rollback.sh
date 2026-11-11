#!/usr/bin/env bash

# Application-only rollback. The database remains at its current Flyway schema;
# this script never invokes migrations, repair, baseline or database restore.

set -Eeuo pipefail
umask 077

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="${DT_REPO_ROOT:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
readonly STATE_ROOT="/var/lib/dt-control-power/deployments"
readonly CURRENT_LINK="/var/www/dt-control-power/current"
readonly NGINX_AVAILABLE="/etc/nginx/sites-available/dt-control-power.conf"
readonly NGINX_ENABLED="/etc/nginx/sites-enabled/dt-control-power.conf"
MANIFEST=""
EXPECTED_BACKUP_REF=""
AUTHORIZED_ROLLBACK=false
DB_COMPATIBLE=false

usage() {
  cat <<'USAGE'
Usage: rollback.sh --authorize-rollback --confirm-db-compatible
                   --manifest /var/lib/dt-control-power/deployments/ID/manifest.env
                   --expected-db-backup-ref REF

Compatibility confirmation means the operator has verified that the previous
application image can run against the CURRENT (not reverted) Flyway schema.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --authorize-rollback) AUTHORIZED_ROLLBACK=true; shift ;;
    --confirm-db-compatible) DB_COMPATIBLE=true; shift ;;
    --manifest) MANIFEST="${2:?missing value for --manifest}"; shift 2 ;;
    --expected-db-backup-ref) EXPECTED_BACKUP_REF="${2:?missing value for --expected-db-backup-ref}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'ERROR: unsupported argument: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

[[ "${AUTHORIZED_ROLLBACK}" == true ]] || { printf 'ERROR: --authorize-rollback is required\n' >&2; exit 2; }
[[ "${DB_COMPATIBLE}" == true ]] || { printf 'ERROR: --confirm-db-compatible is required\n' >&2; exit 2; }
[[ "${EXPECTED_BACKUP_REF}" =~ ^[A-Za-z0-9._:/@+-]{3,200}$ ]] || { printf 'ERROR: a safe --expected-db-backup-ref is required\n' >&2; exit 2; }
[[ -f "${MANIFEST}" ]] || { printf 'ERROR: manifest not found\n' >&2; exit 2; }

manifest_real="$(readlink -f "${MANIFEST}")"
state_real="$(readlink -f "${STATE_ROOT}")"
[[ "${manifest_real}" == "${state_real}"/*/manifest.env ]] || { printf 'ERROR: manifest must be inside %s\n' "${STATE_ROOT}" >&2; exit 2; }

manifest_value() {
  local key="$1" value
  value="$(awk -F= -v wanted="${key}" '$1 == wanted {sub(/^[^=]*=/, ""); print; exit}' "${manifest_real}")"
  [[ -n "${value}" ]] || { printf 'ERROR: manifest key missing: %s\n' "${key}" >&2; exit 2; }
  printf '%s' "${value}"
}

[[ "$(manifest_value MANIFEST_VERSION)" == 1 ]] || { printf 'ERROR: unsupported manifest version\n' >&2; exit 2; }
backup_ref="$(manifest_value DB_BACKUP_REF)"
[[ "${backup_ref}" == "${EXPECTED_BACKUP_REF}" ]] || { printf 'ERROR: backup reference does not match manifest\n' >&2; exit 2; }
[[ "${backup_ref}" == /* && -s "${backup_ref}" ]] || { printf 'ERROR: recorded database backup file is unavailable or empty\n' >&2; exit 1; }
compose_file="$(manifest_value COMPOSE_FILE)"
env_file="$(manifest_value ENV_FILE)"
backend_service="$(manifest_value BACKEND_SERVICE)"
configured_image="$(manifest_value CONFIGURED_IMAGE)"
previous_image_id="$(manifest_value PREVIOUS_IMAGE_ID)"
previous_frontend="$(manifest_value PREVIOUS_FRONTEND)"
nginx_backup="$(manifest_value NGINX_BACKUP)"
nginx_existed="$(manifest_value NGINX_PREVIOUSLY_EXISTED)"
nginx_enabled_backup="$(manifest_value NGINX_ENABLED_BACKUP)"
nginx_enabled_existed="$(manifest_value NGINX_ENABLED_PREVIOUSLY_EXISTED)"

[[ -f "${compose_file}" && -f "${env_file}" ]] || { printf 'ERROR: recorded Compose/env files are unavailable\n' >&2; exit 1; }
[[ "${backend_service}" =~ ^[A-Za-z0-9._-]+$ ]] || { printf 'ERROR: unsafe service name in manifest\n' >&2; exit 2; }
[[ "${configured_image}" =~ ^[A-Za-z0-9._/:@-]+$ ]] || { printf 'ERROR: unsafe image reference in manifest\n' >&2; exit 2; }
[[ "${previous_image_id}" =~ ^sha256:[a-f0-9]{64}$ ]] || { printf 'ERROR: no valid previous backend image is recorded; automatic rollback is unsafe\n' >&2; exit 1; }
[[ -d "${previous_frontend}" && "${previous_frontend}" == /var/www/dt-control-power/releases/* ]] || { printf 'ERROR: no valid previous frontend release is recorded\n' >&2; exit 1; }
docker image inspect "${previous_image_id}" >/dev/null 2>&1 || { printf 'ERROR: previous backend image no longer exists locally\n' >&2; exit 1; }
if [[ "${nginx_existed}" == true ]]; then
  [[ -f "${nginx_backup}" && "${nginx_backup}" == "${state_real}"/*/nginx.previous.conf ]] || { printf 'ERROR: recorded NGINX backup is unavailable or unsafe\n' >&2; exit 1; }
elif [[ "${nginx_existed}" != false ]]; then
  printf 'ERROR: invalid NGINX state in manifest\n' >&2
  exit 2
fi
if [[ "${nginx_enabled_existed}" == true ]]; then
  [[ ( -e "${nginx_enabled_backup}" || -L "${nginx_enabled_backup}" ) && "${nginx_enabled_backup}" == "${state_real}"/*/nginx.enabled.previous ]] || { printf 'ERROR: recorded NGINX enabled-site backup is unavailable or unsafe\n' >&2; exit 1; }
elif [[ "${nginx_enabled_existed}" != false ]]; then
  printf 'ERROR: invalid enabled-site state in manifest\n' >&2
  exit 2
fi

if (( EUID == 0 )); then SUDO=(); else SUDO=(sudo); fi
export APP_VERSION="${configured_image##*:}"
compose=(docker compose --env-file "${env_file}" -f "${compose_file}")

printf 'Restoring application artifacts only; database schema remains unchanged\n'
docker tag "${previous_image_id}" "${configured_image}"
"${compose[@]}" up -d --no-build --no-deps --force-recreate "${backend_service}"

if [[ "${nginx_existed}" == true ]]; then
  "${SUDO[@]}" install -m 0644 "${nginx_backup}" "${NGINX_AVAILABLE}"
elif [[ "${nginx_existed}" == false ]]; then
  "${SUDO[@]}" rm -f -- "${NGINX_AVAILABLE}"
fi
"${SUDO[@]}" rm -f -- "${NGINX_ENABLED}"
if [[ "${nginx_enabled_existed}" == true ]]; then
  "${SUDO[@]}" cp -a "${nginx_enabled_backup}" "${NGINX_ENABLED}"
fi

"${SUDO[@]}" nginx -t
"${SUDO[@]}" systemctl reload nginx
"${SUDO[@]}" ln -sfn "${previous_frontend}" "${CURRENT_LINK}"

healthy=false
for _ in {1..36}; do
  if curl --fail --silent --show-error --max-time 5 http://127.0.0.1:8080/api/v1/health >/dev/null; then healthy=true; break; fi
  sleep 5
done
[[ "${healthy}" == true ]] || { printf 'ERROR: previous application did not become healthy; database was not changed\n' >&2; exit 1; }

printf 'ROLLED_BACK application_to_image=%s frontend=%s\n' "${previous_image_id}" "${previous_frontend}"
printf 'database_schema=UNCHANGED verified_backup_ref=%s\n' "${backup_ref}"
