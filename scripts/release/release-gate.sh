#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

run_gate() {
  local gate_name="$1"
  shift
  printf '\n[release-gate] START %s\n' "${gate_name}"
  "$@"
  printf '[release-gate] PASS %s\n' "${gate_name}"
}

if [[ "${RTO_RELEASE_SKIP_BROWSER_E2E:-0}" == "1" ]]; then
  if [[ -z "${RTO_RELEASE_E2E_EVIDENCE:-}" ]]; then
    cat >&2 <<'MSG'
[release-gate] BLOCKED Browser E2E was skipped without evidence.
Set RTO_RELEASE_E2E_EVIDENCE to a readable signed evidence artifact or run:
  npm run release:e2e:pilot
MSG
    exit 20
  fi
  if [[ ! -r "${RTO_RELEASE_E2E_EVIDENCE}" ]]; then
    cat >&2 <<MSG
[release-gate] BLOCKED Browser E2E evidence artifact is not readable:
  ${RTO_RELEASE_E2E_EVIDENCE}
Run npm run release:e2e:pilot, or provide a readable risk-acceptance artifact.
MSG
    exit 21
  fi
  for required_pattern in \
    "signed risk acceptance" \
    "owner:" \
    "reason:" \
    "expir" \
    "rollback" \
    "release:e2e:pilot"; do
    if ! grep -Eiq "${required_pattern}" "${RTO_RELEASE_E2E_EVIDENCE}"; then
      cat >&2 <<MSG
[release-gate] BLOCKED Browser E2E evidence artifact is missing required content:
  pattern: ${required_pattern}
  file: ${RTO_RELEASE_E2E_EVIDENCE}
MSG
      exit 22
    fi
  done
  printf '[release-gate] Browser E2E signed risk-acceptance artifact accepted: %s\n' "${RTO_RELEASE_E2E_EVIDENCE}"
fi

run_gate "Backend regression" npm run test:core-api
run_gate "Frontend unit regression" npm --workspace @rto/web run test
run_gate "Frontend typecheck" npm run typecheck:web
run_gate "Frontend build" npm run build:web
run_gate "Migration validation" npm run release:migrations
run_gate "Privacy/security negative" npm run release:privacy-security
run_gate "AI eval regression" npm run release:ai-eval

if [[ "${RTO_RELEASE_SKIP_BROWSER_E2E:-0}" == "1" ]]; then
  printf '[release-gate] PASS Browser E2E evidence gate via RTO_RELEASE_E2E_EVIDENCE\n'
else
  run_gate "Browser E2E" npm run release:e2e:pilot
fi

cat <<'MSG'

RELEASE_READY: Task 58 release regression gates passed for this checkout.
This is a release safety-system result, not a public readiness or final acceptance claim.
MSG
