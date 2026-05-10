# Task 52 Production Security Compliance Baseline

## Scope

Task 52 upgrades the Task 41 controlled-pilot hardening work into a production
security compliance baseline. It is a baseline and evidence register, not a SOC
2 report, ISO certification, public penetration-test attestation, or statement
that every future production control is finished.

No 100% production-security claim is allowed unless every issue found by the
baseline review, dependency scan, and security regression suite is either:

- Status: CLOSED with the closing evidence named here.
- Status: RISK_ACCEPTED with a bounded rationale, owner, and review date.

## Threat Model

Primary assets:

- Candidate identity, contact details, compensation, motivation, consent, and
  disclosure history.
- Client company requirements, commercial terms, feedback, and interview data.
- Claim Ledger, ReviewEvent, WorkflowEvent, DisclosureRecord, access audit, and
  governance configuration history.
- Source documents and parsed document artifacts.
- AI task prompts, model outputs, routing configuration, and tool execution
  envelopes.

Primary actors:

- Owner / Partner: business visibility, risk oversight, audit review.
- Consultant: operational workflow owner with privileged recruiting access.
- Client: external customer with client-safe shortlist and feedback access.
- Candidate: self-scoped profile, opportunity, and consent access.
- Admin / System: governance, policy, audit, retention, and security operation.
- AI Assistant / automation: task execution without authority to confirm facts,
  disclose identity, unlock contact data, or bypass service-layer gates.

Baseline threats and required controls:

| Threat | Baseline control |
| --- | --- |
| Client reads raw Candidate or PII before unlock | Role/field access policy, client-safe projection tests, five-portal boundary regressions. |
| Cross-organization access or inference | Same-organization access checks; Task 51 owns tenant-wide hardening beyond focused Task 52 regression coverage. |
| Unauthorized disclosure, unlock, export, or approve action | PermissionEnforcer deny-by-default tests and persistent access-audit recording for wired sensitive surfaces. |
| AI writes confirmed facts or performs sensitive actions | Truth-layer gates, review events, workflow audit, and AI policy regression suite. |
| Unsafe document upload or source artifact exposure | Filename/path validation, virus-scan port enforcement, consultant document permission checks, and rate limits. |
| PII leaks into request logs or client-safe summaries | Request log path masking, redaction tests, shortlist re-identification gates, and client-safe projection tests. |
| Dependency vulnerability exposure | `npm audit --omit=dev` plus `security:core-api:dependency-check` workflow and tracked remediation decisions. |
| Lost accountability during access, retention, or incident review | Append-only audit ledgers, access-review runbook, data-retention runbook, and issue register below. |
| Secret exposure or stale provider credentials | Secret handling policy, rotation runbook, no-commit rule, and environment-level revocation procedure. |

## Access Review Process

Cadence:

- Monthly for pilot/private production.
- Before onboarding a new organization, privileged admin, AI tool route, or
  sensitive export surface.
- Immediately after any security incident, denied-access anomaly, or permission
  model change.

Review steps:

1. Export current users, roles, organization membership, and active service or
   automation principals from the identity source.
2. Confirm every user has a business owner, organization scope, and role that
   matches current duties.
3. Review admin, owner, consultant, export, disclosure, unlock, and AI routing
   privileges first.
4. Sample recent `audit.audit_log` records for allowed and denied sensitive
   access. Any unexplained allow becomes a blocking security issue.
5. Remove stale accounts before the review is considered closed.
6. Record the review date, reviewer, evidence links, removed privileges, and any
   accepted risk in the issue register.

Required evidence:

- Reviewer identity and date.
- User/role inventory source.
- Access changes made.
- Audit samples reviewed.
- Open issues with Status: CLOSED or Status: RISK_ACCEPTED.

## Privacy and Data Retention Runbook

Principles:

- Governance ledgers, WorkflowEvents, ReviewEvents, consent/disclosure records,
  access-denial evidence, and security investigation records are append-only and
  cannot be deleted by normal product workflows.
- Candidate/client privacy requests must preserve legal hold, consent,
  disclosure, fee-protection, and audit proof.
- Task 46 data-lifecycle controls are the execution baseline for retention and
  deletion tombstones. Task 52 does not add a destructive retention executor.

Request handling:

1. Classify the request as export, correction, deletion, restriction, or legal
   hold.
2. Verify requester identity and organization/self scope before exposing data.
3. Identify protected audit records that must be retained.
4. Run the data-lifecycle review path for eligible data rather than manual
   database edits.
5. Record the workflow/audit event, reviewer, reason, and outcome.
6. If data cannot be deleted, record the legal/audit reason and next review
   date.

Retention baselines:

