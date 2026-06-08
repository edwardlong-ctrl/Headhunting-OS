# RC1 Pilot Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove controlled-pilot readiness for one complete recruiting transaction from intake through placement, commission, and audit trace, while producing an evidence log and a capability map that dismantles the 160k-line codebase black box.

**Architecture:** RC1 is gate-driven, not feature-driven. Each task runs one bounded release or transaction-readiness gate, records hard evidence, and updates a capability map showing what the system actually does, which code owns it, which data is persisted, and whether AI participates as a claim-producing task rather than a fact owner. The success standard is intentionally high: missing commercial-closure evidence is a failed or incomplete RC1 run, not a successful pre-placement readiness claim.

**Tech Stack:** Java 21, Spring Boot 3.x, PostgreSQL, Flyway, Testcontainers, Maven, React/Vite/Vitest, Playwright, deterministic pilot AI provider, RTK command wrapper.

---

## Human Summary

RC1 has one success exit: `ready-for-controlled-pilot`.

That decision requires current evidence for the full controlled transaction chain: intake, AI claim generation, human review, canonical/truth write, job activation, match report, anonymous shortlist, consent, unlock/disclosure, interview feedback, placement, commission/revenue, and owner/admin audit trace.

Partial evidence is useful but not sufficient:

- If evidence stops at interview feedback or any pre-placement step, the decision is `not-ready-insufficient-evidence` or `not-ready-blocked-product`.
- If Docker, Testcontainers, local PostgreSQL, ports, or browser startup block proof, the decision is `not-ready-blocked-environment`.
- A signed E2E waiver may explain a degraded release-regression run, but it cannot support `ready-for-controlled-pilot`.
- Historical Task 42 or Task 60 acceptance evidence can guide what to inspect, but it cannot replace current RC1 command, runtime, persistence, browser, or audit evidence.

Default execution should use isolated local synthetic data only. Do not use real candidate, client, customer, or production data. Do not mutate a shared local database unless the user has explicitly confirmed that database is disposable for this RC1 run.

## Readiness Term Boundaries

| Term | Meaning | What It Can Claim | What It Cannot Claim |
| --- | --- | --- | --- |
| Local acceptance | Local commands and deterministic evidence passed for this checkout. | "This checkout passed named local gates." | Controlled pilot, customer go-live, public launch, or commercial readiness. |
| RC1 controlled pilot readiness | One deterministic synthetic transaction is proven from intake through placement, commission, and audit trace with no P0 blocker. | `ready-for-controlled-pilot` only for the evaluated commit and run evidence. | Public SaaS launch, managed cloud signoff, live-provider activation, SOC 2/ISO, customer go-live approval, or acquisition readiness. |
| Production go-live readiness | Managed deployment, live provider posture, monitoring, support, rollback, backup/restore, and customer approval are current. | "Ready for a named customer/environment go-live." | This is out of RC1 unless a later plan explicitly adds it. |
| Commercial/acquisition readiness | Pricing, pilots/customers, sales packaging, deployment proof, diligence pack, and buyer narrative are current. | "Commercially packaged for sale or acquisition diligence." | RC1 code evidence alone cannot support this claim. |

## Source Of Truth

- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/specs/v2.0/product-spec-v2.0.md` only when a task touches UI preservation or v2.0 portal baseline behavior
- `docs/release/release-checklist.md`
- `scripts/release/release-gate.sh`
- `scripts/release/run-pilot-e2e.sh`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotacceptance/PilotAcceptanceGate.java`

RC1 must preserve these product invariants:

- v2.1 is the current product source of truth.
- v2.0 UI and portal definitions must not be deleted, compressed, or replaced.
- Consultant is one unified portal.
- AI outputs claims, not facts.
- Backend owns truth.
- PostgreSQL is the target source of truth.
- Every key state transition must create `WorkflowEvent`.
- Client must never read raw `Candidate` objects before unlock/disclosure.

## Spec-To-RC1 Traceability

RC1-10 and RC1-11 must reconcile every row before any final decision. A row may end as `passed`, `partial`, `not-verified`, `blocked-environment`, `blocked-product`, or `out-of-rc1-with-risk-note`; only rows with the required minimum evidence can support `ready-for-controlled-pilot`.

| v2.1 Requirement | RC1 Relevance | Task / Gate | Minimum Evidence | Final Evidence Row | Out-Of-Scope Note |
| --- | --- | --- | --- | --- | --- |
| v2.1 source of truth and v2.0 UI/portal preservation | Required | RC1-00, RC1-04, RC1-10, RC1-11 | E1 route contract plus route/source mapping; E4/E5 for used pilot routes | Gate Evidence `Frontend regression`; P0 Proof `v2.0/v2.1 portal preservation` | None |
| Five portals and unified consultant portal | Required for pilot surfaces | RC1-04, RC1-07, RC1-09 | E1 route/render checks plus E4/E5 exercised route evidence | Golden Path rows; Pilot Role/Route Guard Matrix | Full public portal polish is out of RC1. |
| AI outputs claims, not facts | Required | RC1-05, RC1-06, RC1-09, RC1-10 | E1/E2 tests plus E3 runtime or persisted `AITaskRun`/`ClaimLedger` evidence when exercised | P0 Proof `AI cannot directly own canonical facts` | Live model activation is out of RC1. |
| Claim Ledger field-level governance | Required when AI intake or claim review is exercised | RC1-05, RC1-06, RC1-09, RC1-10 | E1/E3 evidence that exercised claim rows include `source_span`, `assertion_strength`, `speaker`, `verification_status`, `canonical_write_allowed`, and `client_shareability` | Claim Ledger v2.1 Evidence; P0 Proof `Claim Ledger fields before canonical write` | If no AI claim path is exercised, final memo must call this not verified. |
| Backend owns truth; PostgreSQL is source of truth | Required | RC1-02, RC1-03, RC1-03A, RC1-05, RC1-09 | E2 migration/persistence plus E3 runtime evidence for exercised writes | Gate Evidence `Migration validation`; Golden Path persistence evidence | Managed cloud database signoff is out of RC1. |
| Every key transition creates `WorkflowEvent` | Required | RC1-03, RC1-05, RC1-08A, RC1-09, RC1-10 | E2/E3 plus WorkflowEvent/audit row tied to transaction ledger IDs | Transaction Trace Ledger; Golden Path workflow/audit evidence | Full audit export certification is out of RC1 unless RC1-08B proves it. |
| Risk-tiered review and anti-false-confirmation | Required for trust boundary | RC1-06, RC1-09, RC1-10 | E1 tests or E3/E5 review evidence plus persisted review/audit signal when exercised | P0 Proof `Risk-tiered review / bulk approve downgrade` | Full review-quality analytics beyond the pilot transaction may be partial. |
| MatchReport v2.1 governance | Required because MatchReport is in the golden path | RC1-06, RC1-07, RC1-09, RC1-10 | E1/E3/E4 evidence for evidence coverage, provenance weighting, confidence, authenticity risk, score caps, and ontology version/stale warning | P0 Proof `MatchReport v2.1 governance` | If ontology maturity is not implemented, classify as partial with controlled-pilot risk. |
| Client-safe shortlist and re-identification risk | Required | RC1-06, RC1-06B, RC1-07, RC1-09 | E1 privacy tests plus E3/E4 browser/API negative proof and redaction assessment/WorkflowEvent IDs | P0 Proof `Client-safe re-identification gate` | None |
| Consent, unlock, disclosure, and raw Candidate block | Required | RC1-06, RC1-06B, RC1-07, RC1-09 | E1 plus E3/E4; disclosure requires persistence/audit evidence | Consent/Disclosure Evidence; P0 Proof rows | None |
| Prior contact/application and transaction protection | Required if exercised in pilot data | RC1-06, RC1-09, RC1-10 | E1/E3 evidence or explicit not-exercised note | P0 Proof `Prior contact/application protection` | If pilot data omits it, final memo must call it not verified. |
| Placement, commission, revenue/accounting handoff | Required for RC1 success | RC1-08A, RC1-09, RC1-10, RC1-11 | E3 runtime/API plus persistence or WorkflowEvent/audit evidence | Commercial Closure Evidence; Golden Path rows | None |
| Owner commercial proof and Admin governance/audit/replay proof | Required for final traceability | RC1-08A, RC1-08B, RC1-09, RC1-10, RC1-11 | E3/E5 owner/admin or consultant workflow evidence tied to same ledger IDs | Owner Commercial Proof; Admin Governance Proof | Full external audit certification is out of RC1. |
| Operations readiness for controlled local pilot | Required as RC1 guardrail | RC1-08B, RC1-11 | Current command/runbook evidence or explicit not-current classification | Gate Evidence `Operations readiness` | Public production ops signoff is out of RC1. |

## RC1 Scope

### Included

- Controlled pilot readiness for one complete recruiting transaction.
- Local gate execution and evidence capture.
- Docker/Testcontainers, migration, backend, frontend, privacy/security, AI eval, browser E2E, and release gate verification.
- Golden path runtime verification through placement, commission, and audit trace.
- Capability map creation while gates are executed.
- Minimal blocker fixes only after a failing gate has exact evidence.

### Excluded

- Public SaaS launch.
- Managed cloud signoff.
- Formal SOC 2, ISO, or external certification.
- Customer go-live approval.
- Production email/SMS delivery.
- Live external AI provider activation.
- Large new features, large refactors, or converting the system into AI agents.
- Pricing, billing, sales packaging, or acquisition positioning.

## Golden Path

RC1 proves this transaction chain:

1. Consultant creates or imports candidate intake material.
2. AI produces draft claims.
3. Human review confirms or rejects claims through governed boundaries.
4. Approved claims reach canonical/truth-layer state without AI owning fact writes.
5. Client or consultant creates job intake.
6. Job is clarified and activated.
7. Match report is generated with evidence-backed score and confidence boundaries.
8. Consultant builds anonymous client-safe shortlist.
9. Candidate receives opportunity and consent request.
10. Candidate grants consent.
11. Client reviews anonymous shortlist and requests unlock.
12. Consultant approves disclosure after prerequisite gates.
13. Client sees disclosed profile only after unlock/disclosure.
14. Client submits interview feedback.
15. Placement is recorded.
16. Commission/revenue/accounting handoff surface is visible.
17. Admin/owner can trace the workflow, governance, and audit chain.

## RC1 Decision Ladder

Use this ladder to avoid binary thinking during execution:

| Level | Name | Meaning | Allowed Claim |
| --- | --- | --- | --- |
| L0 | Plan ready | Plan, evidence log, capability map templates exist. | "RC1 execution is prepared." |
| L1 | Environment ready | Docker/Testcontainers and isolated local data source are available. | "Local RC1 gates can run." |
| L2 | Regression gates ready | Frontend, backend, migrations, privacy/security, and AI eval gates pass. | "Current checkout passes local release regressions." |
| L3 | Automated pilot flow ready | `release:e2e:pilot` passes through deterministic browser flows. | "Pre-placement automated pilot workflow is repeatable, but RC1 is not ready yet." |
| L4 | Commercial closure evidenced | Placement and commission have runtime/API plus persistence or audit evidence. | "The transaction can reach commercial closure in controlled pilot conditions." |
| L5 | Operator-ready controlled pilot | Manual golden path is understandable and has no P0 blocker. | "Ready for controlled pilot." |

RC1 cannot be called `ready-for-controlled-pilot` below L5. A pre-placement result is not a downgraded success state; it is `not-ready-insufficient-evidence` unless the blocker is specifically product or environment related. If execution stops below L5, the decision must name the highest proven level and the first missing blocker.

## Evidence Rules

