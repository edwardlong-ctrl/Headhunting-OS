# RC1 Pilot Readiness Audit - Privacy, Security, Authorization, And Pilot Risk

## Scope And Evidence Read

This report is a bounded main-thread fallback because the privacy/security subagent failed to produce a report after multiple attempts. It is not a full exploit-style security review and no tests were run.

Evidence read:

- `docs/release/RC1-pilot-readiness-plan.md`
- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/release/release-checklist.md`
- `scripts/release/privacy-security-regression.sh`
- `scripts/release/release-gate.sh`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ClientSafeCandidateCardController.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/AuditedPostgresClientSafeCandidateCardQueryPort.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/ClientSafeCandidateCardControllerTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/ClientSafeCandidateCardPostgresQueryPortTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureProtectionPolicyTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureRegressionClosureTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/pilotdata/PilotDataPrivacyValidatorTest.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotdata/PilotDataPrivacyValidator.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/identityauth/SensitiveEndpointRateLimitFilterTest.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/SecurityConfig.java`

## Risk Matrix

| Risk | RC1 plan coverage | Current code/test evidence | Gap |
| --- | --- | --- | --- |
| Client reads raw `Candidate` before unlock/disclosure | Listed as invariant and P0 proof row in `RC1-pilot-readiness-plan.md:47`, `370` | Client-safe controller uses anonymous-card endpoint and authenticated principal in `ClientSafeCandidateCardController.java:18-40`; tests assert raw IDs, PII, internal notes, governance objects, and raw type names are absent in `ClientSafeCandidateCardControllerTest.java:94-140`, `183-244` | Plan should require the exact negative API/browser evidence artifact, not only the selected backend test command. |
| Client-safe query misses re-identification/audit trail | Plan requires client-safe/unlock/disclosure E1 plus E3/E4 and audit evidence in `RC1-pilot-readiness-plan.md:140`, `370-372` | Audited query path evaluates `RedactionAuditService`, blocks when redaction audit fails, and projects with client-safe access in `AuditedPostgresClientSafeCandidateCardQueryPort.java:127-144`; integration test counts persisted assessment and workflow event in `ClientSafeCandidateCardPostgresQueryPortTest.java:130-142` | Add required artifact fields for assessment ref, redaction decision, and WorkflowEvent ID. |
| Consent/disclosure bypass | Golden path includes consent, unlock, consultant approval, and post-unlock disclosure in `RC1-pilot-readiness-plan.md:82-87`, `356-358` | Protection policy denies raw candidate/profile access, requires consent, approved unlock, approved disclosure, and audit boundary in `ConsentDisclosureProtectionPolicyTest.java:33-87`, `129-217` | RC1 evidence log should require profile version, consent text version, unlock decision ID, disclosure record ID, human approver, and WorkflowEvent ID. |
| Browser/UI privacy regression not proved | RC1-06 runs backend privacy/security negatives; RC1-07/09 exercise browser/manual flows in `RC1-pilot-readiness-plan.md:843-908`, `911-970`, `1083-1169` | `privacy-security-regression.sh` selects backend Maven tests only in `scripts/release/privacy-security-regression.sh:7-31` | Add explicit Playwright negative assertions for pre-unlock client surfaces and post-disclosure boundary, or require RC1-07/09 to attach those screenshots/API/DOM assertions. |
| Tenant/auth boundary regression | Release checklist treats tenant boundary as privacy gate in `docs/release/release-checklist.md:14`, `35-36` | Security config authenticates `/api/**` except auth/health in `SecurityConfig.java:46-50`; rate-limit test covers login, refresh, document endpoints in `SensitiveEndpointRateLimitFilterTest.java:19-82`; selected privacy script includes tenant/access/auth tests in `privacy-security-regression.sh:7-17` | Add a role/route guard matrix to the plan so pilot users cannot rely on scattered test names. |
| Unsafe pilot data or accidental real PII in evidence | Plan requires deterministic synthetic pilot data and forbids secrets/raw PII in artifacts in `RC1-pilot-readiness-plan.md:26`, `118`, `162-171` | Validator permits reserved test domains and rejects public profile URLs and real high-signal company names in `PilotDataPrivacyValidator.java:18-33`, `71-124`; test proves default dataset passes and unsafe sample fails in `PilotDataPrivacyValidatorTest.java:11-50` | The validator is static and cannot prove absence of all real PII. RC1 should require a signed synthetic-data statement plus validator output. |