| Data class | Baseline decision |
| --- | --- |
| WorkflowEvent, ReviewEvent, Claim Ledger, consent, disclosure, access audit | Retain append-only for legal, transaction, security, and audit proof. |
| Source documents and parsed artifacts | Retain while transaction is active; later purge requires data-lifecycle tombstone and legal-hold check. |
| Security scan reports and access reviews | Retain with release/security evidence for audit traceability. |
| AI prompts and model outputs containing personal data | Retain only when needed for audit/eval; future production retention windows must be configured before public SaaS use. |

## Key and Secret Rotation Runbook

Secret classes:

- Database credentials.
- JWT signing or verification material.
- AI provider keys.
- SMTP/SMS/object-storage credentials.
- Dependency scanner keys such as `NVD_API_KEY`.
- CI, deployment, and support/admin credentials.

Rotation triggers:

- Scheduled production rotation at least every 90 days for provider/API keys and
  immediately for leaked, over-scoped, departed-owner, or suspicious credentials.
- Any repo, log, ticket, chat, artifact, or browser exposure of a secret.
- Vendor, AI provider, CI, or hosting security advisory requiring rotation.

Procedure:

1. Create a replacement secret in the managed secret store.
2. Deploy the replacement with overlap where the protocol supports dual keys.
3. Verify health, authentication, AI provider smoke tests, and security scans.
4. Revoke the old secret.
5. Confirm no plaintext secret appears in git, logs, issue trackers, or exported
   artifacts.
6. Record owner, rotated secret class, verification command, and revocation time.

Incident rule:

- If a secret may have been committed, do not only delete it from the latest
  commit. Revoke it first, then remove it from history according to the incident
  plan.

## Dependency Vulnerability Remediation Workflow

Required local baseline commands:

```sh
rtk npm audit --omit=dev
rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check
```

Default dependency-check mode should use `NVD_API_KEY`. The prewarmed-cache mode
is acceptable for local review gates when the cache is prepared and the wrapper
creates both expected reports:

- `services/core-api/target/dependency-check-report.json`
- `services/core-api/target/dependency-check-report.html`

Remediation process:

1. Classify findings by exploitability against used runtime paths, not package
   presence alone.
2. Upgrade the narrowest dependency range that fixes the issue without broad
   dependency churn.
3. If no safe upgrade exists, document compensating control, owner, and review
   date as Status: RISK_ACCEPTED.
4. Re-run the audit/scan and affected test suite.
5. Close the finding only when the verification command and evidence are named.

## Pen-Test Issue Remediation Tracking

Pen-test, red-team, manual review, and security scan findings use one issue
register format:

| Field | Required content |
| --- | --- |
| ID | Stable `T52-SEC-###` identifier. |
| Source | Baseline review, npm audit, dependency-check, manual code review, pen test, incident. |
| Severity | Critical, high, medium, low, informational. |
| Affected surface | Portal, API, database, AI task, document upload, dependency, runbook. |
| Owner | Named responsible role or person before closure. |
| Status | OPEN, CLOSED, or RISK_ACCEPTED. |
| Evidence | Commit, test, scan result, runbook section, or dated acceptance. |
| Review date | Required for OPEN and RISK_ACCEPTED items. |

Blocking rule:

- Critical/high findings cannot be risk-accepted without owner approval,
  compensating controls, and a dated re-review. Public production launch is
  blocked while any critical/high item is OPEN.

## Security Regression Suite

Minimum Task 52 gate:

```sh
rtk git diff --check
rtk docker info
rtk mvn -f services/core-api/pom.xml -Dtest=SecurityComplianceBaselineDocumentationTest,SensitiveEndpointRateLimitFilterTest,JwtAuthenticationFilterTest,FivePortalBoundaryRegressionTest,PermissionEnforcerTest,JdbcAccessAuditRecorderPostgresIntegrationTest test
rtk mvn -f services/core-api/pom.xml test
rtk npm audit --omit=dev
rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check
```

Suite coverage:

| Test or scan | Covered baseline |
| --- | --- |
| SensitiveEndpointRateLimitFilterTest | Auth and sensitive document endpoint throttling. |
| JwtAuthenticationFilterTest | JWT parsing/authentication fail-closed behavior. |
| FivePortalBoundaryRegressionTest | Five-portal permission and client raw-candidate boundary. |
| PermissionEnforcerTest | Deny-by-default permission behavior and access-audit calls. |
| JdbcAccessAuditRecorderPostgresIntegrationTest | Persistent immutable access-audit recording. |
| SecurityComplianceBaselineDocumentationTest | Task 52 evidence, issue tracking, and scan/runbook presence. |
| Full `services/core-api` Maven suite | Existing privacy, redaction, upload, truth-layer, workflow, and governance regressions. |
| `npm audit --omit=dev` | JavaScript dependency vulnerability baseline. |
| `security:core-api:dependency-check` | Core API dependency vulnerability baseline. |