- No gate is passed unless the exact command exits `0`.
- No runtime behavior is considered verified unless there is command output, browser E2E output, API/server output, screenshot, log, or persisted-row evidence.
- For failing tests, release gates, migrations, Docker/Testcontainers, and E2E, rerun or inspect with `rtk proxy zsh -lc '...'` when exact failure output matters. Compact `rtk` output is acceptable for first-pass success checks, but not enough for root-cause evidence after a failure.
- Docker/Testcontainers failure is classified as an environment blocker until logs prove a product regression.
- A command that validates only migration filenames does not prove PostgreSQL migration application.
- `release:gate` readiness requires the command to print `RELEASE_READY`.
- Do not set `RTO_RELEASE_SKIP_BROWSER_E2E=1` unless the user has explicitly approved a signed risk-acceptance artifact for this run.
- If a task fails, stop that task, record the blocker, and create a minimal follow-up fix task. Do not continue pretending the gate passed.
- Do not use real candidate, client, or production data during RC1 unless the user explicitly approves it for a named task. Default to deterministic synthetic pilot data.
- Do not print secrets or environment variable values. If a task must check whether a variable exists, record only the variable name and whether it is present.
- For P0 transaction steps, RC1 evidence must include at least runtime/API behavior plus persistence or workflow/audit evidence. Screenshots alone are not sufficient for canonical write, consent, unlock/disclosure, placement, commission, or governance/audit claims.
- Missing placement, commission, or audit-trace evidence cannot be waived into pilot readiness. Classify it as `not-ready-insufficient-evidence` when no current proof exists, or `not-ready-blocked-product` when current proof shows a product failure.

## Evidence Strength Model

When updating `RC1-pilot-readiness-evidence.md`, tag each non-trivial claim with the strongest evidence level available:

| Level | Evidence Type | Examples | Can Support |
| --- | --- | --- | --- |
| E0 | Source/doc only | Spec, source file, static route, static service class | Mapping, not readiness |
| E1 | Unit/contract test | Vitest, JUnit unit test, controller contract test | Component behavior |
| E2 | Integration/persistence test | Testcontainers, Flyway apply, JDBC integration test | DB-backed behavior |
| E3 | Runtime/API evidence | Running API response, server log, health check, focused command output | Runtime behavior |
| E4 | Browser E2E evidence | Playwright deterministic pilot flow | User-flow automation |
| E5 | Manual operator evidence | Human walkthrough with screenshots, IDs, DB/audit rows, confusion notes | Controlled pilot operator readiness |

Required minimums:

- Migration validation: E2.
- Backend/frontend regression: E1 or stronger.
- Client-safe/unlock/disclosure privacy: E1 plus E3/E4 for RC1 readiness.
- Golden path P0 steps: E3 plus persistence/audit evidence; E4/E5 where UI operation is part of the claim.
- Placement/commission closure: E3 plus persistence or audit evidence. E1 alone is only `partial`.

## Standard Evidence Row Schema

Every gate row must use the same evidence shape. Do not mark a row `passed` unless all required fields are populated.

| Field | Required Meaning |
| --- | --- |
| Gate | Stable gate name from this plan. |
| Owner | Accountable role for the gate result. |
| Command / Action | Exact command, browser action, API action, or manual walkthrough action. |
| Run At | Timestamp for the current RC1 run. |
| Exit Code / Result | Exit code for commands, HTTP/status result for API/browser/manual checks. |
| Status | One value from Status Vocabulary. |
| Evidence Level | Strongest supported E-level from the Evidence Strength Model. |
| Evidence Artifact | File path, screenshot path, log excerpt path, or concise inline evidence. |
| Transaction IDs | Ledger IDs/refs used to correlate the claim, or `not-applicable`. |
| Blocker | Empty only when passed; otherwise exact blocker classification and excerpt. |
| Rollback / Cleanup | Cleanup or rollback path, especially for DB, server, browser, or evidence artifacts. |
| Next Action | The smallest next step, not a broad project direction. |

For canonical write, consent, unlock/disclosure, placement, commission, owner commercial proof, admin governance proof, and audit trace, an API response or screenshot is not enough by itself. The row must include persistence evidence or `WorkflowEvent`/audit evidence tied to the same transaction IDs.

## Artifact Responsibilities

- `docs/release/RC1-pilot-readiness-plan.md`: This plan and task queue.
- `docs/release/RC1-pilot-readiness-evidence.md`: Run-by-run hard evidence, command results, blockers, and decision log.
- `docs/release/RC1-capability-map.md`: Capability map for the transaction system and AI-agent-readiness notes.
- `docs/release/rc1-artifacts/`: Optional screenshots, terminal excerpts, API logs, DB row exports, and browser E2E evidence snippets.

## Status Vocabulary

- `passed`: Exact command or runtime check exited successfully and evidence is recorded.
- `blocked-environment`: External local environment blocked the check, such as Docker/Testcontainers unavailable.
- `blocked-product`: Product code, schema, test, or runtime behavior failed.
- `blocked-dependency`: Third-party dependency, package audit, vulnerability, NVD/cache, or external tool prerequisite blocked the check.
- `partial`: Some prerequisite evidence passed, but the gate did not fully prove the requirement.
- `runtime-gap-recorded`: A source/test map exists, but current runtime/API plus persistence/audit proof is missing; this is not a pass and cannot support `ready-for-controlled-pilot`.
- `waived-with-risk-acceptance`: A gate was not run and a signed risk-acceptance artifact explains why. This is not equivalent to `passed` and cannot support `ready-for-controlled-pilot`.
- `not-verified`: No current evidence was collected in this RC1 run.
- `scaffold`: Code or docs exist, but no current runtime or test evidence proves the behavior.
- `no-op`: A provider or subsystem intentionally records or simulates behavior without performing the real external action.
- `out-of-rc1-with-risk-note`: Requirement is outside this RC1 run and has an explicit final-memo boundary; this does not support `ready-for-controlled-pilot` unless the requirement is nonessential for RC1.

## Artifact Retention And Privacy Policy

RC1 artifacts may include command excerpts, API summaries, screenshots, log snippets, and database row evidence. They must remain safe to review and commit only after explicit user approval.

- Use deterministic synthetic pilot data only.
- Do not store secrets, passwords, tokens, full environment dumps, private keys, real candidate/client data, or raw PII in evidence files.
- Database evidence should record only IDs/refs, statuses, timestamps, safe summary fields, and WorkflowEvent/action codes needed to prove the claim.
- Screenshots must come from synthetic pilot accounts and should avoid raw identity/contact fields unless the step being proven is post-disclosure identity access.
- Failure artifacts should contain the shortest exact excerpt needed to classify the blocker; do not paste whole logs when a focused excerpt proves the issue.
- If an artifact accidentally captures sensitive data, stop, do not commit it, remove the sensitive artifact, and rerun with a safe evidence capture method.
- RC1-03A must create or update a synthetic-data attestation artifact before screenshots/API excerpts are collected. The attestation records dataset source, validator command result, no-real-data statement, operator, and screenshot/API redaction policy. It must not include secrets or raw connection strings.

## Subagent Execution Contract

Execute tasks sequentially. Do not dispatch multiple implementation subagents in parallel because each task may update the same evidence and capability documents.

Each implementer subagent must return this shape:

```text
Status: DONE | DONE_WITH_CONCERNS | NEEDS_CONTEXT | BLOCKED
Task: RC1-XX
Files changed:
Commands run:
Evidence rows updated:
Capability rows updated:
Owner:
Approver / signoff role:
Rollback / cleanup performed:
Waiver status:
Blockers:
Next recommended task:
```

After each task, run two reviews before moving on:

1. Spec compliance review: did the task update exactly the required rows/artifacts and avoid overclaiming?
2. Evidence quality review: do command outputs, logs, screenshots, or DB/audit evidence actually support the recorded status?

Do not commit unless the user explicitly asks for commits. Keep RC1 artifacts as working-tree evidence until the user asks for a commit or PR.

## Gate Metadata Matrix

| Task | Owner | Approver / Signoff | Deliverable | Rollback / Cleanup | Waiver Allowed | Waiver Authority | Exit Gate |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RC1-00 Evidence templates | Release owner | Product owner | Evidence log, capability map, artifact directory | Remove only the created RC1 artifacts if the user asks | No | None | Templates exist and contain no pass claims |
| RC1-01 Checkout freeze | Release owner | Product owner | Commit, branch, dirty-worktree classification | None; read-only evidence | No | None | Dirty worktree classified |
| RC1-02 Docker/Testcontainers | Release owner | Engineering owner | Docker client/server evidence | Stop containers started by the task | No for RC1 success | None | Docker server reachable or `blocked-environment` |
| RC1-03 Migration validation | Backend owner | Engineering owner | Migration filename and PostgreSQL apply evidence | Drop only disposable Testcontainers DB | No for RC1 success | None | `release:migrations` passes with E2 evidence |
| RC1-03A Pilot data contract | Backend owner | Product owner | Synthetic pilot data rebuild/validate and attestation | Reset only approved isolated/disposable datasource | No for RC1 success | Product owner for disposable DB use only | Synthetic data validated and attested |
| RC1-04 Frontend gates | Frontend owner | Product owner | Web test/type/build and route preservation evidence | Remove generated build artifacts only if needed | No for RC1 success | None | Web gates pass |
| RC1-05 Backend gates | Backend owner | Engineering owner | Backend test/build evidence and capability map updates | None unless a disposable DB/container was started | No for RC1 success | None | Backend gates pass |
| RC1-06 Privacy/security and AI eval | Security owner / AI governance owner | Product owner | Negative privacy/security and local AI eval evidence | Remove unsafe artifacts if any are captured | No for privacy leakage, raw Candidate leakage, tenant-boundary failure, or unlock/disclosure bypass | Product owner only for non-required degraded AI eval evidence | Required gates pass |
| RC1-06B Browser privacy negative proof | Security owner | Product owner | Browser/API negative evidence for pre-unlock and disclosure boundaries | Remove unsafe screenshots/logs immediately | No | None | Privacy negative browser/API proof recorded |
| RC1-07 Pilot browser E2E | Release owner | Product owner | Pilot E2E output and covered golden-path rows | Cleanup API process, web process, DB container, ports | Only degraded release-regression; not RC1 success | Product owner with signed risk acceptance | `release:e2e:pilot` passes or blocks final readiness |
| RC1-08A Commercial closure | Backend owner / Frontend owner | Product owner | Placement/commission/runtime/persistence/audit correlation evidence | Revert only disposable pilot transaction data if approved | No for RC1 success | None | Closure evidence passed or runtime gap recorded |
| RC1-08 Full release gate | Release owner | Engineering owner | `release:gate` output with `RELEASE_READY` | Cleanup processes/containers started by release gate | Browser skip only for degraded release-regression, not RC1 success | Product owner with signed risk acceptance | `release:gate` exits 0 and prints `RELEASE_READY` |
| RC1-08B Operations readiness | Release owner / Ops owner | Product owner | Config, provider posture, observability, rollback, backup, monitoring, dependency/security evidence | Remove generated ops artifacts if unsafe | Some ops subchecks may be risk-accepted, but not privacy/security/data loss blockers | Product owner | Ops rows passed or explicitly bounded below pilot readiness |
| RC1-09 Manual golden path | Product owner / Release owner | Product owner | Manual walkthrough evidence tied to transaction ledger | Reset approved pilot data after evidence capture if needed | No for missing P0 transaction proof | None | No P0 operator or transaction blocker |
| RC1-10 Synthesis | Release owner | Product owner | Reconciled evidence log and capability map | None | No | None | No unsupported `implemented` capability |
| RC1-11 Decision | Product owner | Product owner | Final decision and operator memo | None | No required gate may be waiver-only | Product owner | One exact decision value recorded |

## Dependency Graph And Stop Rules

Execute in this order unless a fix task is inserted:

