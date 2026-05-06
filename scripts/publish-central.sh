#!/usr/bin/env bash
set -euo pipefail

VERSION="${VERSION:-}"
PUBLISHING_TYPE="${PUBLISHING_TYPE:-AUTOMATIC}"
DEPLOYMENT_NAME="${DEPLOYMENT_NAME:-}"
POLL_SECONDS="${POLL_SECONDS:-10}"
MAX_POLLS="${MAX_POLLS:-90}"
SKIP_PUBLISH_SIGNED="${SKIP_PUBLISH_SIGNED:-0}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

get_build_version() {
  sed -n 's/.*ThisBuild \/ version := "\([^"]*\)".*/\1/p' "${REPO_ROOT}/build.sbt" | head -n 1
}

get_credential_value() {
  local key="$1"
  local env_name="$2"
  local env_value="${!env_name:-}"
  local cred_file="${HOME}/.sbt/sonatype_central_credentials"

  if [[ -n "${env_value}" ]]; then
    printf '%s\n' "${env_value}"
    return
  fi

  if [[ ! -f "${cred_file}" ]]; then
    echo "Credentials-Datei nicht gefunden: ${cred_file}" >&2
    exit 1
  fi

  local value
  value="$(sed -n "s/^${key}=//p" "${cred_file}" | head -n 1)"
  if [[ -z "${value}" ]]; then
    echo "Eintrag '${key}' fehlt in ${cred_file}" >&2
    exit 1
  fi

  printf '%s\n' "${value}"
}

json_field() {
  local json="$1"
  local field="$2"

  if command -v python3 >/dev/null 2>&1; then
    python3 -c 'import json,sys; print(json.loads(sys.stdin.read()).get(sys.argv[1], ""))' "${field}" <<<"${json}"
    return
  fi

  sed -n "s/.*\"${field}\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" <<<"${json}" | head -n 1
}

cd "${REPO_ROOT}"

if [[ -z "${VERSION}" ]]; then
  VERSION="$(get_build_version)"
fi

if [[ -z "${VERSION}" ]]; then
  echo "Konnte Version aus build.sbt nicht lesen." >&2
  exit 1
fi

if [[ -z "${DEPLOYMENT_NAME}" ]]; then
  DEPLOYMENT_NAME="com.anjunar:scalajs-jfx2:${VERSION}"
fi

if [[ "${PUBLISHING_TYPE}" != "AUTOMATIC" && "${PUBLISHING_TYPE}" != "USER_MANAGED" ]]; then
  echo "PUBLISHING_TYPE muss AUTOMATIC oder USER_MANAGED sein." >&2
  exit 1
fi

if [[ "${SKIP_PUBLISH_SIGNED}" != "1" ]]; then
  sbt --batch "publishSigned"
fi

BUNDLE_DIR="${REPO_ROOT}/target/sona-staging"
BUNDLE_ZIP="${REPO_ROOT}/target/central-bundle-${VERSION}.zip"

if [[ ! -d "${BUNDLE_DIR}" ]]; then
  echo "Bundle-Verzeichnis nicht gefunden: ${BUNDLE_DIR}" >&2
  exit 1
fi

rm -f "${BUNDLE_ZIP}"
(
  cd "${BUNDLE_DIR}"
  if command -v zip >/dev/null 2>&1; then
    zip -qr "${BUNDLE_ZIP}" .
  else
    jar --create --file "${BUNDLE_ZIP}" -C . .
  fi
)

USER_NAME="$(get_credential_value user SONATYPE_CENTRAL_USERNAME)"
PASSWORD="$(get_credential_value password SONATYPE_CENTRAL_PASSWORD)"
TOKEN="$(printf '%s' "${USER_NAME}:${PASSWORD}" | base64 | tr -d '\n')"

if command -v python3 >/dev/null 2>&1; then
  ENCODED_DEPLOYMENT_NAME="$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "${DEPLOYMENT_NAME}")"
else
  ENCODED_DEPLOYMENT_NAME="${DEPLOYMENT_NAME//:/%3A}"
fi

UPLOAD_URL="https://central.sonatype.com/api/v1/publisher/upload?name=${ENCODED_DEPLOYMENT_NAME}&publishingType=${PUBLISHING_TYPE}"

echo "Lade Bundle hoch: ${BUNDLE_ZIP}"
DEPLOYMENT_ID="$(curl --silent --show-error --fail \
  --request POST \
  --header "Authorization: Bearer ${TOKEN}" \
  --form "bundle=@${BUNDLE_ZIP}" \
  "${UPLOAD_URL}")"

if [[ -z "${DEPLOYMENT_ID}" ]]; then
  echo "Sonatype hat keine Deployment-ID zurueckgegeben." >&2
  exit 1
fi

echo "Deployment-ID: ${DEPLOYMENT_ID}"

for ((attempt = 1; attempt <= MAX_POLLS; attempt++)); do
  sleep "${POLL_SECONDS}"

  STATUS_JSON="$(curl --silent --show-error --fail \
    --request POST \
    --header "Authorization: Bearer ${TOKEN}" \
    "https://central.sonatype.com/api/v1/publisher/status?id=${DEPLOYMENT_ID}")"

  STATE="$(json_field "${STATUS_JSON}" deploymentState)"
  echo "[${attempt}/${MAX_POLLS}] Status: ${STATE}"

  if [[ "${STATE}" == "PUBLISHED" ]]; then
    echo "${STATUS_JSON}"
    echo "Maven Central Publish abgeschlossen."
    exit 0
  fi

  if [[ "${STATE}" == "FAILED" || "${STATE}" == "VALIDATED" ]]; then
    echo "${STATUS_JSON}"
    echo "Deployment beendet mit Status ${STATE}." >&2
    exit 1
  fi
done

echo "Timeout beim Warten auf den Sonatype-Status. Deployment-ID: ${DEPLOYMENT_ID}" >&2
exit 1