## Severity-Ranked Findings

### P1 - RC1 privacy/security is mostly backend-negative-test driven; browser privacy evidence is not first-class enough

Observed evidence: RC1-06 runs `rtk npm run release:privacy-security` and expects selected negative tests to cover tenant boundary, client-safe projection, disclosure/unlock, access audit, auth/rate-limit, upload hardening, and pilot privacy regressions (`RC1-pilot-readiness-plan.md:843-908`). The underlying script is a Maven selected-test runner only (`scripts/release/privacy-security-regression.sh:7-31`). The plan separately expects `Client-safe/unlock/disclosure privacy: E1 plus E3/E4` (`RC1-pilot-readiness-plan.md:140`).

Inference: The plan knows E1 is insufficient, but execution can still leave privacy evidence split across RC1-06, RC1-07, and RC1-09 without a single privacy-specific browser/API proof row.

Likely cause: The release gate grew from backend regression safety, while pilot readiness now needs user-visible privacy proof.

Recommended change: Add an RC1-06B or explicit RC1-07 subsection named `Browser privacy negative proof` with required artifacts:

- pre-unlock client page/API response does not contain name, email, phone, LinkedIn, exact employer, exact project/product/chip, raw source text, consultant notes, candidate/profile UUIDs, `WorkflowEvent`, `ClaimLedger`, or internal governance object names;
- raw candidate/profile UUID route attempts return safe denial;
- cross-organization anonymous-card ref returns `404`/safe unavailable, not raw data;
- post-disclosure identity access requires consent, unlock, consultant approval, and audit evidence.

### P1 - Consent/disclosure evidence rows need transaction-grade IDs and versions

Observed evidence: v2.1 requires every consent to record profile version and consent text version, and every contact unlock to create a `DisclosureRecord` (`product-spec-v2.1.md:899-910`). RC1 tracks consent/unlock/disclosure IDs in the transaction trace ledger (`RC1-pilot-readiness-plan.md:300-302`, `1139`), but the evidence template does not force profile version, consent text version, human approver, approval reason, disclosure level, or WorkflowEvent ID for each transition. The code policy tests show these details matter: confirmed consent, approved unlock, approved disclosure, and audit boundary are all required for L4 identity disclosure (`ConsentDisclosureProtectionPolicyTest.java:129-217`).

Inference: An implementer could record "consent ID exists" and "disclosure approved" without proving versioned consent scope and auditable human approval.

Likely cause: The plan captures the object chain but not the minimum columns for a legal/privacy proof record.

Recommended change: Expand the transaction trace ledger rows for consent/unlock/disclosure into a mini table:

| Step | Record ID | Profile version | Consent text version | Disclosure level | Human approver | Reason | WorkflowEvent ID | Evidence artifact |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

### P1 - Client-safe re-identification audit is present in code but not required as an RC1 artifact

Observed evidence: The audited Postgres query path evaluates `RedactionAuditService`, blocks when audit fails, and only then projects the client-safe card (`AuditedPostgresClientSafeCandidateCardQueryPort.java:127-144`). The integration test expects the query to persist a re-identification assessment and a `REIDENTIFICATION_RISK_ASSESSED` workflow event (`ClientSafeCandidateCardPostgresQueryPortTest.java:130-142`).

Inference: RC1 should not stop at "client-safe card rendered"; it should capture the risk assessment and workflow event that prove the privacy gate actually executed.

Likely cause: RC1 evidence rows are capability-oriented, not privacy-control-oriented.

Recommended change: In RC1-07/09, require `assessment_ref`, redaction level, blocked/allowed result, and WorkflowEvent ID for each client-safe shortlist/card read used in the golden path.

### P1 - Route/role authorization is not summarized into a pilot guard matrix