```text
RC1-00 -> RC1-01 -> RC1-02
RC1-02 passed -> RC1-03 -> RC1-03A
RC1-03A passed -> RC1-04 -> RC1-05 -> RC1-06
RC1-04/05/06 passed -> RC1-07
RC1-07 passed -> RC1-06B -> RC1-08A
RC1-08A passed -> RC1-08 -> RC1-08B -> RC1-09
RC1-08A runtime-gap-recorded -> RC1-09-commercial-closure-runtime
RC1-09-commercial-closure-runtime closed -> RC1-08 -> RC1-08B -> RC1-09-operator-review
RC1-08A partial/not-verified -> stop or final decision cannot exceed not-ready-insufficient-evidence
RC1-09 complete -> RC1-10 -> RC1-11
```

`runtime-gap-recorded` is a non-pass state. It must name the missing runtime action, missing persistence/audit proof, owner, next command or manual step, and the maximum allowed final decision until the gap is closed.

`RC1-09-commercial-closure-runtime` and `RC1-09-operator-review` are execution modes inside Task RC1-09, not separate task files. Use them only to make the dependency branch explicit.

Stop immediately when:

- Docker/Testcontainers is unavailable and the next task depends on it.
- A command exits non-zero and exact failure output has not been recorded.
- A task would mutate a non-local or non-isolated database.
- A dirty file outside `docs/release/` must be edited to proceed.
- A P0 invariant is violated.

Allowed to continue with a degraded decision only when:

- The blocker is recorded.
- The next task does not depend on the blocked capability.
- The final decision cannot be `ready-for-controlled-pilot`.

---

## Task Queue

### Task RC1-00: Create Evidence Log And Capability Map Templates

**Files:**
- Create: `docs/release/RC1-pilot-readiness-evidence.md`
- Create: `docs/release/RC1-capability-map.md`
- Create directory: `docs/release/rc1-artifacts/`
- Read: `docs/specs/CURRENT_SPEC.md`
- Read: `docs/release/release-checklist.md`

- [ ] **Step 1: Re-read the governing docs**

Run:

```bash
rtk sed -n '1,120p' docs/specs/CURRENT_SPEC.md
rtk sed -n '1,180p' docs/release/release-checklist.md
```

Expected: both files are readable; the worker records that v2.1, backend truth ownership, PostgreSQL truth source, WorkflowEvent, and client raw Candidate restrictions are the RC1 invariants.

- [ ] **Step 2: Create the artifact directory**

Run:

```bash
rtk mkdir -p docs/release/rc1-artifacts
```

Expected: directory exists.

- [ ] **Step 3: Create `RC1-pilot-readiness-evidence.md`**

Use `apply_patch` to create this exact starting structure:

```markdown
# RC1 Pilot Readiness Evidence

## Run Metadata

| Field | Value |
| --- | --- |
| RC1 run started at | not-recorded |
| RC1 run id | not-recorded |
| Git commit | not-recorded |
| Working tree status | not-recorded |
| Operator | Codex |
| Scope | Controlled pilot transaction readiness |
| Decision | not-decided |

## Transaction Trace Ledger

Record IDs as they are created so later evidence can be correlated instead of treated as isolated screenshots.

| Entity | ID / Ref | Source Step | Evidence Artifact | Correlation Notes |
| --- | --- | --- | --- | --- |
| Organization | not-recorded | pilot data |  |  |
| Consultant account/session | not-recorded | login/session |  |  |
| Client account/session | not-recorded | login/session |  |  |
| Candidate account/session | not-recorded | login/session |  |  |
| Candidate | not-recorded | intake/seed |  |  |
| Candidate profile | not-recorded | intake/canonical |  |  |
| Source document | not-recorded | candidate intake |  |  |
| AI task run | not-recorded | AI draft claims |  |  |
| Claim ledger item | not-recorded | AI draft claims |  |  |
| Canonical write / reviewed fact | not-recorded | human review |  |  |
| Company | not-recorded | job/client intake |  |  |
| Job | not-recorded | job/client intake |  |  |
| Match report | not-recorded | matching |  |  |
| Shortlist | not-recorded | shortlist |  |  |
| Shortlist card | not-recorded | shortlist |  |  |
| Re-identification assessment | not-recorded | client-safe shortlist/card |  |  |
| Consent record | not-recorded | candidate consent |  |  |
| Unlock request | not-recorded | client unlock |  |  |
| Disclosure record | not-recorded | consultant approval |  |  |
| Interview feedback | not-recorded | feedback |  |  |
| Placement | not-recorded | placement |  |  |
| Commission | not-recorded | commission |  |  |
| Owner revenue/accounting evidence | not-recorded | owner commercial proof |  |  |
| Admin governance/audit evidence | not-recorded | admin governance proof |  |  |
| WorkflowEvent trace | not-recorded | audit |  |  |

## Consent / Unlock / Disclosure Evidence

| Step | Record ID | Profile Version | Consent Text Version | Disclosure Level | Human Approver | Reason | WorkflowEvent ID | Evidence Artifact |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Candidate consent | not-recorded | not-recorded | not-recorded | not-applicable | not-recorded | not-recorded | not-recorded |  |
| Client unlock request | not-recorded | not-applicable | not-applicable | not-recorded | not-recorded | not-recorded | not-recorded |  |
| Consultant disclosure approval | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded |  |

## Claim Ledger v2.1 Evidence

| Claim Ref | Source Span Present | Assertion Strength | Speaker | Verification Status | Canonical Write Allowed | Client Shareability | AITaskRun Ref | Review / WorkflowEvent Ref | Evidence Artifact | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded |  | not-verified |

## Pilot Role / Route Guard Matrix

| Portal Role | Route / API | Expected Outcome | Current Result | Evidence Artifact | Status |
| --- | --- | --- | --- | --- | --- |
| Consultant | candidate intake / review / placement / commission | allowed only for pilot consultant account | not-recorded |  | not-verified |
| Client | pre-unlock candidate card | client-safe only; no raw identity or internal governance data | not-recorded |  | not-verified |
| Client | raw candidate/profile UUID route | denied or safe unavailable | not-recorded |  | not-verified |
| Candidate | opportunity consent | allowed only for scoped candidate session | not-recorded |  | not-verified |
| Owner | placements / commission / revenue / accounting | allowed only for owner account and scoped to pilot organization | not-recorded |  | not-verified |
| Admin | audit / governance / workflow trace | allowed only for admin account and scoped to pilot organization | not-recorded |  | not-verified |

## MatchReport v2.1 Governance Evidence

| Requirement | Minimum Evidence | Status | Evidence Artifact | Blocker |
| --- | --- | --- | --- | --- |
| Evidence coverage is visible | E3/E4 when match report is exercised | not-verified |  |  |
| Provenance weighting is applied or explicitly unsupported | E1 plus E3/E4 | not-verified |  |  |
| Score confidence is visible | E3/E4 | not-verified |  |  |
| Authenticity risk is visible and affects score or next action | E1 plus E3/E4 | not-verified |  |  |
| Score caps are enforced for weak evidence/cold ontology/high risk | E1 plus E3/E4 | not-verified |  |  |
| Ontology / industry pack version and stale warning are visible or risk-accepted | E0 classification plus E3/E4 if implemented | not-verified |  |  |

## Browser Privacy Negative Evidence

| Check | Expected Result | Current Result | Evidence Artifact | Status |
| --- | --- | --- | --- | --- |
| Pre-unlock client page/API excludes name, email, phone, LinkedIn, exact employer, exact project/product/chip, raw source text, consultant notes, candidate/profile UUIDs, `WorkflowEvent`, and `ClaimLedger` | no forbidden strings present | not-recorded |  | not-verified |
| Raw candidate/profile UUID route attempt | safe denial or unavailable response | not-recorded |  | not-verified |
| Cross-organization anonymous-card reference | safe unavailable response, no data leakage | not-recorded |  | not-verified |
| Post-disclosure identity access | requires consent, unlock, consultant approval, and audit evidence | not-recorded |  | not-verified |

## Operations Readiness Evidence

| Check | Command / Action | Status | Evidence Artifact | Risk Note |
| --- | --- | --- | --- | --- |
| Deployment config validation | not-recorded | not-verified |  |  |
| Secrets/provider presence without values | not-recorded | not-verified |  |  |
| External provider posture | deterministic-only / live-configured / manual-channel-approved / out-of-scope | not-verified |  |  |
| Observability incident dry run | not-recorded | not-verified |  |  |
| Backup/restore or explicit not-current classification | not-recorded | not-verified |  |  |
| Rollback target | not-recorded | not-verified |  |  |
| First-week monitoring owner/cadence | not-recorded | not-verified |  |  |
| Dependency/security scan | `rtk npm audit --omit=dev` and dependency-check or bounded risk acceptance | not-verified |  |  |

## Scope Freeze

### Included

- Controlled pilot transaction chain from intake to commission.
- Local release gates and runtime smoke evidence.
- Capability mapping while testing.

### Excluded

- Public SaaS launch.
- Managed cloud signoff.
- Formal certification.
- Customer go-live approval.
- Production provider activation.
- Large new features or agent refactor.

## Spec-To-RC1 Traceability Status

| v2.1 Requirement | RC1 Task / Gate | Status | Evidence Row / Artifact | Risk Note |
| --- | --- | --- | --- | --- |
| v2.1 source of truth and v2.0 UI/portal preservation | RC1-00 / RC1-04 / RC1-10 | not-verified |  |  |
| Five portals and unified consultant portal | RC1-04 / RC1-07 / RC1-09 | not-verified |  |  |
| AI outputs claims, not facts | RC1-05 / RC1-06 / RC1-09 / RC1-10 | not-verified |  |  |
| Claim Ledger field-level governance | RC1-05 / RC1-06 / RC1-09 / RC1-10 | not-verified |  |  |
| Backend owns truth and PostgreSQL is source of truth | RC1-02 / RC1-03 / RC1-03A / RC1-05 / RC1-09 | not-verified |  |  |
| Every key transition creates WorkflowEvent | RC1-03 / RC1-05 / RC1-08A / RC1-09 / RC1-10 | not-verified |  |  |
| Risk-tiered review and anti-false-confirmation | RC1-06 / RC1-09 / RC1-10 | not-verified |  |  |
| MatchReport v2.1 governance | RC1-06 / RC1-07 / RC1-09 / RC1-10 | not-verified |  |  |
| Client-safe shortlist and re-identification risk | RC1-06 / RC1-06B / RC1-07 / RC1-09 | not-verified |  |  |
| Consent, unlock, disclosure, and raw Candidate block | RC1-06 / RC1-06B / RC1-07 / RC1-09 | not-verified |  |  |
| Prior contact/application and transaction protection | RC1-06 / RC1-09 / RC1-10 | not-verified |  |  |
| Placement, commission, revenue/accounting handoff | RC1-08A / RC1-09 / RC1-10 / RC1-11 | not-verified |  |  |
| Owner commercial proof and Admin governance/audit/replay proof | RC1-08A / RC1-08B / RC1-09 / RC1-10 / RC1-11 | not-verified |  |  |
| Operations readiness for controlled local pilot | RC1-08B / RC1-11 | not-verified |  |  |

## Gate Evidence

| Gate | Owner | Command / Action | Run At | Exit Code / Result | Status | Evidence Level | Evidence Artifact | Transaction IDs | Blocker | Rollback / Cleanup | Next Action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Working tree freeze | Release owner | `rtk git status --short` | not-recorded | not-recorded | not-verified | E0 |  | not-applicable |  | none |  |
| Docker/Testcontainers | Release owner | `rtk docker info` | not-recorded | not-recorded | not-verified | E0 |  | not-applicable |  | none |  |
| Migration validation | Backend owner | `rtk npm run release:migrations` | not-recorded | not-recorded | not-verified | E2 |  | not-applicable |  | Testcontainers cleanup |  |
| Pilot data contract | Backend owner | `RC1-03A rebuild + validate` | not-recorded | not-recorded | not-verified | E2 |  | pilot org / dataset refs |  | reset isolated datasource if approved |  |
| Frontend regression | Frontend owner | `rtk npm --workspace @rto/web run test` | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | none |  |
| Frontend route preservation | Frontend owner | `portalRouteContract.test.ts` within web test | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | none |  |
| Frontend typecheck | Frontend owner | `rtk npm run typecheck:web` | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | none |  |
| Frontend build | Frontend owner | `rtk npm run build:web` | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | build artifact cleanup if needed |  |
| Backend regression | Backend owner | `rtk npm run test:core-api` | not-recorded | not-recorded | not-verified | E1/E2 |  | not-applicable |  | test containers cleanup |  |
| Backend build | Backend owner | `rtk npm run build:core-api` | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | target cleanup if needed |  |
| Privacy/security negative | Security owner | `rtk npm run release:privacy-security` | not-recorded | not-recorded | not-verified | E1/E2 |  | relevant transaction IDs if exercised |  | test containers cleanup |  |
| Browser privacy negative | Security owner | RC1-06B browser/API checks | not-recorded | not-recorded | not-verified | E3/E4 |  | candidate/profile/shortlist/disclosure refs |  | remove unsafe screenshots/logs |  |
| AI eval regression | AI governance owner | `rtk npm run release:ai-eval` | not-recorded | not-recorded | not-verified | E1 |  | AI task keys |  | none |  |
| Pilot browser E2E | Release owner | `rtk npm run release:e2e:pilot` | not-recorded | not-recorded | not-verified | E4 |  | pilot transaction refs |  | API/web/DB process cleanup |  |
| Commercial closure | Backend / frontend owners | placement and commission runtime/API plus persistence/audit evidence | not-recorded | not-recorded | not-verified | E3 plus persistence/audit |  | placement/commission/WorkflowEvent refs |  | reset pilot data if approved |  |
| Full release gate | Release owner | `rtk npm run release:gate` | not-recorded | not-recorded | not-verified | E4 |  | not-applicable |  | release gate cleanup |  |
| Operations readiness | Ops / release owners | RC1-08B ops checklist | not-recorded | not-recorded | not-verified | E0-E3 |  | not-applicable |  | remove unsafe artifacts |  |

## Golden Path Evidence

| Step | Status | Evidence Level | User Surface | API/Service Evidence | Persistence Evidence | Workflow/Audit Evidence | Blocker | Next Action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Consultant candidate intake | not-verified | E0 |  |  |  |  |  |  |
| AI draft claims | not-verified | E0 |  |  |  |  |  |  |
| Human review | not-verified | E0 |  |  |  |  |  |  |
| Canonical/truth write | not-verified | E0 |  |  |  |  |  |  |
| Client/job intake | not-verified | E0 |  |  |  |  |  |  |
| Job clarification/activation | not-verified | E0 |  |  |  |  |  |  |
| Match report | not-verified | E0 |  |  |  |  |  |  |
| Anonymous shortlist | not-verified | E0 |  |  |  |  |  |  |
| Candidate consent | not-verified | E0 |  |  |  |  |  |  |
| Client unlock request | not-verified | E0 |  |  |  |  |  |  |
| Consultant disclosure approval | not-verified | E0 |  |  |  |  |  |  |
| Disclosed client view | not-verified | E0 |  |  |  |  |  |  |
| Interview feedback | not-verified | E0 |  |  |  |  |  |  |
| Placement | not-verified | E0 |  |  |  |  |  |  |
| Commission/revenue | not-verified | E0 |  |  |  |  |  |  |
| Owner commercial/accounting proof | not-verified | E0 |  |  |  |  |  |  |
| Admin governance/audit/replay proof | not-verified | E0 |  |  |  |  |  |  |

## P0 Proof Matrix

| Requirement | Minimum Evidence | Status | Evidence | Blocker |
| --- | --- | --- | --- | --- |
| AI cannot directly own canonical facts | E1 plus E3/E4 when exercised | not-verified |  |  |
| Claim Ledger fields before canonical write | E1 plus E3 when exercised; source span, assertion strength, speaker, verification status, canonical-write permission, and client shareability are recorded | not-verified |  |  |
| Client cannot read raw Candidate before unlock/disclosure | E1 plus E4 | not-verified |  |  |
| Consent is required before identity disclosure | E1 plus E4 | not-verified |  |  |
| Unlock/disclosure creates auditable state | E3 plus persistence/audit evidence | not-verified |  |  |
| Canonical write creates or preserves truth-layer evidence | E3 plus persistence/audit evidence | not-verified |  |  |
| Risk-tiered review / bulk approve downgrade | E1 plus persisted review/audit signal when exercised | not-verified |  |  |
| MatchReport v2.1 governance | E1 plus E3/E4 for evidence coverage, provenance, confidence, authenticity risk, score caps, ontology version | not-verified |  |  |
| Client-safe re-identification gate | E1 plus E3/E4 and assessment/WorkflowEvent evidence | not-verified |  |  |
| Prior contact/application protection | E1 plus E3 when exercised, or explicit not-exercised note | not-verified |  |  |
| Placement creates auditable commercial state | E3 plus persistence/audit evidence | not-verified |  |  |
| Commission/revenue surface matches placement evidence | E3 plus persistence/audit evidence | not-verified |  |  |
| Owner commercial/accounting proof correlates to placement/commission | E3 plus persistence/audit evidence | not-verified |  |  |
| Admin governance/audit/replay proof correlates to transaction ledger | E3 plus WorkflowEvent/audit evidence | not-verified |  |  |
| Operations readiness has current bounded evidence | E0-E3 depending on subcheck; no privacy/security/data-loss waiver-only blockers | not-verified |  |  |

## Blocker List

| ID | Severity | Type | Evidence | Owner | Minimal Next Action | Status |
| --- | --- | --- | --- | --- | --- | --- |

## RC1 Decision

- Decision: not-decided
- Passed evidence:
- Blocking evidence:
- Not verified:
- Recommended next action:
```

