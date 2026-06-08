# RC1 Pilot Readiness Audit - 02 Spec Coverage

## Scope and evidence read

Scope: product/spec coverage audit of `docs/release/RC1-pilot-readiness-plan.md` against the current product source of truth for RC1 controlled-pilot readiness.

Evidence read:

- `docs/release/RC1-pilot-readiness-plan.md`
- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/specs/v2.0/product-spec-v2.0.md`
- Bounded route/surface verification only:
  - `apps/web/src/portalRouteContract.test.ts`
  - `apps/web/src/App.tsx`

Audit boundary: read-only except this report. I did not run release gates, servers, browser E2E, migrations, Docker, or tests. Runtime readiness remains not verified in this component audit.

## Coverage matrix

| Spec requirement | RC1 plan coverage | Gap / risk |
| --- | --- | --- |
| v2.1 is source of truth; v2.0 is preserved as UI baseline | Covered. `CURRENT_SPEC.md` states v2.1 is current, v2.0 is preserved, v2.0 UI/routes must not be deleted, consultant is one unified portal, backend owns truth, PostgreSQL is target source, key transitions create `WorkflowEvent`, and clients cannot read raw `Candidate` pre-unlock (`docs/specs/CURRENT_SPEC.md:3`, `docs/specs/CURRENT_SPEC.md:5`, `docs/specs/CURRENT_SPEC.md:7`, `docs/specs/CURRENT_SPEC.md:9`, `docs/specs/CURRENT_SPEC.md:13`, `docs/specs/CURRENT_SPEC.md:15`, `docs/specs/CURRENT_SPEC.md:17`, `docs/specs/CURRENT_SPEC.md:19`, `docs/specs/CURRENT_SPEC.md:21`). The RC1 plan repeats these invariants (`docs/release/RC1-pilot-readiness-plan.md:38`, `docs/release/RC1-pilot-readiness-plan.md:40`, `docs/release/RC1-pilot-readiness-plan.md:41`, `docs/release/RC1-pilot-readiness-plan.md:42`). | Needs explicit traceability row so final RC1 evidence can prove this was checked, not merely restated. |
| Full retention of v2.0 UI, seven UI designs, five portals, consultant as one portal | Partially covered. v2.1 requires full retention of v2.0 UI assets and says governance belongs inside the same five portals, not a separate governance system (`docs/specs/v2.1/product-spec-v2.1.md:25`, `docs/specs/v2.1/product-spec-v2.1.md:26`, `docs/specs/v2.1/product-spec-v2.1.md:29`, `docs/specs/v2.1/product-spec-v2.1.md:223`, `docs/specs/v2.1/product-spec-v2.1.md:225`). The plan preserves the invariant (`docs/release/RC1-pilot-readiness-plan.md:41`, `docs/release/RC1-pilot-readiness-plan.md:42`) and asks RC1-04 to map portal entry points (`docs/release/RC1-pilot-readiness-plan.md:777`, `docs/release/RC1-pilot-readiness-plan.md:779`, `docs/release/RC1-pilot-readiness-plan.md:786`). Current code has a route contract test for v2.1/v2.0 owner, consultant, client, candidate, and admin route sets (`apps/web/src/portalRouteContract.test.ts:7`, `apps/web/src/portalRouteContract.test.ts:26`, `apps/web/src/portalRouteContract.test.ts:49`, `apps/web/src/portalRouteContract.test.ts:63`, `apps/web/src/portalRouteContract.test.ts:73`). | RC1-04 does not name `portalRouteContract.test.ts` or require a v2.0/v2.1 route-contract evidence row. A worker could map routes manually and miss the preservation test. |
| Five user portals with routes, pages, permissions, state, core actions | Partially covered. v2.1 acceptance requires Owner, Consultant, Client, Candidate, and Admin routes/pages/permissions/states/actions, with Consultant unified (`docs/specs/v2.1/product-spec-v2.1.md:905`). `App.tsx` routes all five portal roots (`apps/web/src/App.tsx:157`, `apps/web/src/App.tsx:158`, `apps/web/src/App.tsx:159`, `apps/web/src/App.tsx:160`, `apps/web/src/App.tsx:161`, `apps/web/src/App.tsx:162`). The RC1 manual walkthrough spans five portal surfaces where available (`docs/release/RC1-pilot-readiness-plan.md:1100`). | Good for surface discovery, but route presence is not permission/state/core-action proof. Final evidence should separate static route preservation from runtime journey readiness. |
| AI outputs claims, not facts; Claim Ledger before canonical facts | Covered with one traceability gap. v2.1 requires Claim Ledger fields and canonical write gates (`docs/specs/v2.1/product-spec-v2.1.md:489`, `docs/specs/v2.1/product-spec-v2.1.md:493`, `docs/specs/v2.1/product-spec-v2.1.md:499`, `docs/specs/v2.1/product-spec-v2.1.md:501`, `docs/specs/v2.1/product-spec-v2.1.md:544`, `docs/specs/v2.1/product-spec-v2.1.md:545`, `docs/specs/v2.1/product-spec-v2.1.md:921`, `docs/specs/v2.1/product-spec-v2.1.md:922`). The plan includes Golden Path steps for AI claims, human review, and canonical/truth write (`docs/release/RC1-pilot-readiness-plan.md:75`, `docs/release/RC1-pilot-readiness-plan.md:76`, `docs/release/RC1-pilot-readiness-plan.md:77`, `docs/release/RC1-pilot-readiness-plan.md:78`) and P0 proof rows for AI canonical ownership and truth-layer evidence (`docs/release/RC1-pilot-readiness-plan.md:369`, `docs/release/RC1-pilot-readiness-plan.md:373`). | Add explicit ClaimLedgerItem field-level evidence expectations to the traceability table so source span, assertion strength, speaker, verification status, and shareability are not reduced to a generic "AI claims" row. |
| Risk-tiered human review and anti-false confirmation | Partially covered. v2.1 requires risk-tiered review and states bulk approve cannot become `external_verified` / `candidate_confirmed`; review velocity and sample audit must be queryable (`docs/specs/v2.1/product-spec-v2.1.md:467`, `docs/specs/v2.1/product-spec-v2.1.md:471`, `docs/specs/v2.1/product-spec-v2.1.md:475`, `docs/specs/v2.1/product-spec-v2.1.md:477`, `docs/specs/v2.1/product-spec-v2.1.md:479`, `docs/specs/v2.1/product-spec-v2.1.md:485`, `docs/specs/v2.1/product-spec-v2.1.md:920`). The plan rechecks `HUMAN_ACKNOWLEDGED` and bulk approval in RC1-10 (`docs/release/RC1-pilot-readiness-plan.md:1187`, `docs/release/RC1-pilot-readiness-plan.md:1202`). | No Gate Evidence or P0/P1 proof row directly protects anti-false-confirmation. Risk: the final decision can pass AI/canonical rows while not proving bulk-approve downgrade and review-quality auditability. |
| Backend owns truth; PostgreSQL is target source of truth | Covered. v2.1 technical architecture requires Java/Spring Boot core backend, PostgreSQL source of truth, and no AI/tool bypass of domain service (`docs/specs/v2.1/product-spec-v2.1.md:173`, `docs/specs/v2.1/product-spec-v2.1.md:181`, `docs/specs/v2.1/product-spec-v2.1.md:884`, `docs/specs/v2.1/product-spec-v2.1.md:927`). The plan includes Docker/Testcontainers, migration validation, backend regression/build, and release-gate checks (`docs/release/RC1-pilot-readiness-plan.md:502`, `docs/release/RC1-pilot-readiness-plan.md:548`, `docs/release/RC1-pilot-readiness-plan.md:788`, `docs/release/RC1-pilot-readiness-plan.md:970`). | No material spec coverage gap found. Runtime pass/fail is not verified in this report. |
| Every key state transition creates `WorkflowEvent`; audit and replay | Partially covered. v2.1 requires every key state transition write `WorkflowEvent` and every AI task / human confirmation / unlock / disclosure / score generation be replayable, attributable, and exportable (`docs/specs/v2.1/product-spec-v2.1.md:682`, `docs/specs/v2.1/product-spec-v2.1.md:895`, `docs/specs/v2.1/product-spec-v2.1.md:928`). The plan has WorkflowEvent trace in the transaction ledger (`docs/release/RC1-pilot-readiness-plan.md:306`), Golden Path audit rows (`docs/release/RC1-pilot-readiness-plan.md:363`), manual evidence requirements (`docs/release/RC1-pilot-readiness-plan.md:1125`), and hard pass criteria for owner/admin audit trace (`docs/release/RC1-pilot-readiness-plan.md:1307`). | Replay/export is less explicit than event creation. Add a traceability row for "audit replay/export evidence" or define it as out-of-RC1 with rationale. |
| Client-safe shortlist, consent, unlock, disclosure, raw Candidate blocked pre-unlock | Covered. v2.1 and v2.0 require anonymity, consent/disclosure checks, and DisclosureRecord (`docs/specs/v2.1/product-spec-v2.1.md:797`, `docs/specs/v2.1/product-spec-v2.1.md:799`, `docs/specs/v2.1/product-spec-v2.1.md:809`, `docs/specs/v2.1/product-spec-v2.1.md:910`; `docs/specs/v2.0/product-spec-v2.0.md:566`, `docs/specs/v2.0/product-spec-v2.0.md:578`). The plan includes golden path rows and P0 proofs for consent, unlock/disclosure, and raw Candidate blocking (`docs/release/RC1-pilot-readiness-plan.md:83`, `docs/release/RC1-pilot-readiness-plan.md:87`, `docs/release/RC1-pilot-readiness-plan.md:370`, `docs/release/RC1-pilot-readiness-plan.md:371`, `docs/release/RC1-pilot-readiness-plan.md:372`, `docs/release/RC1-pilot-readiness-plan.md:1308`). | Re-identification risk is only partially explicit; see separate row. |
| Re-identification risk before shortlist send | Partially covered. v2.1 requires shortlist re-identification risk scoring and automatic generalization/blocking for high-risk summaries (`docs/specs/v2.1/product-spec-v2.1.md:823`, `docs/specs/v2.1/product-spec-v2.1.md:827`, `docs/specs/v2.1/product-spec-v2.1.md:831`, `docs/specs/v2.1/product-spec-v2.1.md:925`). RC1-10 lists shortlist sendability with re-identification gates as a known risk surface (`docs/release/RC1-pilot-readiness-plan.md:1201`). | The P0 Proof Matrix only says client cannot read raw Candidate and consent is required; it does not separately require re-identification scorer evidence before shortlist send. |
| Match Score v2.1: score confidence, evidence coverage, provenance weighting, authenticity risk, score caps | Partially covered / under-scoped. v2.1 requires score confidence, evidence coverage, provenance weight, authenticity risk, ontology version, and explicit score caps (`docs/specs/v2.1/product-spec-v2.1.md:725`, `docs/specs/v2.1/product-spec-v2.1.md:729`, `docs/specs/v2.1/product-spec-v2.1.md:734`, `docs/specs/v2.1/product-spec-v2.1.md:736`, `docs/specs/v2.1/product-spec-v2.1.md:740`, `docs/specs/v2.1/product-spec-v2.1.md:745`, `docs/specs/v2.1/product-spec-v2.1.md:923`, `docs/specs/v2.1/product-spec-v2.1.md:926`). The plan has `Match report` in the golden path and capability map (`docs/release/RC1-pilot-readiness-plan.md:81`, `docs/release/RC1-pilot-readiness-plan.md:354`, `docs/release/RC1-pilot-readiness-plan.md:417`) and an AI eval gate (`docs/release/RC1-pilot-readiness-plan.md:875`, `docs/release/RC1-pilot-readiness-plan.md:890`, `docs/release/RC1-pilot-readiness-plan.md:893`). | Missing explicit RC1 evidence rows for cold/seeded score caps, provenance weighting, authenticity risk, and ontology-version/stale-warning behavior. A matching flow could pass while not proving v2.1 matching governance. |
| Industry packs, cold-start maturity, Living Ontology | Missing as first-class RC1 coverage. v2.1 requires IndustryPack maturity and versioned, deprecated, auditable ontology with stale warnings (`docs/specs/v2.1/product-spec-v2.1.md:764`, `docs/specs/v2.1/product-spec-v2.1.md:768`, `docs/specs/v2.1/product-spec-v2.1.md:773`, `docs/specs/v2.1/product-spec-v2.1.md:777`, `docs/specs/v2.1/product-spec-v2.1.md:781`, `docs/specs/v2.1/product-spec-v2.1.md:924`). The plan mentions capability mapping and AI eval, but no RC1 task explicitly verifies these fields or classifies them as deferred. | Decide whether this is a must-have RC1 gate because MatchReport is in the pilot chain, or document it as a non-RC1 item with a controlled-pilot risk acceptance. Current plan leaves it ambiguous. |
| Placement, commission, revenue/accounting handoff | Covered. v2.1 workflow and delivery plan include Placement/Commission (`docs/specs/v2.1/product-spec-v2.1.md:168`, `docs/specs/v2.1/product-spec-v2.1.md:680`, `docs/specs/v2.1/product-spec-v2.1.md:851`). The RC1 plan explicitly refuses readiness without placement and commission evidence (`docs/release/RC1-pilot-readiness-plan.md:103`, `docs/release/RC1-pilot-readiness-plan.md:120`, `docs/release/RC1-pilot-readiness-plan.md:142`), adds RC1-08A commercial closure (`docs/release/RC1-pilot-readiness-plan.md:1017`, `docs/release/RC1-pilot-readiness-plan.md:1030`), and hard pass criteria require runtime/API plus persistence/audit evidence (`docs/release/RC1-pilot-readiness-plan.md:1306`). | Strong coverage. No spec-to-plan issue found. |
| Pilot user journeys across owner / consultant / client / candidate / admin | Partially covered. The plan's Golden Path covers consultant intake/review/shortlist/disclosure/placement, client job/shortlist/unlock/feedback, candidate consent, and admin/owner trace (`docs/release/RC1-pilot-readiness-plan.md:75`, `docs/release/RC1-pilot-readiness-plan.md:91`). Manual verification repeats a five-portal walkthrough (`docs/release/RC1-pilot-readiness-plan.md:1100`, `docs/release/RC1-pilot-readiness-plan.md:1115`). | Owner and Admin are conflated late in the journey. Owner revenue/placements/commission and Admin audit/governance should be separate manual proof rows. |
| Scope control: controlled pilot, not public SaaS/cloud/certification/live providers | Covered. The plan excludes public SaaS, managed cloud signoff, formal certification, customer go-live approval, production email/SMS, live external AI providers, large features/refactors/agent conversion, pricing/billing/sales packaging/acquisition positioning (`docs/release/RC1-pilot-readiness-plan.md:60`, `docs/release/RC1-pilot-readiness-plan.md:69`). It also says agentization remains future architecture and RC1 does not refactor into agents (`docs/release/RC1-pilot-readiness-plan.md:1242`, `docs/release/RC1-pilot-readiness-plan.md:1251`). | No major over-scope. The capability map / black-box deconstruction is extra but bounded and supports readiness evidence. |

## Severity-ranked findings

### P1 - Add a first-class spec-to-plan traceability table before RC1 execution

Observed evidence:

- v2.1 acceptance criteria define concrete product categories, including full v2.0 retention, anti-false confirmation, Claim Ledger, canonical gates, cold-start governance, Living Ontology, re-identification risk, provenance weighting, technical architecture, and audit/replay (`docs/specs/v2.1/product-spec-v2.1.md:919`, `docs/specs/v2.1/product-spec-v2.1.md:928`).
- The RC1 plan has transaction capability rows for Candidate intake through Governance/audit (`docs/release/RC1-pilot-readiness-plan.md:410`, `docs/release/RC1-pilot-readiness-plan.md:424`), but no table that maps each v2.1 acceptance requirement to RC1 tasks, evidence rows, and final decision criteria.
- The plan self-review says spec coverage is covered (`docs/release/RC1-pilot-readiness-plan.md:1454`), but that is a narrative assertion rather than an auditable traceability artifact.

Inference:

RC1 tasks can pass their local command/checklist expectations while still leaving v2.1 acceptance categories only implied. This is most visible for matching governance, Living Ontology, anti-false confirmation, route preservation, and audit replay/export.

Likely cause:

The plan is organized around release gates and transaction evidence, not around a spec requirement map.

Recommended change:

Add a `Spec-To-RC1 Traceability` section near the top of the plan and require RC1-10/RC1-11 to reconcile it before the final decision.

### P1 - v2.1 matching governance is under-specified for an RC1 that includes MatchReport

Observed evidence:

- v2.1 requires `score_confidence`, `evidence_coverage`, `provenance_weight`, `authenticity_risk`, and `ontology_version` in MatchScore governance (`docs/specs/v2.1/product-spec-v2.1.md:725`, `docs/specs/v2.1/product-spec-v2.1.md:734`).
- v2.1 score caps limit no-two-independent-evidence cases to 4, cold Industry Pack to 3, keyword-only core skills to 3, weak-signal intent to 3, high re-identification summaries to no-send, and stale ontology to 4 (`docs/specs/v2.1/product-spec-v2.1.md:736`, `docs/specs/v2.1/product-spec-v2.1.md:745`).
- The RC1 Golden Path includes MatchReport generation (`docs/release/RC1-pilot-readiness-plan.md:81`) and the capability map includes a Matching row (`docs/release/RC1-pilot-readiness-plan.md:417`), but the P0 Proof Matrix does not include score caps, provenance, authenticity risk, evidence coverage, or ontology version (`docs/release/RC1-pilot-readiness-plan.md:365`, `docs/release/RC1-pilot-readiness-plan.md:375`).

Inference:

The plan may verify that a match report exists without verifying the v2.1 reason that the match report can be trusted in a controlled pilot.

Likely cause:

The RC1 plan strengthened transaction closure after recognizing placement/commission gaps, but the same explicit treatment was not applied to v2.1 matching quality gates.

Recommended change:

Add a P1/P0 proof row for `MatchReport v2.1 governance`, and make RC1-06, RC1-07, RC1-09, and RC1-10 record whether score confidence, evidence coverage, provenance weighting, authenticity risk, score caps, and ontology version are passed, partial, or not verified.

### P1 - v2.0/v2.1 route preservation is declared but not wired to the existing route contract test

Observed evidence:

- v2.1 says the seven v2.0 UI designs and same five-portal information architecture remain the baseline (`docs/specs/v2.1/product-spec-v2.1.md:223`, `docs/specs/v2.1/product-spec-v2.1.md:225`).
- The plan declares v2.0 UI/portal definitions must not be deleted/compressed/replaced (`docs/release/RC1-pilot-readiness-plan.md:41`) and asks RC1-04 to map portal entry points by reading `App.tsx` and running broad `rg` over `apps/web/src` (`docs/release/RC1-pilot-readiness-plan.md:777`, `docs/release/RC1-pilot-readiness-plan.md:784`).
- Current source has a direct route contract test named "keeps every v2.1/v2.0 ... route named in the spec reachable" for Owner, Consultant, Client, Candidate, and Admin (`apps/web/src/portalRouteContract.test.ts:7`, `apps/web/src/portalRouteContract.test.ts:99`), and `App.tsx` mounts `/owner/*`, `/consultant/*`, `/client/*`, `/candidate/*`, and `/admin/*` (`apps/web/src/App.tsx:157`, `apps/web/src/App.tsx:162`).

Inference:

The plan preserves the idea of v2.0 UI, but the most precise local verification hook is not named. A worker could update the capability map from route search while omitting the formal route-preservation test from evidence.

Likely cause:

RC1-04 was written as a bounded source-mapping task, not a spec-preservation task.

Recommended change:

Add `apps/web/src/portalRouteContract.test.ts` to RC1-04 reads and require the frontend evidence row to note whether the route contract ran under `npm --workspace @rto/web run test`.

### P2 - Owner and Admin pilot journeys are conflated at the final evidence step

Observed evidence:

- v2.1 Owner/Partner routes include revenue, placements, commission, risk, data quality, AI quality, and audit (`docs/specs/v2.1/product-spec-v2.1.md:270`, `docs/specs/v2.1/product-spec-v2.1.md:280`).
- v2.1 Admin/System routes include AI policy, AI task registry, industry packs, schema, workflow rules, permissions, audit log, integrations, and security (`docs/specs/v2.1/product-spec-v2.1.md:356`, `docs/specs/v2.1/product-spec-v2.1.md:364`), plus v2.1 extensions for claim ledger, review quality, ontology governance, and privacy redaction (`docs/specs/v2.1/product-spec-v2.1.md:370`, `docs/specs/v2.1/product-spec-v2.1.md:373`).
- The plan uses `Admin/owner audit trace` as one Golden Path step (`docs/release/RC1-pilot-readiness-plan.md:91`) and one manual walkthrough step (`docs/release/RC1-pilot-readiness-plan.md:1115`), while RC1-08A separately mentions owner revenue and accounting export handoff (`docs/release/RC1-pilot-readiness-plan.md:1051`).

Inference:

The five-role journey is mostly clear, but Owner business proof and Admin governance proof can collapse into one late evidence row, making operator-readiness and governance-readiness harder to assess independently.

Likely cause:

The final journey steps compress two non-consultant oversight personas into a single shorthand.

Recommended change:

Split manual evidence and final decision rows into `Owner revenue/placement/commission/accounting view` and `Admin governance/audit/replay view`.

### P2 - Anti-false-confirmation coverage is only a synthesis recheck, not a gate row

Observed evidence:

- v2.1 states bulk approve can never write `external_verified`, defaults to `human_acknowledged`, and must record review velocity and sample audit triggers (`docs/specs/v2.1/product-spec-v2.1.md:477`, `docs/specs/v2.1/product-spec-v2.1.md:487`).
- The RC1 plan searches for `HUMAN_ACKNOWLEDGED` and requires it not count as independent trusted matching evidence in RC1-10 (`docs/release/RC1-pilot-readiness-plan.md:1187`, `docs/release/RC1-pilot-readiness-plan.md:1202`).
- The P0 Proof Matrix has rows for AI canonical ownership and client raw Candidate protection, but not for bulk-approve downgrade or review-quality auditability (`docs/release/RC1-pilot-readiness-plan.md:365`, `docs/release/RC1-pilot-readiness-plan.md:375`).

Inference:

RC1 may catch a known prior risk late in synthesis, but anti-false-confirmation is important enough in v2.1 to be visible earlier in the gate/evidence template.

Likely cause:

The plan treats false-confirmation as a known risk surface to recheck, not as an acceptance category.

Recommended change:

Add an evidence row for `Risk-tiered review / bulk approve downgrade`, with minimum evidence from tests or runtime/API plus persisted `ReviewEvent` / `ReviewQualitySignal` / status evidence where available.

## Missing evidence / not verified

- I did not verify that any RC1 command exits `0`.
- I did not verify Docker, Testcontainers, PostgreSQL migration application, pilot data rebuild, backend tests, frontend tests, privacy/security gate, AI eval gate, Playwright E2E, release gate, manual walkthrough, placement/commission runtime behavior, or persisted audit rows.
- I did not inspect all backend implementation files. Code search was bounded to route/surface corroboration and was not used to make implementation readiness claims.
- I did not verify permissions/state/core actions for each route; static route presence is not runtime readiness.
- I did not verify whether current AI eval files already cover score caps, provenance weighting, authenticity risk, cold-start warning, or ontology stale warning.

## Concrete patch suggestions for RC1 plan sections

### Add after `## Source Of Truth`

```markdown
## Spec-To-RC1 Traceability

RC1 workers must update this table as evidence is collected. A row can be `covered`
only when the named task records the required evidence in the evidence log or
capability map. Source-only mapping is `partial`, not readiness.

| Spec requirement | Spec source | RC1 task(s) | Evidence artifact / row | Minimum evidence | RC1 status | Gap / next action |
| --- | --- | --- | --- | --- | --- | --- |
| v2.0 UI and five-portal route preservation | CURRENT_SPEC; v2.1 5.1; v2.0 route tables | RC1-04, RC1-09, RC1-10 | Frontend regression; route contract evidence; capability map user surfaces | Frontend test includes portal route contract; manual route notes for pilot-critical portals | not-verified | Record route contract result and any missing route/action separately |
| AI claims, not facts / Claim Ledger | v2.1 11.6, 12.1, 13.4, 20.1 | RC1-05, RC1-06, RC1-07, RC1-09, RC1-10 | Golden Path rows: AI draft claims, Human review, Canonical/truth write | ClaimLedgerItem fields plus canonical write gate evidence | not-verified | Record source_span, assertion_strength, speaker, verification_status, client_shareability |
| Risk-tiered review and bulk-approve downgrade | v2.1 11.4, 11.5, 20.1 | RC1-05, RC1-06, RC1-10, RC1-11 | P0/P1 proof row: Risk-tiered review / bulk approve downgrade | Test or runtime evidence that bulk approve cannot create candidate_confirmed/external_verified and review quality signals are queryable | not-verified | Add evidence row before final decision |
| Backend truth and PostgreSQL source of truth | CURRENT_SPEC; v2.1 3.3, 19.1, 20.1 | RC1-02, RC1-03, RC1-05, RC1-08, RC1-11 | Docker/Testcontainers, Migration validation, Backend regression, Release gate | PostgreSQL/Testcontainers migration application plus backend pass evidence | not-verified | Existing plan coverage is sufficient |
| WorkflowEvent and audit/replay | CURRENT_SPEC; v2.1 14, 20.1 | RC1-05, RC1-07, RC1-08A, RC1-09, RC1-10, RC1-11 | Transaction Trace Ledger; P0 Proof Matrix; Governance/audit capability row | WorkflowEvent IDs plus audit/replay/export evidence for key transitions | not-verified | Split event creation from replay/export if replay/export is out of RC1 |
| Client-safe shortlist and re-identification risk | v2.1 17.3, 20.1 | RC1-06, RC1-07, RC1-09, RC1-10, RC1-11 | Golden Path: Anonymous shortlist; P0 proof: client-safe before unlock | Re-identification risk scorer, redaction/generalization/blocking evidence | not-verified | Add explicit re-identification proof row |
| MatchReport v2.1 governance | v2.1 15.2, 15.3, 16.1, 16.2, 20.1 | RC1-06, RC1-07, RC1-09, RC1-10, RC1-11 | Golden Path: Match report; Capability: Matching | score_confidence, evidence_coverage, provenance_weight, authenticity_risk, score caps, ontology_version/stale warning | not-verified | Add direct AI eval/runtime/test evidence |
| Placement and commission closure | v2.1 14, 18, 20.1 | RC1-08A, RC1-09, RC1-11 | Commercial closure; Placement; Commission/revenue | Runtime/API plus persistence/audit evidence | not-verified | Existing plan coverage is sufficient |
| Owner business proof and Admin governance proof | v2.1 Owner/Admin route tables, 20.1 | RC1-08A, RC1-09, RC1-10, RC1-11 | Separate Owner and Admin Golden Path rows | Owner revenue/placement/commission/accounting evidence; Admin audit/governance/replay evidence | not-verified | Split current Admin/owner row |
```

### Amend RC1-04

```markdown
- Read:
  - `apps/web/src/App.tsx`
  - `apps/web/src/portalRouteContract.test.ts`

Expected: frontend test evidence explicitly states whether the v2.0/v2.1 portal
route contract ran. Route presence supports UI preservation mapping only; runtime
operator readiness still requires RC1-07/RC1-09 evidence.
```

### Amend P0 Proof Matrix

```markdown
| MatchReport v2.1 governance enforces evidence, confidence, provenance, score caps, authenticity risk, and ontology version/stale-warning rules | E1 plus E3/E4 when exercised | not-verified |  |  |
| Re-identification risk scorer runs before shortlist send and blocks/generalizes unsafe summaries | E1 plus E3/E4 for shortlist send | not-verified |  |  |
| Bulk approve cannot create candidate_confirmed/external_verified and review-quality signals are inspectable | E1 plus persistence/audit evidence where available | not-verified |  |  |
| v2.0/v2.1 portal route contract remains intact | E1 plus source route mapping | not-verified |  |  |
```

### Amend RC1-09 manual journey

```markdown
12. Placement.
13. Owner revenue / placement / commission / accounting handoff view.
14. Admin governance / audit / replay view.
15. Cross-check that owner/admin evidence correlates to the same transaction trace ledger IDs.
```

## Top 5 improvement actions

1. Add the `Spec-To-RC1 Traceability` table and require RC1-10/RC1-11 to reconcile every row before final decision.
2. Add direct RC1 evidence rows for MatchReport v2.1 governance: score confidence, evidence coverage, provenance weighting, authenticity risk, score caps, and ontology version/stale warning.
3. Wire `apps/web/src/portalRouteContract.test.ts` into RC1-04 evidence so v2.0/v2.1 route preservation is tested, not only searched.
4. Split the final oversight journey into Owner commercial/accounting proof and Admin governance/audit/replay proof.
5. Add anti-false-confirmation as a gate/evidence row, not only an RC1-10 synthesis search.
