#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

API_PORT="${RTO_E2E_API_PORT:-8097}"
WEB_PORT="${RTO_E2E_WEB_PORT:-4197}"
DB_PORT="${RTO_RELEASE_E2E_DB_PORT:-55432}"
DB_USER="${SPRING_DATASOURCE_USERNAME:-${RTO_PILOT_DATA_DB_USER:-${POSTGRES_USER:-recruiting_os}}}"
DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-${RTO_PILOT_DATA_DB_PASSWORD:-${POSTGRES_PASSWORD:-recruiting_os_local_password}}}"
DB_CONTAINER=""
if [[ -z "${SPRING_DATASOURCE_URL:-}" && -z "${RTO_PILOT_DATA_JDBC_URL:-}" ]]; then
  JDBC_URL="jdbc:postgresql://127.0.0.1:${DB_PORT}/recruiting_os"
  DB_CONTAINER="rto-release-e2e-postgres-$$"
else
  JDBC_URL="${SPRING_DATASOURCE_URL:-${RTO_PILOT_DATA_JDBC_URL}}"
fi
DOC_ROOT="${RTO_DOCUMENT_STORAGE_ROOT_DIR:-$(mktemp -d "${TMPDIR:-/tmp}/rto-release-e2e-docs.XXXXXX")}"
LOG_FILE="${RTO_RELEASE_E2E_API_LOG:-${ROOT_DIR}/test-results/release-pilot-e2e-core-api.log}"
API_PID=""

cleanup() {
  local exit_code=$?
  if [[ -n "${API_PID}" ]] && kill -0 "${API_PID}" >/dev/null 2>&1; then
    printf '[release:e2e] Cleaning up core-api process %s.\n' "${API_PID}"
    kill "${API_PID}" >/dev/null 2>&1 || true
    wait "${API_PID}" >/dev/null 2>&1 || true
  fi
  if [[ "${RTO_DOCUMENT_STORAGE_ROOT_DIR:-}" == "" && -d "${DOC_ROOT}" ]]; then
    rm -rf "${DOC_ROOT}"
  fi
  if [[ -n "${DB_CONTAINER}" ]]; then
    printf '[release:e2e] Cleaning up PostgreSQL container %s.\n' "${DB_CONTAINER}"
    docker rm -f "${DB_CONTAINER}" >/dev/null 2>&1 || true
  fi
  exit "${exit_code}"
}
trap cleanup EXIT

case "${JDBC_URL}" in
  jdbc:postgresql://localhost:*|jdbc:postgresql://127.0.0.1:*)
    ;;
  *)
    if [[ "${RTO_RELEASE_ALLOW_NONLOCAL_PILOT_DB:-0}" != "1" ]]; then
      cat >&2 <<MSG
[release:e2e] BLOCKED: pilot E2E may rebuild pilot seed data and only runs by default
against localhost PostgreSQL URLs. Refusing JDBC URL:
  ${JDBC_URL}
Set RTO_RELEASE_ALLOW_NONLOCAL_PILOT_DB=1 only for an isolated test database.
MSG
      exit 30
    fi
    ;;
esac

port_must_be_free() {
  local port="$1"
  if (echo >/dev/tcp/127.0.0.1/"${port}") >/dev/null 2>&1; then
    printf '[release:e2e] BLOCKED: port %s is already in use.\n' "${port}" >&2
    exit 31
  fi
}

wait_for_api() {
  local deadline=$((SECONDS + 120))
  while (( SECONDS < deadline )); do
    if curl -fsS "http://127.0.0.1:${API_PORT}/health" >/dev/null 2>&1; then
      printf '[release:e2e] healthy: /health returned HTTP 200 on port %s.\n' "${API_PORT}"
      return 0
    fi
    if [[ -n "${API_PID}" ]] && ! kill -0 "${API_PID}" >/dev/null 2>&1; then
      printf '[release:e2e] crashed: core-api process exited before readiness.\n' >&2
      tail -n 120 "${LOG_FILE}" >&2 || true
      return 1
    fi
    printf '[release:e2e] still booting: waiting for /health on port %s.\n' "${API_PORT}"
    sleep 2
  done

  printf '[release:e2e] indeterminate: core-api did not become healthy before timeout.\n' >&2
  tail -n 120 "${LOG_FILE}" >&2 || true
  return 1
}