Expected: file exists and uses `not-verified`, `not-recorded`, and `not-applicable` only as explicit starting placeholders until commands are actually run. It must contain no accidental `passed`, `implemented`, or readiness claims.

- [ ] **Step 4: Create `RC1-capability-map.md`**

Use `apply_patch` to create this exact starting structure:

```markdown
# RC1 Capability Map

## Classification

- `implemented`: Source and current verification prove the capability works.
- `partial`: Some behavior exists, but a transaction step or gate is incomplete.
- `scaffold`: Code/docs/routes exist, but no current proof shows the capability works.
- `no-op`: Provider intentionally records or simulates behavior without external action.
- `not-verified`: No current RC1 evidence has been collected.

## Transaction Capability Map

| Capability | Golden Path Step | User Surface | Backend Entry Points | Persistence / Tables | Workflow / Audit | AI Task / Provider | Current Evidence | Status | Next Action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Candidate intake | Consultant candidate intake | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| AI draft claims | AI draft claims | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Claim Ledger governance | AI draft claims / human review / canonical write | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map source span, assertion strength, speaker, verification status, canonical-write permission, client shareability, AITaskRun, and review/audit linkage |
| Human review | Human review | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Canonical/truth write | Canonical/truth write | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Job intake | Client/job intake | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Matching | Match report | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| MatchReport v2.1 governance | Match report | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map score confidence, evidence coverage, provenance weighting, authenticity risk, score caps, and ontology version |
| Shortlist | Anonymous shortlist | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Re-identification risk | Anonymous shortlist / client-safe card | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map assessment, redaction level, allowed/blocked result, and WorkflowEvent |
| Consent | Candidate consent | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Unlock/disclosure | Client unlock request and consultant approval | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Feedback | Interview feedback | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Placement | Placement | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Commission | Commission/revenue | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Owner commercial/accounting proof | Owner revenue / accounting handoff | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Correlate to placement and commission IDs |
| Admin governance/audit/replay proof | Admin governance / audit trace | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Correlate to transaction ledger and WorkflowEvent IDs |
| Governance/audit | End-to-end trace | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |

## AI Agent Readiness Notes

| Candidate Agent | Current Subsystem | Evidence Source | What It Can Do Today | What Is Missing Before Agentization | Status |
| --- | --- | --- | --- | --- | --- |
| Intake agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
| Matching agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
| Consent/disclosure guard agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
| Workflow follow-up agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
| Governance/audit agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
```

Expected: file exists and does not overstate any capability.

- [ ] **Step 5: Record Task RC1-00 result**

Update `docs/release/RC1-pilot-readiness-evidence.md`:

- `RC1 run started at`: current timestamp from `date`
- `Git commit`: output of `rtk git rev-parse HEAD`
- `Working tree status`: summarize `rtk git status --short`
- Add a blocker only if the artifact files could not be created.

Run:

```bash
rtk sed -n '1,240p' docs/release/RC1-pilot-readiness-evidence.md
rtk sed -n '1,220p' docs/release/RC1-capability-map.md
```

Expected: both files render cleanly and contain no accidental pass claims.

### Task RC1-01: Freeze Current Checkout And Dirty Worktree

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Read: `docs/release/RC1-pilot-readiness-plan.md`

- [ ] **Step 1: Capture current git state**

Run:

```bash
rtk git rev-parse --abbrev-ref HEAD
rtk git rev-parse HEAD
rtk git status --short
```

Expected: branch, commit, and dirty files are captured.

- [ ] **Step 2: Classify dirty files**

Rules:

- If dirty files are unrelated to RC1 docs, do not edit or revert them.
- If dirty files are in files a later task must touch, stop and ask the user before modifying them.
- If untracked audit/report artifacts exist, leave them alone unless a later task explicitly writes inside `docs/release/`.

Expected: evidence log records a concise dirty-worktree note.

- [ ] **Step 3: Update evidence log**

Update the `Working tree freeze` row:

- `Status`: `passed` if the state was captured.
- `Evidence`: branch, commit, and dirty file summary.
- `Blocker`: empty unless a dirty file conflicts with a planned edit.
- `Next Action`: `Proceed to Docker/Testcontainers gate`.

Run:

```bash
rtk rg -n "Working tree freeze|Run Metadata" docs/release/RC1-pilot-readiness-evidence.md
```

Expected: the evidence log reflects the exact checkout under evaluation.

### Task RC1-02: Docker And Testcontainers Environment Gate

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Optionally write evidence excerpt: `docs/release/rc1-artifacts/RC1-02-docker.txt`

- [ ] **Step 1: Check Docker client and server**

Run:

```bash
rtk docker version
rtk docker info
```

Expected:

- `docker version` shows client and server sections.
- `docker info` exits `0`.

- [ ] **Step 2: If Docker fails, classify environment blocker**

If either command fails, write the exact failure excerpt to `docs/release/rc1-artifacts/RC1-02-docker.txt` and update the evidence log:

- `Status`: `blocked-environment`
- `Blocker`: exact Docker/Testcontainers availability problem
- `Next Action`: start or repair Docker Desktop, then rerun Task RC1-02

Do not run migration, backend, or E2E gates while Docker is blocked.

- [ ] **Step 3: If Docker passes, update evidence log**

Update the `Docker/Testcontainers` row:

- `Status`: `passed`
- `Evidence`: Docker server reachable and command timestamp
- `Next Action`: `Run migration validation`

Run:

```bash
rtk rg -n "Docker/Testcontainers" docs/release/RC1-pilot-readiness-evidence.md
```

Expected: the row is updated with a current result.

### Task RC1-03: Migration Validation Gate

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optionally write evidence excerpt: `docs/release/rc1-artifacts/RC1-03-release-migrations.txt`
- Read: `scripts/release/validate-migrations.sh`

- [ ] **Step 1: Inspect the migration gate script**

Run:

```bash
rtk sed -n '1,220p' scripts/release/validate-migrations.sh
```

Expected: worker identifies whether the script validates Flyway filenames, Testcontainers-backed PostgreSQL migration application, or both.

