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
- Focused privacy/security regressions cover weak login payloads, throttling,
  unsafe upload filenames, URL-path PII masking, and export permission
  enforcement.

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
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml org.owasp:dependency-check-maven:check
```

Current task validation is regression and build oriented. Any scanner findings
must be tracked to remediation or explicit risk acceptance before production
security claims.

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
