# Task 49 Integrations v1

## Scope

Task 49 adds backend integration boundaries for operating channels without
turning integrations into sources of truth.

Implemented behavior:

- Email, SMS, calendar, OCR/STT, ATS/HRIS, and webhook/event provider ports now
  exist under `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/integration`.
- Default providers report `UNCONFIGURED` or `PRODUCTION_PLACEHOLDER` status
  honestly. They do not fake provider success.
- Outbound integration sends require organization id, actor id, same-organization
  actor/target scope, a non-blank reason, and an explicit redaction/disclosure
  decision.
- Outbound integration attempts produce audit evidence through an
  `IntegrationAuditRecorder`; the JDBC adapter writes tenant-scoped rows into
  `audit.audit_log` with provider status, safe status code, payload kind, and
  redaction decision metadata.
- Email and WeChat safe-summary export is represented as a narrow
  `SAFE_SUMMARY_EXPORT` payload, not a broad report/export package.
- Raw candidate payload fields are blocked before disclosure/unlock unless the
  command carries an explicit `DISCLOSURE_UNLOCK_CONFIRMED` decision and
  `DISCLOSED` state.
- Safe-summary payload text carrying raw email/phone identity markers is also
  blocked before disclosure/unlock.
- Inbound integration commands route through a review-first sink that can create
  `SourceItem` and `InformationPacket` records via the governed intake service.
  The result is `ACCEPTED_FOR_REVIEW`; it never reports a confirmed fact write.
- Inbound commands requesting direct confirmed-fact/canonical writes fail
  closed before governed-intake mutation.
- Webhook inbound handling validates same-organization route scope, schema
  version, JSON-object payload shape, and idempotency key. When no idempotency
  key is supplied, it derives a deterministic event key from org/provider/type
  and payload.
- ATS/HRIS v1 is only a provider boundary plus safe mapping contract. Mappings
  to `canonical.*` or confirmed-fact write targets are rejected.

## Governance Boundary

External inputs are not facts. Integration payloads enter the governed intake
path as source material for later extraction, claim generation, and review.

Outbound commands are audited before they can be treated as externally
deliverable. Sensitive outbound payloads fail closed unless the caller supplies
the disclosure/unlock decision state required by the privacy boundary.

## Out of Scope

- No Task 55 import engine, bulk migration workflow, or legacy ATS/CRM data
  migration runner.
- No Task 56 support retry tooling, dead-letter queue, or operator retry UI.
- No Task 57 broad reporting, legal export package, or PDF report framework.
- No production vendor credentials, failover, or provider-specific delivery
  retry semantics.
- No OCR/STT execution worker; the port exists and the placeholder reports
  production-placeholder status.

## Verification Evidence

Focused Task 49 TDD loop:

- `rtk mvn -f services/core-api/pom.xml -Dtest=IntegrationProviderPlaceholderTest,InboundIntegrationBoundaryServiceTest,OutboundIntegrationBoundaryServiceTest,WebhookIntegrationBoundaryServiceTest,AtsHrisIntegrationBoundaryTest,JdbcIntegrationAuditRecorderPostgresIntegrationTest,IntegrationPublicApiContractTest test`
- Result after review fixes: 18 tests, 0 failures, 0 errors, 0 skipped.

Full required validation for Task 49 is recorded in the final handoff for the
commit that adds this file.