- [ ] **Step 2: Run migration gate**

Run:

```bash
rtk npm run release:migrations
```

Expected:

- Exit `0`.
- Evidence shows contiguous Flyway migration validation.
- Evidence shows `TruthLayerPostgresMigrationIntegrationTest` applies migrations to PostgreSQL through Testcontainers.

- [ ] **Step 3: Record result**

If the command exits `0`, update:

- Evidence log `Migration validation` row to `passed`.
- Capability map rows for `Canonical/truth write`, `Consent`, `Unlock/disclosure`, `Placement`, `Commission`, and `Governance/audit` with persistence evidence if migration output or inspected migrations support the mapping.

If the command fails:

- Save exact failure excerpt to `docs/release/rc1-artifacts/RC1-03-release-migrations.txt`.
- Use `blocked-environment` only when the failure is Docker/Testcontainers availability.
- Use `blocked-product` when the failure is migration SQL, Flyway naming, Java compile, or test assertion failure.
- Stop and create a minimal fix task before continuing.

Run:

```bash
rtk rg -n "Migration validation|Canonical/truth write|Consent|Unlock/disclosure|Placement|Commission|Governance/audit" docs/release/RC1-pilot-readiness-evidence.md docs/release/RC1-capability-map.md
```

Expected: result is recorded without conflating filename validation with PostgreSQL application.

### Task RC1-03A: Pilot Data Contract Gate

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optionally write evidence excerpt: `docs/release/rc1-artifacts/RC1-03A-pilot-data.txt`
- Create or update attestation: `docs/release/rc1-artifacts/RC1-03A-synthetic-data-attestation.md`
- Read: `scripts/pilot-data.sh`
- Read: `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotdata/PilotDataCliApplication.java`
- Read: `docs/pilot/task-38-pilot-scenario.md`

This task exists because `release:e2e:pilot` rebuilds and validates deterministic synthetic pilot data before the browser scenarios. Validating the pilot data contract separately makes later E2E failures easier to classify.

- [ ] **Step 1: Inspect pilot data script**

Run:

```bash
rtk sed -n '1,260p' scripts/pilot-data.sh
rtk sed -n '1,110p' services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotdata/PilotDataCliApplication.java
rtk sed -n '1,90p' docs/pilot/task-38-pilot-scenario.md
```

Expected: worker identifies which datasource is used, how rebuild/validate/export/reset behave, and whether the task might mutate a local database. Record that `rebuild` runs migrations and resets/reimports pilot data for the pilot organization.

- [ ] **Step 2: Confirm data-source safety before mutation**

Run:

```bash
rtk proxy zsh -lc 'printf "RTO_PILOT_DATA_JDBC_URL=%s\n" "${RTO_PILOT_DATA_JDBC_URL:+present}"; printf "SPRING_DATASOURCE_URL=%s\n" "${SPRING_DATASOURCE_URL:+present}"; printf "POSTGRES_USER=%s\n" "${POSTGRES_USER:+present}"'
```

Expected:

- Do not print URL, username, password, or secret values.
- If no datasource override is present, record that the CLI would default to `jdbc:postgresql://localhost:5432/recruiting_os`, but do not run rebuild against that default.
- If a datasource override is present, do not run rebuild unless the worker can prove it is a localhost/127.0.0.1 isolated RC1 test database. If unsure, stop with `NEEDS_CONTEXT`.
- The default safe path is to start a throwaway PostgreSQL container for this task, bind it to `127.0.0.1`, and remove it after validation.
- Running `pilot:data:rebuild` against `localhost:5432/recruiting_os` is allowed only when the user explicitly confirms that database is disposable for RC1.
- Do not use `RTO_PILOT_DATA_ALLOW_RESET=true` in RC1-03A.

- [ ] **Step 3: Rebuild and validate synthetic pilot data against an isolated PostgreSQL container**

Run:

```bash
rtk proxy zsh -lc '
set -euo pipefail
port="${RTO_RC1_PILOT_DATA_DB_PORT:-55433}"
container="rto-rc1-pilot-data-$$"
cleanup() {
  docker rm -f "${container}" >/dev/null 2>&1 || true
}
trap cleanup EXIT
if (echo >/dev/tcp/127.0.0.1/"${port}") >/dev/null 2>&1; then
  printf "[RC1-03A] BLOCKED: port %s is already in use.\n" "${port}" >&2
  exit 31
fi
docker run -d \
  --name "${container}" \
  -e POSTGRES_DB=recruiting_os \
  -e POSTGRES_USER=recruiting_os \
  -e POSTGRES_PASSWORD=recruiting_os_local_password \
  -p "127.0.0.1:${port}:5432" \
  postgres:16-alpine >/dev/null
deadline=$((SECONDS + 60))
until docker exec "${container}" pg_isready -U recruiting_os -d recruiting_os >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    printf "[RC1-03A] BLOCKED: isolated PostgreSQL did not become ready.\n" >&2
    docker logs "${container}" >&2 || true
    exit 32
  fi
  sleep 2
done
export RTO_PILOT_DATA_JDBC_URL="jdbc:postgresql://127.0.0.1:${port}/recruiting_os"
export RTO_PILOT_DATA_DB_USER="recruiting_os"
export RTO_PILOT_DATA_DB_PASSWORD="recruiting_os_local_password"
npm run pilot:data:rebuild
npm run pilot:data:validate
'
```

Expected:

- Command exits `0` against the isolated synthetic pilot datasource.
- Output confirms rebuild and validate both passed.
- Output does not expose real data or credentials in the evidence log.
- The temporary database container is removed by the shell trap.
- If it fails, rerun or inspect with `rtk proxy zsh -lc '...'` using the same isolated-container pattern and save the exact blocker.

- [ ] **Step 4: Optional user-approved disposable local database path**

Use this only when the user explicitly confirms that `localhost:5432/recruiting_os` is disposable for RC1.

```bash
rtk npm run pilot:data:rebuild
rtk npm run pilot:data:validate
```

Expected: command exits `0` and confirms the deterministic pilot dataset needed by browser E2E. Record the user's confirmation and the datasource shape, but do not record passwords or full connection strings.

- [ ] **Step 5: Create synthetic-data attestation**

Use `apply_patch` to create `docs/release/rc1-artifacts/RC1-03A-synthetic-data-attestation.md` with this structure:

```markdown
# RC1-03A Synthetic Data Attestation

| Field | Value |
| --- | --- |
| RC1 run id | not-recorded |
| Dataset source | deterministic pilot dataset from `scripts/pilot-data.sh` / pilot data CLI |
| Datasource shape | isolated-container / user-approved-disposable-local |
| Validator command | `npm run pilot:data:validate` |
| Validator result | not-recorded |
| Operator | Codex |
| Statement | No real candidate, client, customer, or production data was intentionally used for this RC1 run. |
| Screenshot/API redaction policy | Do not capture secrets, full connection strings, passwords, raw PII, private keys, or real identity/contact data. Pre-disclosure screenshots must avoid identity/contact fields. |
```

Expected: the attestation exists before RC1-07, RC1-08A, or RC1-09 collects browser/API screenshots or row excerpts.

- [ ] **Step 6: Record result**

Update the `Pilot data contract` row:

- `Status`: `passed`, `blocked-environment`, or `blocked-product`
- `Evidence`: datasource shape, rebuild result, validate result, timestamp, and attestation artifact path
- `Blocker`: exact blocker if any
- `Next Action`: `Proceed to frontend/backend gates` if passed

Update `RC1-capability-map.md` with seed-data dependencies for consultant, client, candidate, owner, and admin surfaces if the script or validation output identifies them.

- [ ] **Step 7: Capture stable pilot account and seed-shape evidence**

Update `Transaction Trace Ledger` with any stable seed identities that are documented or emitted by validation output:

- owner pilot account
- consultant pilot account
- client pilot account
- candidate pilot account
- admin pilot account
- pilot organization

Expected: no passwords or secrets are written to the evidence log.

### Task RC1-04: Frontend Regression, Typecheck, And Build Gates

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optionally write evidence excerpts:
  - `docs/release/rc1-artifacts/RC1-04-web-test.txt`
  - `docs/release/rc1-artifacts/RC1-04-web-typecheck.txt`
  - `docs/release/rc1-artifacts/RC1-04-web-build.txt`
- Read:
  - `apps/web/src/portalRouteContract.test.ts`
  - `apps/web/src/App.tsx`

- [ ] **Step 1: Run frontend tests**

Run:

```bash
rtk npm --workspace @rto/web run test
```

Expected: Vitest exits `0`, including the route preservation contract where present.

- [ ] **Step 2: Confirm v2.0/v2.1 route preservation evidence**

Run:

```bash
rtk rg -n "portalRouteContract|keeps every|v2\\.1|v2\\.0|owner|consultant|client|candidate|admin" apps/web/src/portalRouteContract.test.ts apps/web/src/App.tsx
```

Expected:

- Worker records whether `portalRouteContract.test.ts` exists and is included in the web test run.
- Worker records whether evidence is source-string preservation only or includes render-level route smoke.
- If render-level route smoke is missing, update the capability map and blocker list as a route-usability gap; do not claim render-level route readiness from source-string evidence.

- [ ] **Step 3: Run frontend typecheck**

Run:

```bash
rtk npm run typecheck:web
```

Expected: TypeScript exits `0`.

- [ ] **Step 4: Run frontend build**

Run:

```bash
rtk npm run build:web
```

Expected: Vite build exits `0` and produces the web build artifact.

- [ ] **Step 5: Record frontend evidence**

Update evidence log rows:

- `Frontend regression`
- `Frontend route preservation`
- `Frontend typecheck`
- `Frontend build`

If a command fails, save the exact excerpt in the matching artifact file, classify as `blocked-product`, and stop for a minimal fix task.

- [ ] **Step 6: Update capability map from route and portal evidence**

Read only enough source to map portal entry points:

```bash
rtk sed -n '1,220p' apps/web/src/App.tsx
rtk rg -n "consultant|client|candidate|owner|admin|shortlist|unlock|consent|placement|commission" apps/web/src -g '!**/node_modules/**'
```

Expected: capability map records user surfaces for the golden path without claiming runtime E2E behavior yet. Route presence is E0/E1 preservation evidence unless paired with render-level route smoke or browser E2E evidence.

### Task RC1-05: Backend Regression And Build Gates

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optionally write evidence excerpts:
  - `docs/release/rc1-artifacts/RC1-05-core-api-test.txt`
  - `docs/release/rc1-artifacts/RC1-05-core-api-build.txt`

- [ ] **Step 1: Run backend regression suite**

Run:

```bash
rtk npm run test:core-api
```

Expected: Maven exits `0`.

- [ ] **Step 2: Run backend build gate**

Run:

```bash
rtk npm run build:core-api
```

Expected: Maven `clean verify` exits `0`.

- [ ] **Step 3: Record backend gate evidence**

Update evidence log rows:

- `Backend regression`
- `Backend build`

If a command fails:

- Save exact failure excerpt.
- Classify Docker/Testcontainers-only failures as `blocked-environment`.
- Classify compile/test assertion/domain failures as `blocked-product`.
- If Maven aggregates many errors, identify the first non-Docker/Testcontainers compile/assertion/domain failure before creating a product fix task.
- Stop and create a minimal fix task.

- [ ] **Step 4: Map backend transaction capabilities**

Read bounded backend entry points for golden path mapping:

```bash
rtk rg -l "Consent|Disclosure|Unlock|Placement|Commission|MatchReport|Shortlist|AITask|Claim|Canonical|WorkflowEvent" services/core-api/src/main/java services/core-api/src/test/java
rtk rg -n "class .*Controller|class .*Service|interface .*Port|record .*Command|enum .*Status" services/core-api/src/main/java/com/recruitingtransactionos/coreapi/{consentdisclosure,placement,commission,shortlist,matching,aitaskrunner,truthlayer,workflow,apiboundary} services/core-api/src/test/java/com/recruitingtransactionos/coreapi/{consentdisclosure,placement,commission,shortlist,matching,aitaskrunner,truthlayer,workflow,apiboundary}
```

