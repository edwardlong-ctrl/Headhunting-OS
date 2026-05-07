#!/usr/bin/env bash
set -euo pipefail

COMMAND="${1:-validate}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

exec mvn \
  -f "${ROOT_DIR}/services/core-api/pom.xml" \
  -q \
  -DskipTests \
  -Dexec.mainClass=com.recruitingtransactionos.coreapi.pilotdata.PilotDataCliApplication \
  -Dexec.args="${COMMAND}" \
  compile \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java