start_postgres_if_needed() {
  if [[ -z "${DB_CONTAINER}" ]]; then
    return 0
  fi

  port_must_be_free "${DB_PORT}"
  printf '[release:e2e] Starting isolated PostgreSQL container %s on port %s.\n' "${DB_CONTAINER}" "${DB_PORT}"
  docker run -d \
    --name "${DB_CONTAINER}" \
    -e POSTGRES_DB=recruiting_os \
    -e POSTGRES_USER="${DB_USER}" \
    -e POSTGRES_PASSWORD="${DB_PASSWORD}" \
    -p "127.0.0.1:${DB_PORT}:5432" \
    postgres:16-alpine >/dev/null

  local deadline=$((SECONDS + 60))
  while (( SECONDS < deadline )); do
    if docker exec "${DB_CONTAINER}" pg_isready -U "${DB_USER}" -d recruiting_os >/dev/null 2>&1; then
      printf '[release:e2e] healthy: PostgreSQL container is ready on port %s.\n' "${DB_PORT}"
      return 0
    fi
    printf '[release:e2e] still booting: waiting for PostgreSQL on port %s.\n' "${DB_PORT}"
    sleep 2
  done

  printf '[release:e2e] blocked: PostgreSQL container did not become ready before timeout.\n' >&2
  docker logs "${DB_CONTAINER}" >&2 || true
  return 1
}

port_must_be_free "${API_PORT}"
port_must_be_free "${WEB_PORT}"
mkdir -p "$(dirname "${LOG_FILE}")" "${DOC_ROOT}"
start_postgres_if_needed

cat <<MSG
[release:e2e] Expected startup signals:
- PostgreSQL readiness via pg_isready when the script starts its own database
- core-api log contains "Started CoreApiApplication"
- http://127.0.0.1:${API_PORT}/health returns HTTP 200
- Playwright web server serves http://127.0.0.1:${WEB_PORT}
[release:e2e] Using isolated release ports: API=${API_PORT}, WEB=${WEB_PORT}
[release:e2e] Using deterministic AI provider routes for local pilot E2E.
MSG

export SPRING_DATASOURCE_URL="${JDBC_URL}"
export SPRING_DATASOURCE_USERNAME="${DB_USER}"
export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"
export RTO_PILOT_DATA_JDBC_URL="${JDBC_URL}"
export RTO_PILOT_DATA_DB_USER="${DB_USER}"
export RTO_PILOT_DATA_DB_PASSWORD="${DB_PASSWORD}"

printf '[release:e2e] Rebuilding and validating deterministic synthetic pilot data.\n'
npm run pilot:data:rebuild
npm run pilot:data:validate

env \
  CORE_API_PORT="${API_PORT}" \
  SPRING_PROFILES_ACTIVE=staging \
  CORE_API_FLYWAY_ENABLED=true \
  SPRING_DATASOURCE_URL="${JDBC_URL}" \
  SPRING_DATASOURCE_USERNAME="${DB_USER}" \
  SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}" \
  RTO_AUTH_JWT_SECRET=task58_release_e2e_jwt_secret_32_bytes_min \
  RTO_DOCUMENT_STORAGE_ROOT_DIR="${DOC_ROOT}" \
  RTO_DOCUMENT_STORAGE_VIRUS_SCAN_MODE=noop \
  RTO_AI_DEEPSEEK_API_KEY=not-used-by-deterministic-release-e2e \
  RTO_AI_ROUTE_DEFAULT_PROVIDER=deterministic \
  RTO_AI_ROUTE_DEFAULT_MODEL=deterministic-pilot-local \
  RTO_AI_ROUTE_CANDIDATE_PROFILE_PROVIDER=deterministic \
  RTO_AI_ROUTE_CANDIDATE_PROFILE_MODEL=deterministic-pilot-local \
  RTO_AI_ROUTE_AUTHENTICITY_PROVIDER=deterministic \
  RTO_AI_ROUTE_AUTHENTICITY_MODEL=deterministic-pilot-local \
  RTO_AI_ROUTE_INTERVIEW_FEEDBACK_PROVIDER=deterministic \
  RTO_AI_ROUTE_INTERVIEW_FEEDBACK_MODEL=deterministic-pilot-local \
  RTO_FRONTEND_ORIGIN=https://release-e2e.local.test \
  RTO_PUBLIC_BASE_URL=https://release-e2e.local.test \
  RTO_DEPLOYMENT_DATABASE_MANAGED=false \
  RTO_OBJECT_STORAGE_PROVIDER=local-filesystem \
  RTO_OBJECT_STORAGE_LOCAL_ROOT_DIR="${DOC_ROOT}" \
  mvn -f services/core-api/pom.xml spring-boot:run >"${LOG_FILE}" 2>&1 &
API_PID=$!
printf '[release:e2e] started: core-api pid=%s, log=%s\n' "${API_PID}" "${LOG_FILE}"

wait_for_api

RTO_E2E_API_PORT="${API_PORT}" RTO_E2E_WEB_PORT="${WEB_PORT}" npm run test:e2e:pilot
printf '[release:e2e] PASS pilot browser E2E on API=%s WEB=%s; core-api process cleanup is registered.\n' "${API_PORT}" "${WEB_PORT}"
