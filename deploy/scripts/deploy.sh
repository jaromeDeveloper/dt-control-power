#!/usr/bin/env bash

# Deliberately manual deployment. PostgreSQL is external and is never created,
# stopped, removed, repaired or baselined by this script.

set -Eeuo pipefail
umask 077

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="${DT_REPO_ROOT:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
ENV_FILE="${DT_ENV_FILE:-${REPO_ROOT}/deploy/.env}"
COMPOSE_FILE="${DT_COMPOSE_FILE:-${REPO_ROOT}/deploy/compose.yaml}"
BACKEND_SERVICE="${DT_BACKEND_SERVICE:-backend}"
EXPECTED_BRANCH="${DT_DEPLOY_BRANCH:-master}"
BACKUP_REF=""
AUTHORIZED_DEPLOY=false
AUTHORIZED_FLYWAY=false

readonly WEB_ROOT="/var/www/dt-control-power"
readonly RELEASES_ROOT="${WEB_ROOT}/releases"
readonly CURRENT_LINK="${WEB_ROOT}/current"
readonly NGINX_SOURCE="${REPO_ROOT}/deploy/nginx/dt-control-power.conf"
readonly NGINX_AVAILABLE="/etc/nginx/sites-available/dt-control-power.conf"
readonly NGINX_ENABLED="/etc/nginx/sites-enabled/dt-control-power.conf"
readonly STATE_ROOT="/var/lib/dt-control-power/deployments"

usage() {
  cat <<'USAGE'
Usage: deploy.sh --authorize-deploy --authorize-flyway --db-backup-ref REF
                 [--env-file PATH] [--compose-file PATH] [--service NAME]

--authorize-flyway acknowledges that starting the backend may apply forward-only
Flyway migrations. REF identifies an independently verified database backup.
This script does not update Git and requires a clean expected branch. REF must
be an existing, non-empty backup file created and verified by the operator.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --authorize-deploy) AUTHORIZED_DEPLOY=true; shift ;;
    --authorize-flyway) AUTHORIZED_FLYWAY=true; shift ;;
    --db-backup-ref) BACKUP_REF="${2:?missing value for --db-backup-ref}"; shift 2 ;;
    --env-file) ENV_FILE="${2:?missing value for --env-file}"; shift 2 ;;
    --compose-file) COMPOSE_FILE="${2:?missing value for --compose-file}"; shift 2 ;;
    --service) BACKEND_SERVICE="${2:?missing value for --service}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'ERROR: unsupported argument: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

