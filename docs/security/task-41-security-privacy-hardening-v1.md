# Task 41 Security and Privacy Hardening v1

## Scope

This is a controlled-pilot hardening baseline. It is not a production security
certification, SOC 2 claim, penetration-test result, or public-SaaS readiness
statement.

## Delivered Controls

- Auth/login policy rejects malformed emails, weak short passwords, overlong
  passwords, and control characters before authentication service execution.
- Sensitive endpoint rate limiting now covers auth login/refresh and consultant
  document endpoints with configurable in-memory windows:
  - `rto.security.rate-limit.enabled`
  - `rto.security.rate-limit.auth.max-attempts`
  - `rto.security.rate-limit.auth.window-seconds`
  - `rto.security.rate-limit.document.max-attempts`
  - `rto.security.rate-limit.document.window-seconds`
- File upload security now rejects path traversal, path separator, and control
  character original filenames before storage or source-item persistence.
- Request logs mask UUID and email-like path segments in logged route fields.
  Query strings and request bodies remain excluded from normal request logs.
- Disclosure audit export now uses explicit same-organization Admin `EXPORT`
  permission on `DISCLOSURE_RECORD` instead of reusing a generic governance
  read policy.
- Access decisions on Task 41 sensitive surfaces now have a persistent audit
  path. `PermissionEnforcer.requireAllowed(request, auditContext)` records
  allow/deny decisions to `audit.audit_log` through `JdbcAccessAuditRecorder`.
  The first wired surfaces are consultant source-document access
  (upload/download/parse/parsed/evidence) and Admin disclosure-audit export.
- Focused privacy/security regressions cover weak login payloads, throttling,
  unsafe upload filenames, URL-path PII masking, export permission enforcement,
  persistent access-audit recording, and endpoint-level rate-limit coverage for
  auth refresh plus consultant document upload/download/parse/parsed/evidence.

## Data Retention Policy Baseline

- Governance ledgers, WorkflowEvents, ReviewEvents, consent/disclosure records,
  access-denial evidence, and security investigation records are append-only and
  must not be deleted by normal product workflows.
- Source documents and parsed-document artifacts are retained for the active
  recruiting transaction and follow the future data-retention/deletion task
  before any automated purge is introduced.
- Candidate/client export or deletion requests must be implemented through a
  later domain service that preserves audit proof, consent/disclosure evidence,
  and legal hold semantics.
- This task deliberately adds no destructive retention executor.

## Vulnerability Scan Baseline

Run these before claiming pilot security readiness:

```sh
npm audit --omit=dev
NVD_API_KEY=... npm run security:core-api:dependency-check
```

The Maven scan is pinned in `services/core-api/pom.xml` via
`org.owasp:dependency-check-maven:12.2.0` and reads the NVD API key through
`nvdApiKeyEnvironmentVariable`. The wrapper fails fast by default when
`NVD_API_KEY` is absent because unkeyed first-run NVD updates are too slow for
review gates and can appear stalled. Explicit alternatives are
`DEPENDENCY_CHECK_PREWARMED_CACHE=1` for a prepared local data cache or
`DEPENDENCY_CHECK_ALLOW_SLOW_UNKEYED=1` for a deliberate slow unkeyed update.
The wrapper also fails if the expected HTML and JSON baseline reports are not
created. Any scanner findings must be tracked to remediation or explicit risk
acceptance before production security claims.

## Remaining Gaps

- No MFA, lockout persistence, password reset, email verification, SSO/OIDC, or
  multi-organization membership switching.
- Rate limiting is in-memory and per-node; distributed or gateway-level rate
  limiting remains future work.
- No complete product-wide PII logging audit exists.
- No automated data retention or deletion workflow exists.
- No formal vulnerability remediation report or penetration test exists.
- File malware scanning still depends on the configured `VirusScanPort`; the
  baseline rejects unsafe scanner results but does not ship a production scanner.
- Access audit is wired for Task 41 sensitive document/export surfaces; it is
  not yet a product-wide field-level access-audit rollout for every API.