Observed evidence: Security config requires authentication for `/api/**` except `/api/auth/**` and `/health` (`SecurityConfig.java:46-50`), and selected privacy tests include access-control/auth/rate-limit surfaces (`privacy-security-regression.sh:7-17`). Release checklist names tenant boundary and raw Candidate leakage as non-waivable (`docs/release/release-checklist.md:14`, `35-36`).

Inference: The underlying test surface exists, but the RC1 plan does not give the implementer a role/route matrix to prove owner, consultant, client, candidate, and admin pilot accounts are constrained correctly.

Likely cause: Authorization is treated as test-suite coverage rather than pilot acceptance evidence.

Recommended change: Add a `Pilot Role/Route Guard Matrix` to `RC1-pilot-readiness-evidence.md` with allowed/denied outcomes for the five pilot accounts against client-safe card, disclosed profile, consent, unlock, placement, commission, owner revenue, admin audit, and document endpoints.

### P2 - Synthetic data policy is good but should be tied to a signed run artifact

Observed evidence: RC1 requires deterministic synthetic data and bans real candidate/client/production data and raw PII in artifacts (`RC1-pilot-readiness-plan.md:26`, `118`, `162-171`). `PilotDataPrivacyValidator` rejects unsupported email domains, public profile URLs, and real high-signal company names (`PilotDataPrivacyValidator.java:18-33`, `71-124`); tests prove default pass and unsafe sample failure (`PilotDataPrivacyValidatorTest.java:11-50`).

Inference: The validator is useful but intentionally incomplete; static string checks cannot prove there is no real PII or no high-signal identity fingerprint.

Likely cause: The plan treats synthetic-data validation as a command result, not an accountable data-handling attestation.

Recommended change: RC1-03A should write a `synthetic-data-attestation` artifact with dataset source, validator command result, no-real-data statement, approved operator, and redaction policy for screenshots/API excerpts.

## Missing Evidence / Not Verified

- `rtk npm run release:privacy-security` was not run in this audit.
- Browser E2E/manual pre-unlock privacy behavior was not run.
- No current API response, screenshot, DB row, access audit row, redaction assessment row, or WorkflowEvent row was collected.
- Docker/Testcontainers availability remains a blocker from the QA/Ops report, so Postgres-backed privacy integration evidence is not current.
- This was a bounded fallback audit; it did not review every auth controller, every route guard, every migration, or every frontend component.

## Suggested RC1 Privacy/Security Gate Checklist

1. Run `rtk npm run release:privacy-security` and store the exact pass/fail excerpt in `docs/release/rc1-artifacts/RC1-06-privacy-security.txt`.
2. Add `RC1-06B Browser Privacy Negative Proof` before or inside RC1-07:
   - client anonymous-card page/API contains no raw identity/contact/internal/governance strings;
   - raw candidate/profile IDs fail closed;
   - cross-org anonymous-card refs fail closed;
   - denied responses contain safe reasons only.
3. Add consent/disclosure evidence columns: record ID, profile version, consent text version, disclosure level, human approver, reason, WorkflowEvent ID, artifact path.
4. Capture re-identification assessment evidence for every client-safe card used in the golden path: assessment ref, redaction level, allowed/blocked result, WorkflowEvent ID.
5. Add five-portal pilot role/route guard matrix with allowed/denied outcomes for owner, consultant, client, candidate, and admin accounts.
6. Require `pilot:data:validate` plus a synthetic-data attestation artifact before any screenshot/API evidence is collected.
7. Treat privacy leakage, raw Candidate leakage, tenant-boundary failure, or unlock/disclosure bypass as non-waivable P0 blockers.

## Top 5 Improvement Actions

1. Add `RC1-06B Browser Privacy Negative Proof` with concrete Playwright/API assertions and artifact paths.
2. Expand consent/unlock/disclosure ledger rows to include version, approver, reason, level, and WorkflowEvent IDs.
3. Require re-identification assessment and workflow-event IDs for every client-safe shortlist/card read.
4. Add a five-portal role/route guard matrix to the evidence log template.
5. Add a synthetic-data attestation artifact to RC1-03A and make accidental real PII capture a stop-rule.