[[ "${AUTHORIZED_DEPLOY}" == true ]] || { printf 'ERROR: --authorize-deploy is required\n' >&2; exit 2; }
[[ "${AUTHORIZED_FLYWAY}" == true ]] || { printf 'ERROR: --authorize-flyway is required\n' >&2; exit 2; }
[[ "${BACKUP_REF}" =~ ^[A-Za-z0-9._:/@+-]{3,200}$ ]] || {
  printf 'ERROR: --db-backup-ref must be a safe, non-empty external backup reference\n' >&2
  exit 2
}
[[ "${BACKUP_REF}" == /* && -s "${BACKUP_REF}" ]] || {
  printf 'ERROR: --db-backup-ref must be an absolute path to an existing non-empty backup file\n' >&2
  exit 2
}

if (( EUID == 0 )); then SUDO=(); else SUDO=(sudo); fi
compose=(docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}")

bash "${SCRIPT_DIR}/preflight.sh" --env-file "${ENV_FILE}" --compose-file "${COMPOSE_FILE}" --service "${BACKEND_SERVICE}"

branch="$(git -C "${REPO_ROOT}" branch --show-current)"
[[ "${branch}" == "${EXPECTED_BRANCH}" ]] || { printf 'ERROR: expected branch %s\n' "${EXPECTED_BRANCH}" >&2; exit 1; }
[[ -z "$(git -C "${REPO_ROOT}" status --porcelain)" ]] || { printf 'ERROR: worktree changed after preflight\n' >&2; exit 1; }
commit="$(git -C "${REPO_ROOT}" rev-parse --verify HEAD)"
short_commit="$(git -C "${REPO_ROOT}" rev-parse --short=12 HEAD)"
export APP_VERSION="${short_commit}"
release_id="$(date -u +%Y%m%dT%H%M%SZ)-${short_commit}"
release_path="${RELEASES_ROOT}/${release_id}"
manifest_dir="${STATE_ROOT}/${release_id}"
manifest="${manifest_dir}/manifest.env"

configured_image="$("${compose[@]}" config --images | awk 'NF {print; exit}')"
[[ -n "${configured_image}" ]] || { printf 'ERROR: unable to resolve backend image reference\n' >&2; exit 1; }
container_id="$("${compose[@]}" ps -q "${BACKEND_SERVICE}" 2>/dev/null || true)"
previous_image_id="NONE"
if [[ -n "${container_id}" ]]; then
  previous_image_id="$(docker inspect --format '{{.Image}}' "${container_id}")"
fi
previous_frontend="NONE"
if [[ -L "${CURRENT_LINK}" ]]; then previous_frontend="$(readlink -f "${CURRENT_LINK}")"; fi

printf 'Building immutable frontend release %s\n' "${release_id}"
(cd "${REPO_ROOT}/frontend" && npm ci && npm run build)
[[ -f "${REPO_ROOT}/frontend/dist/index.html" ]] || { printf 'ERROR: frontend build produced no index.html\n' >&2; exit 1; }

# umask 077 protects deployment state, but NGINX must be able to traverse the
# public web root. Create and normalize these parents explicitly because
# install -d would otherwise create an omitted parent with mode 0700.
"${SUDO[@]}" install -d -m 0755 "${WEB_ROOT}" "${RELEASES_ROOT}"
"${SUDO[@]}" chmod 0755 "${WEB_ROOT}" "${RELEASES_ROOT}"
"${SUDO[@]}" install -d -m 0755 "${manifest_dir}"
[[ ! -e "${release_path}" ]] || { printf 'ERROR: release already exists: %s\n' "${release_path}" >&2; exit 1; }
"${SUDO[@]}" install -d -m 0755 "${release_path}"
"${SUDO[@]}" cp -a "${REPO_ROOT}/frontend/dist/." "${release_path}/"
"${SUDO[@]}" find "${release_path}" -type d -exec chmod 0755 {} +
"${SUDO[@]}" find "${release_path}" -type f -exec chmod 0644 {} +

nginx_backup="NONE"
nginx_existed=false
if [[ -f "${NGINX_AVAILABLE}" ]]; then
  nginx_backup="${manifest_dir}/nginx.previous.conf"
  "${SUDO[@]}" cp -a "${NGINX_AVAILABLE}" "${nginx_backup}"
  nginx_existed=true
fi
nginx_enabled_backup="NONE"
nginx_enabled_existed=false
if [[ -e "${NGINX_ENABLED}" || -L "${NGINX_ENABLED}" ]]; then
  nginx_enabled_backup="${manifest_dir}/nginx.enabled.previous"
  "${SUDO[@]}" cp -a "${NGINX_ENABLED}" "${nginx_enabled_backup}"
  nginx_enabled_existed=true
fi

tmp_manifest="$(mktemp)"
trap 'rm -f "${tmp_manifest:-}"' EXIT
cat >"${tmp_manifest}" <<EOF
MANIFEST_VERSION=1
RELEASE_ID=${release_id}
DEPLOYED_COMMIT=${commit}
DB_BACKUP_REF=${BACKUP_REF}
COMPOSE_FILE=${COMPOSE_FILE}
ENV_FILE=${ENV_FILE}
BACKEND_SERVICE=${BACKEND_SERVICE}
CONFIGURED_IMAGE=${configured_image}
PREVIOUS_IMAGE_ID=${previous_image_id}
PREVIOUS_FRONTEND=${previous_frontend}
DEPLOYED_FRONTEND=${release_path}
NGINX_BACKUP=${nginx_backup}
NGINX_PREVIOUSLY_EXISTED=${nginx_existed}
NGINX_ENABLED_BACKUP=${nginx_enabled_backup}
NGINX_ENABLED_PREVIOUSLY_EXISTED=${nginx_enabled_existed}
EOF
"${SUDO[@]}" install -m 0644 "${tmp_manifest}" "${manifest}"

printf 'Building and starting backend only; Flyway may migrate forward now\n'
"${compose[@]}" up -d --build --no-deps "${BACKEND_SERVICE}"

healthy=false
for _ in {1..36}; do
  if curl --fail --silent --show-error --max-time 5 http://127.0.0.1:8080/api/v1/health >/dev/null; then healthy=true; break; fi
  sleep 5
done
[[ "${healthy}" == true ]] || {
  printf 'ERROR: backend did not become healthy. Database schema may already be forward-migrated; do not reverse it here.\n' >&2
  printf 'Recovery manifest: %s\n' "${manifest}" >&2
  exit 1
}

"${SUDO[@]}" install -m 0644 "${NGINX_SOURCE}" "${NGINX_AVAILABLE}"
"${SUDO[@]}" ln -sfn "${NGINX_AVAILABLE}" "${NGINX_ENABLED}"
if ! "${SUDO[@]}" nginx -t; then
  if [[ "${nginx_existed}" == true ]]; then
    "${SUDO[@]}" install -m 0644 "${nginx_backup}" "${NGINX_AVAILABLE}"
  else
    "${SUDO[@]}" rm -f -- "${NGINX_AVAILABLE}"
  fi
  "${SUDO[@]}" rm -f -- "${NGINX_ENABLED}"
  if [[ "${nginx_enabled_existed}" == true ]]; then
    "${SUDO[@]}" cp -a "${nginx_enabled_backup}" "${NGINX_ENABLED}"
  fi
  printf 'ERROR: NGINX candidate validation failed; previous files were restored and NGINX was not reloaded.\n' >&2
  exit 1
fi
"${SUDO[@]}" ln -sfn "${release_path}" "${CURRENT_LINK}"
if ! "${SUDO[@]}" systemctl reload nginx; then
  if [[ "${previous_frontend}" != "NONE" ]]; then
    "${SUDO[@]}" ln -sfn "${previous_frontend}" "${CURRENT_LINK}"
  else
    "${SUDO[@]}" rm -f -- "${CURRENT_LINK}"
  fi
  if [[ "${nginx_existed}" == true ]]; then
    "${SUDO[@]}" install -m 0644 "${nginx_backup}" "${NGINX_AVAILABLE}"
  else
    "${SUDO[@]}" rm -f -- "${NGINX_AVAILABLE}"
  fi
  "${SUDO[@]}" rm -f -- "${NGINX_ENABLED}"
  if [[ "${nginx_enabled_existed}" == true ]]; then
    "${SUDO[@]}" cp -a "${nginx_enabled_backup}" "${NGINX_ENABLED}"
  fi
  "${SUDO[@]}" nginx -t >/dev/null 2>&1 || true
  "${SUDO[@]}" systemctl reload nginx >/dev/null 2>&1 || true
  printf 'ERROR: NGINX reload failed; previous frontend and configuration were restored.\n' >&2
  exit 1
fi

printf 'DEPLOYED release=%s commit=%s\n' "${release_id}" "${commit}"
printf 'manifest=%s\ndb_backup_ref=%s\n' "${manifest}" "${BACKUP_REF}"
printf 'Run check.sh separately. No PostgreSQL container or volume was modified by this script.\n'
