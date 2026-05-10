#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

TESTS=(
  FivePortalBoundaryRegressionTest
  PermissionEnforcerTest
  AccessControlContractTest
  SupportImpersonationPolicyTest
  AdminObservabilityControllerPolicyTest
  ConsultantWriteOrgIsolationIntegrationTest
  JdbcAccessAuditRecorderPostgresIntegrationTest
  SensitiveEndpointRateLimitFilterTest
  JwtAuthenticationFilterTest
  DocumentUploadServiceTest
  ClientSafeCandidateProjectionServiceTest
  ClientSafeProjectionContractTest
  ClientSafeSummaryPipelineTest
  ClientSafeCandidateCardControllerTest
  ClientSafeCandidateCardPostgresQueryPortTest
  ConsentDisclosureProtectionPolicyTest
  ConsentDisclosureServiceTest
  UnlockWorkflowServiceTest
  ConsultantUnlockControllerTest
  PilotDataPrivacyValidatorTest
)

printf '[release:privacy-security] Running tenant, client-safe, disclosure/unlock, audit, auth, and upload negative regressions.\n'
mvn -f services/core-api/pom.xml -Dtest="$(IFS=,; echo "${TESTS[*]}")" test