Expected: update `RC1-capability-map.md` with concrete backend entry points and test evidence for each mapped capability. Use `not-verified` for runtime behavior until RC1-07, RC1-08, or RC1-08A proves it. Keep output bounded; do not paste a repository-wide search dump into the evidence log.

### Task RC1-06: Privacy/Security And AI Eval Gates

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optionally write evidence excerpts:
  - `docs/release/rc1-artifacts/RC1-06-privacy-security.txt`
  - `docs/release/rc1-artifacts/RC1-06-ai-eval.txt`
- Read:
  - `scripts/release/privacy-security-regression.sh`
  - `scripts/release/ai-eval-regression.sh`

- [ ] **Step 1: Inspect privacy/security gate**

Run:

```bash
rtk sed -n '1,220p' scripts/release/privacy-security-regression.sh
```

Expected: worker identifies what negative tests are selected and what privacy invariant each protects.

- [ ] **Step 2: Run privacy/security gate**

Run:

```bash
rtk npm run release:privacy-security
```

Expected: command exits `0`; selected tests cover tenant boundary, client-safe projection, disclosure/unlock, access audit, auth/rate-limit, upload hardening, and pilot privacy regressions.

- [ ] **Step 3: Inspect AI eval gate**

Run:

```bash
rtk sed -n '1,220p' scripts/release/ai-eval-regression.sh
```

Expected: worker identifies eval JSON, prompt, schema, and task-key checks.

- [ ] **Step 4: Run AI eval gate**

Run:

```bash
rtk npm run release:ai-eval
```

Expected: command exits `0`; local eval cases and schemas validate without requiring live model calls.

- [ ] **Step 5: Record governance evidence**

Update:

- Evidence log `Privacy/security negative` row.
- Evidence log `AI eval regression` row.
- Evidence log `Claim Ledger v2.1 Evidence` row when the gate or source inspection exercises an AI claim path.
- Capability map rows for `Consent`, `Unlock/disclosure`, `AI draft claims`, `Claim Ledger governance`, `Canonical/truth write`, and `Governance/audit`.
- AI Agent Readiness Notes for `Consent/disclosure guard agent`, `Intake agent`, and `Governance/audit agent`.

If either command fails:

- Save exact failure excerpt.
- Classify Docker/Testcontainers availability or connection failures as `blocked-environment`.
- Classify selected negative-test assertion, schema, prompt, DTO, access-control, upload, disclosure, unlock, or eval-contract failures as `blocked-product`.
- Stop for a minimal fix task only after the blocker type is clear.

### Task RC1-07: Pilot Browser E2E Gate

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optionally write evidence excerpt: `docs/release/rc1-artifacts/RC1-07-release-e2e-pilot.txt`
- Read:
  - `scripts/release/run-pilot-e2e.sh`
  - `tests/e2e/playwright.config.ts`
  - `tests/e2e/pilot-business-flows.spec.ts`

- [ ] **Step 1: Inspect pilot E2E startup contract**

Run:

```bash
rtk sed -n '1,240p' scripts/release/run-pilot-e2e.sh
rtk sed -n '1,220p' tests/e2e/playwright.config.ts
rtk sed -n '1,260p' tests/e2e/pilot-business-flows.spec.ts
```

Expected: worker records expected startup signals:

- PostgreSQL readiness through `pg_isready` when the script starts its own container.
- Core API log contains startup completion.
- `http://127.0.0.1:${RTO_E2E_API_PORT:-8097}/health` returns HTTP 200.
- Playwright web server serves `http://127.0.0.1:${RTO_E2E_WEB_PORT:-4197}`.
- Deterministic AI provider routes are used.

- [ ] **Step 2: Run pilot browser E2E**

Run:

```bash
rtk npm run release:e2e:pilot
```

Expected:

- Command exits `0`.
- Output shows PostgreSQL readiness.
- Output shows `/health` readiness.
- Output shows Playwright pilot scenarios pass.
- Output shows cleanup of API process and database container.

- [ ] **Step 3: Record E2E evidence**

If the command passes:

- Update evidence log `Pilot browser E2E` row to `passed`.
- Update Golden Path Evidence rows covered by the E2E scenarios.
- Update capability map status for covered capabilities to `implemented` only where the E2E and source evidence prove runtime behavior.

If the command fails:

- Save exact failure excerpt to `docs/release/rc1-artifacts/RC1-07-release-e2e-pilot.txt`.
- Classify startup/Docker/port failures separately from product flow failures.
- Stop and create a minimal fix task.

### Task RC1-06B: Browser Privacy Negative Proof

Execution note: run this after RC1-07 so a deterministic pilot runtime, browser output, or transaction ledger is available. If RC1-07 does not leave enough runtime/browser/API evidence to execute these checks, mark this task `runtime-gap-recorded` and create a minimal fix task. Do not claim this gate passed from backend selected tests alone.

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optionally write evidence artifact: `docs/release/rc1-artifacts/RC1-06B-browser-privacy-negative.md`
- Read:
  - `tests/e2e/pilot-business-flows.spec.ts`
  - `apps/web/src/api/clientSafeCandidateCards.ts`
  - `apps/web/src/api/clientDisclosedCandidates.ts`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ClientSafeCandidateCardController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/AuditedPostgresClientSafeCandidateCardQueryPort.java`

- [ ] **Step 1: Confirm runtime and ledger prerequisites**

Run:

```bash
rtk rg -n "Pilot browser E2E|Transaction Trace Ledger|Shortlist card|Consent record|Unlock request|Disclosure record|Browser Privacy Negative Evidence" docs/release/RC1-pilot-readiness-evidence.md
```

Expected:

- A pilot transaction or anonymous-card reference exists, or the task records `runtime-gap-recorded`.
- If the E2E script cleaned up all runtime processes before privacy checks could run, record that as a runtime gap, not a product failure.

- [ ] **Step 2: Inspect executable privacy-negative surfaces**

Run:

```bash
rtk rg -n "client-safe|candidate-cards|disclosed|consent|unlock|raw Candidate|candidateProfileId|fullName|email|phone|linkedIn|WorkflowEvent|ClaimLedger" tests/e2e apps/web/src/api services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary -g '!**/node_modules/**'
```

Expected: worker identifies whether an existing Playwright/API test already proves pre-unlock privacy negatives. Backend unit/contract tests may support E1, but they do not satisfy this browser/API gate by themselves.

- [ ] **Step 3: Capture browser/API privacy-negative evidence**

Use the active pilot runtime from RC1-07 or a manually started RC1 runtime that follows `scripts/release/run-pilot-e2e.sh`. Record results for these checks:

| Check | Required Result |
| --- | --- |
| Pre-unlock client page/API | Response and rendered page do not contain candidate name, email, phone, LinkedIn, exact employer, exact project/product/chip, raw source text, consultant notes, candidate/profile UUIDs, `WorkflowEvent`, or `ClaimLedger`. |
| Raw candidate/profile UUID route attempt | Returns safe denial or safe unavailable response without raw data, stack trace, package names, or internal object names. |
| Cross-organization anonymous-card reference | Returns safe unavailable response and no data from the other organization. |
| Post-disclosure identity access | Requires consent, unlock, consultant approval, and audit evidence before identity/contact fields are visible. |

If there is no executable browser/API path for these checks, write:

- `Status`: `runtime-gap-recorded`
- `Blocker`: missing executable browser/API privacy-negative proof
- `Next Action`: create `RC1-FIX-browser-privacy-negative-proof` with Playwright/API assertions
- Maximum final decision: `not-ready-insufficient-evidence`

- [ ] **Step 4: Record privacy-negative evidence**

Update:

- Evidence log `Browser privacy negative` row.
- `Browser Privacy Negative Evidence` table.
- `Pilot Role / Route Guard Matrix` rows for client, candidate, owner, admin, and consultant access where exercised.
- P0 Proof rows for raw Candidate block, consent before identity disclosure, client-safe re-identification gate, and unlock/disclosure auditability.
- Capability map rows for `Shortlist`, `Re-identification risk`, `Consent`, `Unlock/disclosure`, and `Governance/audit`.

Expected: this task cannot be `passed` without E3/E4 browser/API evidence plus transaction IDs or explicit not-applicable notes. Any privacy leak, raw Candidate leakage, tenant-boundary failure, or unlock/disclosure bypass is a non-waivable P0 blocker.

### Task RC1-08: Task 58 Full Release Regression Gate

Execution note: run RC1-08A before this task, even though RC1-08 keeps its historical numbering. The full release gate proves the Task 58 release regression suite and `RELEASE_READY` marker. It does not by itself prove placement, commission, owner revenue, accounting handoff, manual operator readiness, or RC1 pilot readiness.

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Optionally write evidence excerpt: `docs/release/rc1-artifacts/RC1-08-release-gate.txt`
- Read: `scripts/release/release-gate.sh`

- [ ] **Step 1: Inspect full release gate script**

Run:

```bash
rtk sed -n '1,220p' scripts/release/release-gate.sh
```

Expected: worker records that the gate runs backend, frontend, migration, privacy/security, AI eval, and browser E2E gates, and only a final `RELEASE_READY` output supports release-gate pass evidence.

- [ ] **Step 2: Run full release gate**

Run:

```bash
rtk npm run release:gate
```

Expected:

- Command exits `0`.
- Output includes `RELEASE_READY`.
- Output does not rely on browser E2E skip.

- [ ] **Step 3: Record release gate evidence**

If the command passes:

- Update evidence log `Full release gate` row to `passed`.
- Add `RELEASE_READY` excerpt and timestamp.
- Record that this is a release-regression result, not a standalone RC1 readiness result.

If the command fails:

- Save exact failure excerpt to `docs/release/rc1-artifacts/RC1-08-release-gate.txt`.
- Mark `blocked-environment` or `blocked-product`.
- Stop and create a minimal fix task.

### Task RC1-08A: Commercial Closure Evidence Gate

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optional artifacts:
  - `docs/release/rc1-artifacts/RC1-08A-commercial-closure.txt`
  - `docs/release/rc1-artifacts/RC1-08A-commercial-closure-screenshots.md`
- Read:
  - `tests/e2e/pilot-business-flows.spec.ts`
  - `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/placement/service/PlacementWorkflowServiceTest.java`
  - `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantPlacementQueryServiceTest.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantPlacementController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantCommissionController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/admin/AdminObservabilityController.java`

This task closes a coverage gap: current Task 42 pilot browser flows prove S01-S08 through interview feedback, but do not by themselves prove placement, commission, owner revenue, or accounting handoff runtime behavior. RC1 claims `ready-for-controlled-pilot` only if commercial closure is proven with current runtime/API plus persistence or workflow/audit evidence. Service tests or source mapping are useful, but they do not by themselves satisfy RC1.

- [ ] **Step 1: Confirm E2E coverage boundary**

Run:

```bash
rtk rg -n "S0|placement|commission|feedback|unlock|consent|shortlist" tests/e2e/pilot-business-flows.spec.ts
```

Expected: worker records whether current browser E2E covers placement/commission. If it does not, record that as an evidence gap, not a product failure.

- [ ] **Step 2: Map placement and commission implementation evidence**

Run:

```bash
rtk rg -l "PlacementWorkflowService|CommissionWorkflowService|ConsultantPlacement|ConsultantCommission|Owner.*Placement|Owner.*Commission|accounting|ReportingExport" services/core-api/src/main/java services/core-api/src/test/java apps/web/src -g '!**/node_modules/**'
rtk rg -n "class .*Placement|class .*Commission|interface .*Placement|interface .*Commission|record .*Placement|record .*Commission|enum .*Placement|enum .*Commission|ReportingExport" services/core-api/src/main/java/com/recruitingtransactionos/coreapi/{placement,commission,reportingexport,governancequery,apiboundary} services/core-api/src/test/java/com/recruitingtransactionos/coreapi/{placement,commission,reportingexport,governancequery,apiboundary} apps/web/src/api apps/web/src/features/{consultant-portal,owner-portal}
```

Expected: worker identifies user surfaces, API/service entry points, tests, and persistence surfaces for placement, commission, owner revenue, and read-only accounting export handoff. Keep raw search output out of the evidence log; summarize the mapped files instead.

- [ ] **Step 3: Build the commercial-closure correlation recipe**

Record the exact runtime/API and persistence/audit evidence needed to correlate one transaction:

| Evidence Item | Required Correlation |
| --- | --- |
| `POST /api/consultant/placements` or equivalent UI action | placement ID, job ID, candidate/profile ID, company/client ID |
| Placement status transition, such as `/offer-accepted`, `/onboarded`, `/invoice-ready`, `/invoice-sent`, or `/payment-paid` | same placement ID and WorkflowEvent/action evidence |
| `POST /api/consultant/commissions` or equivalent UI action | commission ID tied to placement ID |
| Commission transition, such as `/mark-paid` or `/withhold` | same commission ID and WorkflowEvent/action evidence |
| `GET /api/owner/revenue` | owner-visible revenue row tied to placement/commission or the same pilot organization |
| `GET /api/owner/revenue/accounting-export` | read-only accounting handoff references the same placement/commission where supported |
| `GET /api/admin/observability/workflow-events` | WorkflowEvent rows for placement and commission entity IDs |
| persisted rows or safe DB excerpts | placement and commission rows for the same IDs, with no secrets or raw PII |

Expected: if the current app cannot execute this recipe, mark `Commercial closure` as `runtime-gap-recorded`, not `passed`.

- [ ] **Step 4: Verify placement/commission through the strongest available current gate**

Use the strongest non-invasive evidence available in this order:

1. If browser/runtime flow already supports placement and commission, run that flow and capture screenshot/API/log evidence.
2. If runtime flow is not available, run focused backend tests that prove placement and commission workflow behavior.
3. If only source or service-test evidence exists, mark `partial`, `runtime-gap-recorded`, or `not-verified`, not `passed`.

Focused test candidates:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=PlacementWorkflowServiceTest,ConsultantPlacementQueryServiceTest,CommissionWorkflowServiceTest,ReportingExportServicePolicyTest test
```