## Baseline Issue Register

| ID | Source | Severity | Affected surface | Owner | Status | Evidence | Review date |
| --- | --- | --- | --- | --- | --- | --- | --- |
| T52-SEC-001 | Task 41 gap review | Medium | MFA, SSO/OIDC, persistent lockout, password reset, email verification | Security owner | Status: RISK_ACCEPTED | Not required for this Task 52 branch; requires identity product decision before public SaaS. Existing auth payload and JWT regressions remain in suite. | 2026-06-30 |
| T52-SEC-002 | Task 41 gap review | Medium | Distributed/gateway rate limiting | Security owner | Status: RISK_ACCEPTED | Current in-memory sensitive endpoint rate limits are regression-tested; multi-node gateway enforcement remains future production infrastructure work. | 2026-06-30 |
| T52-SEC-003 | Task 41 gap review | Medium | Product-wide field-level access audit rollout | Security owner | Status: RISK_ACCEPTED | Task 41 wired consultant document and disclosure export access-audit paths; full field-level expansion must be planned without conflicting with Task 51 tenant hardening. | 2026-06-30 |
| T52-SEC-004 | Task 52 baseline | Low | Missing formal threat model, access review, retention, secret rotation, dependency, and pen-test runbooks | Task 52 branch | Status: CLOSED | Closed by this document and README linkage. | 2026-05-10 |
| T52-SEC-005 | Task 52 baseline | Low | Missing runnable regression protecting the compliance baseline artifact | Task 52 branch | Status: CLOSED | Closed by `SecurityComplianceBaselineDocumentationTest`. | 2026-05-10 |
| T52-SEC-006 | Task 52 verification | Medium | Dependency scan result for this branch | Task 52 branch | Status: CLOSED | `rtk npm audit --omit=dev` found 0 vulnerabilities; `rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check` exited 0, generated JSON/HTML reports, and report JSON showed 0 dependencies with vulnerabilities. | 2026-05-10 |
| T52-SEC-007 | Dependency-Check local scanner limitation | Low | Optional .NET Assembly Analyzer unavailable in local Java core-api scan | Task 52 branch | Status: RISK_ACCEPTED | Dependency-Check logged missing `dotnet` runtime but completed with `BUILD SUCCESS`; core-api is a Java service and the JSON report showed 0 vulnerable dependencies. Re-review if .NET binaries become part of the scanned artifact set. | 2026-06-30 |
| T52-SEC-008 | Baseline review | Medium | AI prompt/model-output retention window | Security owner | Status: RISK_ACCEPTED | Task 52 documents the privacy handling rule and relies on Task 46 lifecycle review paths; exact production retention windows and enforcement are a public-SaaS governance dependency before launch. | 2026-06-30 |

## Task 52 Verification Evidence

The final Task 52 closeout must record fresh command results for:

- `rtk git diff --check`
- `rtk docker info`
- Focused security Maven test command from the security regression suite.
- `rtk mvn -f services/core-api/pom.xml test`
- `rtk npm audit --omit=dev`
- `rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check`

If dependency-check cannot run because no NVD key or prepared cache exists, the
branch is not allowed to claim the dependency scan passed. The finding must be
left OPEN or explicitly Status: RISK_ACCEPTED with owner and review date.

Current branch evidence captured on 2026-05-10:

| Command | Result |
| --- | --- |
| `rtk git diff --check` | Exit 0. |
| `rtk docker info` | Exit 0; Docker Desktop server 29.4.1 reachable for Testcontainers. |
| `rtk npm audit --omit=dev` | Exit 0; found 0 vulnerabilities. |
| `rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check` | Exit 0; Maven `BUILD SUCCESS`; JSON and HTML reports generated under `services/core-api/target/`; JSON vulnerability dependency count was 0. |
| `rtk mvn -f services/core-api/pom.xml -Dtest=SecurityComplianceBaselineDocumentationTest test` | Exit 0; 4 tests run, 0 failures, 0 errors. |
| `rtk mvn -f services/core-api/pom.xml -Dtest=SecurityComplianceBaselineDocumentationTest,SensitiveEndpointRateLimitFilterTest,JwtAuthenticationFilterTest,FivePortalBoundaryRegressionTest,PermissionEnforcerTest,JdbcAccessAuditRecorderPostgresIntegrationTest test` | Exit 0; 35 tests run, 0 failures, 0 errors. |
| `rtk mvn -f services/core-api/pom.xml test` | Exit 0; 1066 tests run, 0 failures, 0 errors, 3 skipped. |
