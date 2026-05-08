#!/usr/bin/env bash
set -euo pipefail

readonly REPORT_DIR="services/core-api/target"
readonly REPORT_JSON="${REPORT_DIR}/dependency-check-report.json"
readonly REPORT_HTML="${REPORT_DIR}/dependency-check-report.html"

maven_args=(-f services/core-api/pom.xml org.owasp:dependency-check-maven:check)

if [[ -n "${NVD_API_KEY:-}" ]]; then
  :
elif [[ "${DEPENDENCY_CHECK_PREWARMED_CACHE:-}" == "1" ]]; then
  maven_args+=(-DautoUpdate=false)
elif [[ "${DEPENDENCY_CHECK_ALLOW_SLOW_UNKEYED:-}" == "1" ]]; then
  cat >&2 <<'MSG'
Running OWASP dependency-check without NVD_API_KEY. First-run NVD updates can be
very slow and may hit rate limits.
MSG
else
  cat >&2 <<'MSG'
NVD_API_KEY is required for the default Task 41 Maven dependency-check gate.

Allowed alternatives:
- DEPENDENCY_CHECK_PREWARMED_CACHE=1 uses the existing local Dependency-Check
  data cache with -DautoUpdate=false.
- DEPENDENCY_CHECK_ALLOW_SLOW_UNKEYED=1 intentionally runs the slow unkeyed NVD
  update path.
MSG
  exit 2
fi

PATH=/opt/homebrew/bin:$PATH mvn "${maven_args[@]}"

if [[ ! -s "${REPORT_JSON}" || ! -s "${REPORT_HTML}" ]]; then
  cat >&2 <<MSG
Dependency-Check did not produce the expected Task 41 baseline reports:
- ${REPORT_JSON}
- ${REPORT_HTML}
MSG
  exit 3
fi