Expected: command exits `0` if using test evidence. This is not enough for full runtime proof or RC1 success, but it can support a `partial` or service-level classification before manual/runtime evidence is collected.

- [ ] **Step 5: Record commercial closure status**

Update:

- Evidence log `Commercial closure` row.
- Golden Path Evidence rows for `Placement` and `Commission/revenue`.
- Golden Path Evidence rows for `Owner commercial/accounting proof` and `Admin governance/audit/replay proof` if exercised.
- Capability map rows for `Placement`, `Commission`, `Owner commercial/accounting proof`, `Admin governance/audit/replay proof`, and `Governance/audit`.

Use:

- `passed` only if current runtime/API plus persistence/audit evidence proves placement and commission behavior.
- `runtime-gap-recorded` when implementation/test/source evidence exists but runtime/API plus persistence/audit correlation is missing.
- `partial` if only service tests or source evidence prove component behavior. `partial` is not a successful RC1 state.
- `not-verified` if no current evidence was collected.

### Task RC1-08B: Operations Readiness Gate

This gate does not turn RC1 into production go-live. It only proves that the controlled local pilot has current operational guardrails or clearly bounded nonclaims. Any privacy leakage, raw Candidate leakage, data-loss risk, tenant-boundary failure, or broken disclosure/unlock path is non-waivable.

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Optionally write evidence artifact: `docs/release/rc1-artifacts/RC1-08B-operations-readiness.md`
- Read:
  - `infra/deployment/production-like.env.example`
  - `infra/deployment/backup-restore-runbook.md`
  - `infra/deployment/rollback-runbook.md`
  - `infra/observability/README.md`
  - `docs/onboarding/go-live-checklist.md`
  - `docs/onboarding/risk-review-guide.md`
  - `docs/security/task-52-production-security-compliance-baseline.md`
  - `scripts/security/dependency-check-core-api.sh`

- [ ] **Step 1: Inspect operations source documents**

Run:

```bash
rtk rg -n "DeploymentEnvironmentValidator|backup|restore|rollback|incident|observability|go-live|risk|first-week|dependency-check|npm audit|provider|secret" infra docs services/core-api/src/main/java services/core-api/src/test/java scripts/security package.json -g '!**/node_modules/**'
```

Expected: worker records which existing docs/tests/scripts can support RC1 operations evidence, and which are historical or out of RC1 scope.

- [ ] **Step 2: Run deterministic deployment/security policy checks**

Run:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=DeploymentArtifactsContractTest,DeploymentEnvironmentValidatorTest,DeploymentEnvironmentConfigurationTest,SecurityComplianceBaselineDocumentationTest test
rtk npm audit --omit=dev
```

Expected:

- Maven focused tests exit `0`, or exact failure is recorded.
- `npm audit --omit=dev` exits `0`, or exact advisories/blocker classification are recorded.
- No secret values are printed into the evidence log.

- [ ] **Step 3: Run or classify dependency-check**

Run:

```bash
rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check
```

Expected:

- If dependency-check exits `0`, record report paths and timestamp.
- If NVD cache/API-key prerequisites block the run, record `blocked-environment` or bounded risk acceptance. Do not call it passed.
- If vulnerabilities fail the gate, record `blocked-product` or `blocked-dependency` in the blocker list.

- [ ] **Step 4: Record provider, backup, rollback, incident, and monitoring posture**

Update `Operations Readiness Evidence` rows:

- Deployment config validation.
- Secrets/provider presence check: record variable names and present/missing only, never values.
- External provider posture: `deterministic-only`, `live-configured`, `manual-channel-approved`, or `out-of-scope`.
- Observability incident dry run: request/correlation/audit search path or `not-current`.
- Backup/restore: current proof, historical-context-only, or `not-current`.
- Rollback target: exact runbook and target environment/version.
- First-week monitoring owner/cadence.
- Dependency/security scan result.

Expected: operations readiness can be `passed` only if every required row has current evidence or a bounded nonclaim that does not affect RC1 success. If backup/restore, rollback, observability, or dependency scan are not current, the final memo must say exactly what RC1 cannot claim.

### Task RC1-09: Manual Golden Path Runtime Verification

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Modify: `docs/release/RC1-capability-map.md`
- Optional artifacts:
  - `docs/release/rc1-artifacts/RC1-09-manual-golden-path-notes.md`
  - screenshots under `docs/release/rc1-artifacts/`

This task is allowed only after Task RC1-07 passes and Task RC1-08A is either `passed` or `runtime-gap-recorded`, or after the user explicitly requests manual diagnosis of a failed E2E/commercial path. If RC1-08A is `partial`, `runtime-gap-recorded`, or `not-verified`, RC1-09 must close the placement/commission runtime plus persistence/audit gap before the final decision can be `ready-for-controlled-pilot`.

- [ ] **Step 1: Start from the pilot E2E runtime contract**

Use `scripts/release/run-pilot-e2e.sh` as the reference for ports, environment variables, deterministic AI provider, PostgreSQL setup, and health checks. Do not use production data or live provider credentials.

- [ ] **Step 2: Run the transaction manually**

Operate the app as a user through the five portal surfaces where available:

1. Consultant candidate intake.
2. AI draft claim review.
3. Canonical/truth write confirmation.
4. Client/job intake.
5. Match report.
6. Shortlist creation.
7. Candidate consent.
8. Client unlock request.
9. Consultant disclosure approval.
10. Disclosed client view.
11. Interview feedback.
12. Placement.
13. Commission/revenue surface.
14. Owner commercial/accounting proof.
15. Admin governance/audit/replay proof.

- [ ] **Step 3: Capture evidence per step**

For non-P0 UI orientation steps, record at least one of:

- Browser screenshot.
- API response summary.
- Server log excerpt.
- Database row/query output.
- WorkflowEvent/audit evidence.
- E2E scenario evidence.

Expected: the evidence log Golden Path table has no unexplained `not-verified` row for a step that was attempted.

For canonical/truth write, candidate consent, unlock/disclosure, placement, commission, owner commercial/accounting proof, admin governance/audit/replay proof, and any audit trace claim, screenshots alone are insufficient. Record runtime/API behavior plus persisted row evidence or WorkflowEvent/audit evidence tied to the same transaction IDs, otherwise keep the decision below `ready-for-controlled-pilot`.

- [ ] **Step 4: Maintain the transaction trace ledger**

After each successful golden path step, update `Transaction Trace Ledger` with newly created IDs and refs:

- candidate/profile/source document IDs
- AI task run IDs, claim ledger IDs, source span, assertion strength, speaker, verification status, canonical-write permission, client shareability, and review/audit linkage
- job, match report, shortlist, shortlist card IDs
- re-identification assessment IDs
- consent, unlock, disclosure IDs, profile version, consent text version, disclosure level, human approver, and WorkflowEvent IDs
- feedback, placement, commission IDs
- owner revenue/accounting evidence refs
- admin governance/audit/replay evidence refs
- WorkflowEvent/audit trace IDs

Expected: every later claim can be correlated to the same RC1 transaction rather than to unrelated seed data.

- [ ] **Step 5: Classify operator confusion**

For any step where the operator does not know what to do next, record:

- Page/route.
- Missing cue or unclear state.
- Expected next action.
- Whether the backend is blocked or only the UI explanation is unclear.

Expected: UX confusion is recorded as a pilot readiness issue, not mixed with backend correctness unless evidence supports it.

- [ ] **Step 6: Validate post-walkthrough pilot data health**

Run:

```bash
rtk npm run pilot:data:validate
```

Expected:

- Command exits `0`, or the evidence log records why the walkthrough intentionally changed validation expectations.
- If validation fails because real workflow-created records exist, classify the exact issue instead of treating it as automatic product failure.

### Task RC1-10: Capability Map And AI Agent Readiness Synthesis

**Files:**
- Modify: `docs/release/RC1-capability-map.md`
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Read:
  - `docs/release/RC1-pilot-readiness-evidence.md`
  - `docs/release/RC1-capability-map.md`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotacceptance/PilotAcceptanceGate.java`

Historical baseline note: `PilotAcceptanceGate` is a Task 42 coverage map and historical acceptance artifact. It can guide which capabilities and tests to re-check, but it is not current RC1 pass evidence unless the corresponding command/runtime/persistence/audit proof was collected in this RC1 run.

- [ ] **Step 1: Recheck known prior Evidence Gate risk surfaces**

Run:

```bash
rtk proxy zsh -lc 'if [ -f audit-reports/evidence-gate-2026-06-03/SYNTHESIS.md ]; then sed -n "1,220p" audit-reports/evidence-gate-2026-06-03/SYNTHESIS.md; else printf "No local prior Evidence Gate synthesis found. Recheck known risk surfaces from source.\n"; fi'
rtk rg -n "client-safe|raw Candidate|canonical write|WorkflowEvent|AITaskRun|re-identification|prior contact|prior application|HUMAN_ACKNOWLEDGED" services/core-api/src/main/java/com/recruitingtransactionos/coreapi/{apiboundary,shortlist,governedintake,truthlayer,consultantmatching,interaction,interviewfeedback} services/core-api/src/test/java/com/recruitingtransactionos/coreapi/{apiboundary,shortlist,governedintake,truthlayer,consultantmatching,interaction,interviewfeedback}
```

Expected: every known high-risk surface is classified as one of:

- `resolved-current-evidence`: current RC1 evidence proves the risk no longer applies.
- `still-open`: current source/runtime evidence shows the risk remains.
- `not-rechecked`: RC1 did not collect enough evidence to decide.

Known risk surfaces to recheck at minimum:

- Pre-unlock client shortlist metadata must not bypass client-safe projection or expose identity/contact fields.
- Company/job reviewed-fact publish must not bypass canonical-write evidence and audit lineage.
- AI-generated or reviewed claims must retain AITaskRun and review linkage where required.
- Claim Ledger evidence must include source span, assertion strength, speaker, verification status, canonical-write permission, and client shareability, not just a claim ID.
- Shortlist sendability must require client-safe cards, evidence readiness, and re-identification gates.
- HUMAN_ACKNOWLEDGED or bulk approval must not count as independent trusted matching evidence.
- Prior contact/application, unlock/disclosure, placement, commission, and protected card-level transitions must have WorkflowEvent/audit coverage.
- MatchReport evidence must cover confidence, evidence coverage, provenance weighting, authenticity risk, score caps, and ontology/industry-pack version or an explicit risk note.
- Browser privacy negative evidence must not be inferred from backend selected tests alone.
- Operations readiness must be current evidence or a bounded nonclaim, not historical runbook text presented as pass evidence.

- [ ] **Step 2: Reconcile capability map against evidence**

Run:

```bash
rtk sed -n '1,260p' docs/release/RC1-pilot-readiness-evidence.md
rtk sed -n '1,260p' docs/release/RC1-capability-map.md
rtk sed -n '1,230p' services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotacceptance/PilotAcceptanceGate.java
```

Expected: every `implemented` capability has current evidence; every unsupported claim is downgraded to `partial`, `scaffold`, `no-op`, or `not-verified`. Historical Task 42/Task 60 evidence is labelled as historical context, not current pass evidence.

- [ ] **Step 3: Reconcile Spec-To-RC1 Traceability**

Read the `Spec-To-RC1 Traceability` section in this plan and mark each row in the evidence log as:

- `passed`: required evidence exists for this RC1 run.
- `partial`: source/test evidence exists but runtime, persistence, browser, or audit proof is incomplete.
- `runtime-gap-recorded`: implementation/test/source evidence exists but current runtime/API plus persistence/audit proof is missing.
- `not-verified`: no current evidence was collected.
- `blocked-environment`, `blocked-product`, or `blocked-dependency`: exact blocker is recorded.
- `out-of-rc1-with-risk-note`: out of RC1 scope and explicitly bounded in the final memo.

Expected: RC1-11 cannot write a final decision until every traceability row has one of these statuses.

- [ ] **Step 4: Triangulate every critical capability**

For each critical capability, require at least two evidence sources before marking it `implemented`:

- Source owner: controller/service/port or frontend API module.
- Test or gate: unit, contract, integration, release gate, or E2E.
- Runtime/persistence/audit: API response, DB row, WorkflowEvent, log, or Playwright evidence.

Critical capabilities:

- Candidate intake.
- AI draft claims.
- Claim Ledger governance.
- Canonical/truth write.
- Job intake and activation.
- Match report.
- MatchReport v2.1 governance.
- Anonymous shortlist.
- Re-identification risk.
- Candidate consent.
- Unlock/disclosure.
- Interview feedback.
- Placement.
- Commission/revenue.
- Owner commercial/accounting proof.
- Admin governance/audit/replay proof.
- Governance/audit.
- Operations readiness.

Expected: single-source evidence results in `partial`, not `implemented`.

- [ ] **Step 5: Identify candidate AI agents**

For each AI Agent Readiness row, write:

- Current subsystem.
- What it can do today.
- Whether it is deterministic/local, live-provider-backed, no-op, or not verified.
- Required preconditions before turning it into an autonomous agent.

Expected: agentization remains a future architecture decision. RC1 does not refactor the system into agents.

- [ ] **Step 6: Update blocker list**

Rank blockers:

- `P0`: prevents RC1 transaction chain or hard invariant.
- `P1`: allows controlled pilot only with workaround or manual operation.
- `P2`: useful hardening after RC1.

Expected: blocker list is short, evidence-backed, and has minimal next actions.

- [ ] **Step 7: Produce a black-box deconstruction summary**

Add a concise section to `RC1-capability-map.md`:

```markdown
## Black Box Deconstruction Summary

### What The System Can Do With Current Evidence

### What Exists But Is Only Partial Or Scaffold

### What Is Explicitly No-op

### What Is Still Not Verified

### What Looks Like A Future AI Agent Boundary
```

Expected: this section helps the user understand what the 160k-line system can actually do without reading the codebase line by line.

### Task RC1-11: RC1 Readiness Decision

**Files:**
- Modify: `docs/release/RC1-pilot-readiness-evidence.md`
- Read:
  - `docs/release/release-checklist.md`
  - `docs/release/RC1-pilot-readiness-evidence.md`
  - `docs/release/RC1-capability-map.md`

- [ ] **Step 1: Check hard pass criteria**

RC1 is `ready-for-controlled-pilot` only if all of these are true:

- `release:migrations` passed with PostgreSQL/Testcontainers application evidence.
- Synthetic pilot data rebuild and validate passed.
- `test:core-api` passed.
- `build:core-api` passed.
- Frontend test, typecheck, and build passed.
- v2.0/v2.1 route preservation was checked; any missing render-level route smoke is classified and does not overclaim runtime route usability.
- `release:privacy-security` passed.
- Browser privacy negative proof passed with E3/E4 browser/API evidence, or the final decision is below `ready-for-controlled-pilot`.
- `release:ai-eval` passed.
- Claim Ledger field-level evidence is current for any exercised AI claim path, including source span, assertion strength, speaker, verification status, canonical-write permission, and client shareability.
- `release:e2e:pilot` passed.
- `release:gate` exited `0` and printed `RELEASE_READY`.
- Operations readiness rows are current or explicitly bounded as nonclaims, with no non-waivable privacy/security/data-loss blocker.
- Golden path has no P0 blocker.
- Canonical write, consent, unlock/disclosure, placement, commission, owner commercial/accounting proof, and admin governance/audit/replay proof each have runtime/API plus persistence or WorkflowEvent/audit evidence tied to the same transaction ledger.
- MatchReport v2.1 governance has evidence for confidence, evidence coverage, provenance weighting, authenticity risk, score caps, and ontology/industry-pack version, or any gap is explicitly classified below readiness.
- Placement and commission have current runtime/API plus persistence or audit evidence.
- Owner commercial/accounting proof has current runtime/API plus persistence or WorkflowEvent/audit evidence.
- Admin governance/audit/replay proof has current runtime/API plus persistence or WorkflowEvent/audit evidence.
- Client raw Candidate access before unlock/disclosure remains blocked.
- Client-safe re-identification gate evidence is current for the exercised shortlist/card path.
- Risk-tiered review / bulk approve downgrade evidence is current or explicitly not exercised with a risk note.
- AI direct canonical fact ownership remains blocked.
- Spec-To-RC1 Traceability rows are reconciled.
- Manual operator walkthrough completed with no P0 operator-confusion blocker, or the evidence log explicitly records equivalent operator-readiness evidence accepted by the user for this RC1 run.
- Evidence log and capability map are current for the evaluated commit.
- No required gate is only `waived-with-risk-acceptance`.

- [ ] **Step 2: Write decision**

Set the evidence log `Decision` field to exactly one of:

- `ready-for-controlled-pilot`
- `not-ready-blocked-environment`
- `not-ready-blocked-product`
- `not-ready-blocked-dependency`
- `not-ready-insufficient-evidence`

Then write:

- Passed evidence.
- Blocking evidence.
- Not verified.
- Minimal next action.
- Highest proven RC1 level from the Decision Ladder.
- Whether any intermediate evidence supports `automated-pre-placement-candidate`; this is a not-ready explanation, not a successful RC1 decision.
- Why `ready-for-controlled-pilot` is or is not supported by current intake-through-commission evidence.

- [ ] **Step 3: Final self-review**

Run:

```bash
rtk rg -n "not-recorded|not-decided|not-mapped|not-verified|TBD|TODO|maybe|probably" docs/release/RC1-pilot-readiness-evidence.md docs/release/RC1-capability-map.md
```

Expected:

- Remaining `not-verified` is allowed only when the decision explains why.
- `not-recorded`, `not-decided`, and `not-mapped` do not remain in fields that should have been filled during RC1.
- No `TBD` or `TODO` remains.

- [ ] **Step 4: Write final operator memo**

Add this section to `docs/release/RC1-pilot-readiness-evidence.md`:

```markdown
## Final Operator Memo

### Decision

### Highest Proven Level

### What Can Be Trusted Now

### What Cannot Be Claimed Yet

### Top P0/P1 Blockers

### Next 3 Codex Tasks

### Recommendation For Next Product Direction
```

Expected: the memo is short enough to read before the next planning session and strict enough that it does not turn partial evidence into readiness.

---

## Execution Policy For Fixes

When a gate fails:

1. Record the exact failure first.
2. Classify it as environment, product, dependency, flaky test, or unclear.
3. Create a separate minimal fix task with exact files and verification commands.
4. Do not broaden the task into feature work.
5. After the fix, rerun the failing gate before proceeding.

Recommended fix-task naming:

- `RC1-FIX-01-docker-testcontainers`
- `RC1-FIX-02-migration-application`
- `RC1-FIX-03-backend-regression`
- `RC1-FIX-04-pilot-e2e-flow`
- `RC1-FIX-05-golden-path-operator-confusion`

Every fix task must use this template:

```markdown
### Task RC1-FIX-NN: [Exact Blocker Name]

**Blocker Evidence:** [quote or artifact path from the failed gate]

**Hypothesis:** [one likely cause, labelled as inference]

**Files:**
- Modify: `[exact file]`
- Test: `[exact test file]`
- Evidence: `[artifact path]`

- [ ] Step 1: Reproduce the blocker with the exact command.

Run: `[exact command]`
Expected: fails with `[specific failure]`

- [ ] Step 2: Add or identify the smallest regression test.

Run: `[focused test command]`
Expected: fails before fix or already covers the blocker.

- [ ] Step 3: Apply the smallest fix.

Change only files needed for the blocker.

- [ ] Step 4: Rerun the focused test.

Run: `[focused test command]`
Expected: exits 0.

- [ ] Step 5: Rerun the blocked RC1 gate.

Run: `[original gate command]`
Expected: exits 0.

- [ ] Step 6: Update evidence and capability rows.

Expected: blocker row moves to resolved or downgraded with current evidence.
```

Fix tasks must not:

- Add unrelated product scope.
- Hide failures behind skips.
- Use live providers or production data.
- Change security/privacy gates to make them easier to pass.
- Mark RC1 ready without rerunning the gate that originally failed.

## Execution Options

Recommended execution mode: Subagent-Driven.

Reason: each RC1 task is bounded, evidence-heavy, and can be reviewed before the next task runs. This matches the user's preferred workflow of plan first, task queue second, Codex execution third.

Alternative execution mode: Inline Execution.

Use this only if the user wants one continuous session to run gates and update documents without spawning task workers.

## Plan Self-Review

- Spec coverage: covered v2.1 source of truth, backend truth ownership, AI claim boundary, PostgreSQL, WorkflowEvent, client disclosure boundary, release checklist gates, pilot E2E, manual golden path, and capability mapping.
- Placeholder scan: this plan intentionally uses `not-verified`, `not-recorded`, and `not-mapped` only inside templates for future task execution. It does not use them as final claims.
- Scope check: RC1 is limited to controlled pilot readiness and black-box deconstruction; public launch, cloud signoff, live providers, and agent refactor are excluded.
- Type consistency: status vocabulary is consistent across evidence log, capability map, blocker list, and decision criteria.
