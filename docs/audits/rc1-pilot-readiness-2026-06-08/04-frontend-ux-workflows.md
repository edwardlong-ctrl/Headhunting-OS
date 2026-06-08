# RC1 Frontend, UX, Routes, And v2.0 UI Preservation Audit

## Scope and evidence read

Scope: frontend, UX, route/workflow, and v2.0 UI preservation readiness for RC1 pilot readiness.

Observed source of truth:

- `docs/release/RC1-pilot-readiness-plan.md:15-24` requires current evidence for the full controlled transaction chain and says historical acceptance cannot replace current RC1 evidence.
- `docs/release/RC1-pilot-readiness-plan.md:71-91` defines the golden path from consultant intake through placement, commission, and admin/owner audit trace.
- `docs/release/RC1-pilot-readiness-plan.md:727-787` defines frontend regression, typecheck, build, and route mapping tasks.
- `docs/release/RC1-pilot-readiness-plan.md:911-969` defines the pilot browser E2E gate.
- `docs/release/RC1-pilot-readiness-plan.md:1017-1080` explicitly acknowledges that current browser flow evidence stops before placement/commission unless RC1-08A closes it.
- `docs/release/RC1-pilot-readiness-plan.md:1083-1154` defines manual golden-path verification across five portal surfaces and operator-confusion capture.
- `docs/release/RC1-pilot-readiness-plan.md:1292-1313` requires placement, commission, owner/admin audit trace, and manual operator readiness before `ready-for-controlled-pilot`.
- `docs/specs/CURRENT_SPEC.md:3-21` makes v2.1 current, preserves v2.0 UI, requires one unified Consultant portal, backend truth, WorkflowEvent, and no raw Candidate access before unlock/disclosure.
- `docs/specs/v2.1/product-spec-v2.1.md:223-234` requires v2.0 UI preservation with v2.1 governance layered into the same five portals.
- `docs/specs/v2.1/product-spec-v2.1.md:264-375` defines Owner, Consultant, Client, Candidate, and Admin route surfaces.
- `docs/specs/v2.1/product-spec-v2.1.md:671-693` defines workflow states and key v2.1 gates.
- `docs/specs/v2.1/product-spec-v2.1.md:901-928` defines v2.1 acceptance criteria, including route completeness, shortlist gates, consent/disclosure, workflow, governance, and replayability.
- `docs/specs/v2.0/product-spec-v2.0.md:147-165` preserves the seven UI boards and clarifies Consultant is one portal.
- `docs/specs/v2.0/product-spec-v2.0.md:195-295` defines the v2.0 five-portal route baseline.

Observed current frontend and test evidence:

- `apps/web/src/App.tsx:26-84` defines the five portal shells and v2.1 governance modules.
- `apps/web/src/App.tsx:156-163` routes `/owner/*`, `/consultant/*`, `/client/*`, `/candidate/*`, and `/admin/*`.
- `apps/web/src/portalRouteContract.test.ts:7-99` checks v2.1/v2.0 route strings against portal source files.
- `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:151-167` keeps a unified Consultant navigation including intake, matching, shortlists, unlocks, placements, commission, workflow, reports, and settings.
- `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:4053-4082` declares Consultant routes for the spec surfaces.
- `apps/web/src/features/client-portal/ClientPortal.tsx:2332-2351` declares Client routes for job intake, clarification, shortlist, unlock, feedback, anonymous cards, and disclosed candidate detail.
- `apps/web/src/features/candidate-portal/CandidatePortal.tsx:1156-1221` declares Candidate routes for home, upload, profile review, follow-up, opportunities, status, and consent.
- `apps/web/src/features/owner-portal/OwnerPortal.tsx:520-533` declares Owner routes including placements, commission, revenue, risk, AI quality, and audit.
- `apps/web/src/features/admin-portal/AdminPortal.tsx:39-61` and `apps/web/src/features/admin-portal/AdminPortal.tsx:308-314` declare Admin governance sections and routes.
- `tests/e2e/pilot-business-flows.spec.ts:99-397` covers S01-S08 from intake through interview feedback, but not placement, commission, owner revenue, or admin audit.
- `tests/e2e/pilot-seed-login.spec.ts:16-56` verifies seed sign-in for consultant, client, candidate, owner, and admin portals.
- `apps/web/src/features/owner-portal/OwnerPortal.test.tsx:138-393` verifies owner placement, commission, revenue, and accounting handoff rendering with mocked data.

Commands run: read-only file inspection and `rtk git status --short`. I did not run frontend tests, builds, Playwright, backend, migrations, or a local server. Current runtime pass/fail is not verified.

Dirty worktree observed: `.gitignore` modified and `docs/release/RC1-pilot-readiness-plan.md` untracked. I did not modify or revert them.

## Route/workflow matrix

| Spec route/journey | Plan coverage | Current code/test evidence | Gap |
| --- | --- | --- | --- |
| Owner dashboard, pipeline, consultants, clients, revenue, placements, commission, risk, data quality, AI quality, audit | Golden path requires commission/revenue and owner/admin audit trace at `docs/release/RC1-pilot-readiness-plan.md:89-91`; manual walkthrough includes commission/revenue and admin/owner audit trace at `docs/release/RC1-pilot-readiness-plan.md:1113-1115`; hard criteria require placement/commission and owner/admin audit evidence at `docs/release/RC1-pilot-readiness-plan.md:1306-1307`. | Spec routes at `docs/specs/v2.1/product-spec-v2.1.md:270-280`; app routes at `apps/web/src/features/owner-portal/OwnerPortal.tsx:520-533`; revenue/accounting view at `apps/web/src/features/owner-portal/OwnerPortal.tsx:348-414`; mocked component coverage at `apps/web/src/features/owner-portal/OwnerPortal.test.tsx:138-393`; seed login only at `tests/e2e/pilot-seed-login.spec.ts:42-56`. | No current Playwright path proves owner placement, commission, revenue, accounting handoff, or audit screens against the same transaction created in S01-S08. Owner/admin audit trace is a readiness requirement, but current browser E2E does not exercise it. |
| Consultant unified portal: dashboard, intake, talent, companies, jobs, matching, outreach, shortlist, follow-ups, workflow, placements, commission | RC1 plan includes consultant intake, AI claim review, canonical write, job activation, match, shortlist, disclosure approval, placement, and commission at `docs/release/RC1-pilot-readiness-plan.md:75-90`; RC1-04 maps portal entry points at `docs/release/RC1-pilot-readiness-plan.md:777-786`; RC1-09 manual flow covers all steps at `docs/release/RC1-pilot-readiness-plan.md:1102-1115`. | Spec requires unified Consultant portal at `docs/specs/v2.1/product-spec-v2.1.md:282-317`; current nav includes the full consultant surface at `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:151-167`; current routes at `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:4053-4082`; current E2E covers consultant intake, job activation, matching, shortlist, unlock approval, and feedback review at `tests/e2e/pilot-business-flows.spec.ts:115-397`. | Consultant placement and commission routes exist, but E2E does not drive them. The placement/commission UI requires raw IDs from other pages (`apps/web/src/features/consultant-portal/ConsultantPortal.tsx:3219-3228`, `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:3405-3406`), which is weak for pilot operators unless the manual run has explicit ID handoff guidance. |
| Client dashboard, job intake, clarification, shortlist, anonymous candidate, unlock, feedback, profile | RC1 plan covers client/job intake, client shortlist review, unlock request, disclosed view, and interview feedback at `docs/release/RC1-pilot-readiness-plan.md:79-88`. | Spec routes at `docs/specs/v2.1/product-spec-v2.1.md:324-334`; code routes at `apps/web/src/features/client-portal/ClientPortal.tsx:2332-2351`; E2E covers client job submission, shortlist review, unlock request, disclosed detail, and feedback at `tests/e2e/pilot-business-flows.spec.ts:146-243`, `tests/e2e/pilot-business-flows.spec.ts:316-329`, and `tests/e2e/pilot-business-flows.spec.ts:366-397`. | Coverage is good through feedback, but not through post-feedback placement handoff. The plan should make the client-to-consultant closure handoff explicit in a Playwright step or manual checklist. |
| Candidate home, upload, profile AI review, follow-up, opportunities, consent, status | RC1 plan requires candidate consent at `docs/release/RC1-pilot-readiness-plan.md:83-84` and candidate participation in manual flow at `docs/release/RC1-pilot-readiness-plan.md:1108`. | Spec routes at `docs/specs/v2.1/product-spec-v2.1.md:342-348`; code routes at `apps/web/src/features/candidate-portal/CandidatePortal.tsx:1156-1221`; consent page shows profile version, consent text version, version match, and shared fields at `apps/web/src/features/candidate-portal/CandidatePortal.tsx:1033-1071`; E2E confirms consent at `tests/e2e/pilot-business-flows.spec.ts:331-353`. | Candidate consent is covered. Candidate upload/profile-review/follow-up/status are route-preserved but not RC1 browser gates. That is acceptable if RC1 pilot starts from consultant intake, but the report should mark those candidate surfaces as route/source evidence only. |
| Admin/System AI policy, task registry, industry packs, schema, workflow rules, permissions, audit log, integrations, security, and v2.1 governance pages | RC1 requires admin/owner audit trace at `docs/release/RC1-pilot-readiness-plan.md:91` and `docs/release/RC1-pilot-readiness-plan.md:1115`; hard criteria require owner/admin audit trace at `docs/release/RC1-pilot-readiness-plan.md:1307`. | Spec routes at `docs/specs/v2.1/product-spec-v2.1.md:356-375`; code sections at `apps/web/src/features/admin-portal/AdminPortal.tsx:39-61`; routes at `apps/web/src/features/admin-portal/AdminPortal.tsx:308-314`; seed login at `tests/e2e/pilot-seed-login.spec.ts:50-56`. | Admin route preservation exists, but no current Playwright test follows the S01-S08 transaction into `/admin/audit-log`, `/admin/claim-ledger`, or review-quality/eval surfaces. |
| v2.0 UI and portal preservation while adding v2.1 governance | RC1 invariants preserve v2.0 UI and portal definitions at `docs/release/RC1-pilot-readiness-plan.md:38-47`. | v2.1 explicitly preserves v2.0 UI and overlays governance at `docs/specs/v2.1/product-spec-v2.1.md:223-234`; v2.0 baseline preserves seven UI boards and unified Consultant portal at `docs/specs/v2.0/product-spec-v2.0.md:147-165`; route contract checks v2.0/v2.1 route strings at `apps/web/src/portalRouteContract.test.ts:7-99`. | Preservation is represented by source and route-string tests. It is not yet a render-level gate proving every preserved route resolves to an operator-usable screen with expected loading, empty, error, and next-action behavior. |

## Severity-ranked findings

### Finding 1: P0 - Current browser gates do not prove the RC1 frontend path through commercial closure and owner/admin audit trace

Severity: P0 for RC1 readiness claim.

Observed evidence:

- RC1 readiness requires placement and commission runtime/API plus persistence or audit evidence, and owner/admin audit trace evidence, before `ready-for-controlled-pilot`: `docs/release/RC1-pilot-readiness-plan.md:1292-1313`.
- The pilot business browser suite is named S01-S08 and ends after client feedback and consultant follow-up review: `tests/e2e/pilot-business-flows.spec.ts:99-397`.
- A targeted search found `placement`, `commission`, `owner`, and `admin` in `tests/e2e/pilot-business-flows.spec.ts` only as commercial terms strings, not as tested portal steps; owner/admin only appear in the seed-login spec: `tests/e2e/pilot-seed-login.spec.ts:42-56`.
- RC1-08A itself states current Task 42 browser flows prove S01-S08 through interview feedback but not placement, commission, owner revenue, or accounting handoff runtime behavior: `docs/release/RC1-pilot-readiness-plan.md:1026-1030`.
- Consultant placement and commission screens exist at `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:3067-3495`, and owner revenue/accounting exists at `apps/web/src/features/owner-portal/OwnerPortal.tsx:348-414`, but they are not exercised by current Playwright flow evidence.

Inference:

The RC1 plan covers the end-to-end workflow in narrative and manual-gate form, but the current frontend/browser evidence does not. Without an added browser/manual evidence gate for the late-stage screens, RC1 can at most claim automated pre-placement workflow coverage from the frontend.

Likely cause:

The existing Task 42 E2E suite was built around pre-placement pilot flow and later commercial-closure work was added as RC1-08A without adding matching frontend E2E scenarios.

Recommended change:

Add an RC1 browser gate, either in `tests/e2e/pilot-business-flows.spec.ts` as S09-S12 or in a new `tests/e2e/pilot-commercial-closure.spec.ts`, that continues from S08 and proves:

- Consultant records placement via `/consultant/placements`.
- Consultant advances placement through offer accepted, onboarded, invoice ready, invoice sent, and paid where the pilot data supports it.
- Consultant creates or verifies commission via `/consultant/commission` and exercises mark-paid/withhold rules.
- Owner verifies the same placement/commission in `/owner/placements`, `/owner/commission`, and `/owner/revenue`.
- Owner or admin opens audit/governance trace for the transaction IDs.

### Finding 2: P1 - Route preservation is checked as source-string containment, not render-level portal usability

Severity: P1.

Observed evidence:

- `apps/web/src/portalRouteContract.test.ts:1-5` imports portal source files with `?raw`.
- The same test checks that route strings are present with `toContain` or section-key containment: `apps/web/src/portalRouteContract.test.ts:7-99`.
- The app routes five portals in React Router at `apps/web/src/App.tsx:156-163`.
- The route contract does not render the routes, mock the required API states, assert screen headings, or check empty/error/next-step behavior.

Inference:

The current test is useful for preventing deletion of v2.0/v2.1 route strings, but it can pass even if a route renders a broken page, redirects unexpectedly, shows only a generic loading/error state, or lacks the operator guidance RC1 needs.

Likely cause:

The route contract was optimized for fast preservation checks after v2.0/v2.1 route expansion, not for pilot-readiness UX proof.

Recommended change:

Keep the source-string test as a preservation guard, but add a render-level route smoke test for every spec route category. At minimum, it should render each portal with mocked sessions/API responses and assert the expected route heading, no fallback redirect, and an explicit empty/error/next-step state.

### Finding 3: P1 - Consultant placement and commission screens require manual UUID handoff, which is fragile for pilot operators

Severity: P1.

Observed evidence:

- RC1 manual verification requires the operator to run placement, commission/revenue, and admin/owner audit trace: `docs/release/RC1-pilot-readiness-plan.md:1113-1115`.
- RC1 asks workers to record operator confusion by page, missing cue, expected next action, and whether backend is blocked or UI explanation is unclear: `docs/release/RC1-pilot-readiness-plan.md:1145-1154`.
- The Consultant placement form asks for raw Job ID, Candidate ID, and Company ID with placeholders "UUID from Jobs", "UUID from Talent Pool", and "UUID from Companies": `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:3219-3228`.
- The Consultant commission form asks for raw Placement ID with placeholder "UUID from Placements": `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:3405-3406`.
- Client feedback can create an "Open feedback workspace" handoff: `apps/web/src/features/client-portal/ClientPortal.tsx:1973-1990`, but there is no observed direct "create placement from this feedback/disclosed candidate" handoff in the inspected frontend code.

Inference:

A trained engineer can complete placement/commission by copying IDs from prior screens, but a pilot operator is likely to lose context or enter inconsistent IDs. That is an operator-readiness risk even if backend validation is correct.

Likely cause:

Commercial closure screens were added as functional administrative forms before the workflow handoff UX was completed.

Recommended change:

Add contextual placement CTAs from the post-feedback consultant follow-up or workflow state into `/consultant/placements` with job/candidate/company IDs prefilled. Add a commission CTA from the created placement row into `/consultant/commission` with placement ID prefilled. If prefill is not possible before RC1, the manual RC1 checklist must explicitly capture the exact source route for each ID and include a screenshot of the populated form before submission.

### Finding 4: P1 - Admin/owner governance surfaces are present, but not tied to the RC1 transaction trace in frontend gates

Severity: P1.

Observed evidence:

- RC1 transaction trace ledger is supposed to keep created IDs correlated through candidate, job, shortlist, consent, unlock, disclosure, feedback, placement, commission, and WorkflowEvent trace: `docs/release/RC1-pilot-readiness-plan.md:1132-1143`.
- Admin sections include audit and v2.1 governance pages: `apps/web/src/features/admin-portal/AdminPortal.tsx:39-61`.
- Admin route rendering is generic by section key: `apps/web/src/features/admin-portal/AdminPortal.tsx:308-314`.
- Owner governance sections render generic read-model rows and warn when no events return: `apps/web/src/features/owner-portal/OwnerPortal.tsx:419-460`.
- Consultant workflow can filter by entity type and entity id and preview legal next actions: `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:2978-3064`.

Inference:

The frontend has governance surfaces, but RC1 currently lacks a concrete gate that proves an owner/admin can trace the same transaction IDs from the pilot flow. The Consultant workflow filter is closer to the needed traceability UX than the owner/admin gates.

Likely cause:

The transaction trace ledger is defined in release docs, while frontend owner/admin audit pages expose broad governance read models rather than a transaction-scoped pilot audit path.

Recommended change:

Add a transaction-scoped audit step to the RC1 frontend gate. It should use IDs from the E2E flow and assert that at least one owner/admin or consultant workflow surface can filter to the transaction and display action code, entity, transition, risk tier, occurred timestamp, and reason.

### Finding 5: P2 - Empty/loading/error states exist, but several are too generic for pilot operator recovery

Severity: P2.

Observed evidence:

- Consultant API states distinguish unauthenticated, denied, invalid request, unavailable, and failed, but mostly pass through raw error detail: `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:433-470`.
- Client safe states provide client-safe default guidance and status labels: `apps/web/src/features/client-portal/ClientPortal.tsx:220-264`.
- Candidate state defaults to "Loading candidate data..." or the API error detail: `apps/web/src/features/candidate-portal/CandidatePortal.tsx:126-136`.
- Owner render states say "Loading owner data..." or "Owner API unavailable.": `apps/web/src/features/owner-portal/OwnerPortal.tsx:43-49`.
- Admin render states say "Loading admin governance data..." or "Admin API unavailable.": `apps/web/src/features/admin-portal/AdminPortal.tsx:86-92`.
- Shortlist send readiness is stronger: it gives can-send, title, detail, next action, and blocked items: `apps/web/src/features/consultant-portal/consultantPortalUtils.ts:53-92`, rendered at `apps/web/src/features/consultant-portal/ConsultantPortal.tsx:2746-2753`.

Inference:

Shortlist blocking UX is RC1-ready enough to guide an operator. Owner/admin/candidate generic error states are weaker; if a pilot operator hits a missing session, unavailable API, or empty audit read model, the UI may not tell them what to do next.

Likely cause:

The stronger safe-state pattern was implemented around shortlist and client privacy flows first, while owner/admin/candidate generic states were left as basic loading/error wrappers.

Recommended change:

Standardize RC1-safe states across portals with title, safe reason, next action, and recovery route. Add tests for empty/error states on owner revenue/audit, admin audit-log/claim-ledger, candidate consent, consultant placement, and consultant commission.

## Missing evidence / not verified

- Not verified: `npm --workspace @rto/web run test`.
- Not verified: `npm run typecheck:web`.
- Not verified: `npm run build:web`.
- Not verified: `npm run release:e2e:pilot`.
- Not verified: runtime browser screenshots or manual operator walkthrough.
- Not verified: live API responses, persisted rows, WorkflowEvent rows, or admin/owner audit rows for the same RC1 transaction.
- Not verified: accessibility, responsive/mobile rendering, visual regression, keyboard flow, or screen-reader behavior.
- Not verified: backend enforcement behind the inspected frontend routes, except where existing tests/source names imply integration.

## Suggested RC1 frontend/UX gate checklist

- Keep as required gate: `npm --workspace @rto/web run test`, because it includes route preservation, owner revenue/commission rendering, client shortlist utilities, session isolation, and API helper contracts.
- Keep as required gate: `npm run typecheck:web`.
- Keep as required gate: `npm run build:web`.
- Keep as required gate: `npm run release:e2e:pilot`, but do not treat it as complete RC1 readiness until commercial closure and audit trace are added.
- Add required Playwright gate: `pilot-commercial-closure.spec.ts` or S09-S12 in the existing pilot suite for consultant placement, consultant commission, owner placement/commission/revenue, and owner/admin audit trace.
- Add required Playwright gate: transaction-scoped audit lookup using IDs from the pilot flow.
- Add render-level component/route gate: every v2.0/v2.1 portal route renders its expected heading and a non-generic loading/empty/error state with next action.
- Add component tests for consultant placement/commission recovery states: missing fee agreement, missing expected fee, amount missing before mark-paid, finalized commission state, and no-action placement state.
- Add component tests for candidate consent version mismatch, no shared fields, declined/revoked consent, and missing consent request reference.
- Add component tests for admin/owner audit empty states that distinguish "no rows" from "instrumentation missing" and tell the operator what to do next.

## Top 5 improvement actions for this scope

1. Add S09-S12 Playwright coverage for placement, commission, owner revenue/accounting handoff, and owner/admin audit trace before allowing RC1 L5 readiness.
2. Convert route preservation from source-string-only to render-level smoke coverage for all preserved v2.0/v2.1 routes.
3. Add contextual handoffs and/or prefilled IDs from feedback/disclosure/workflow to consultant placement and from placement to commission.
4. Add transaction-scoped audit lookup to owner/admin or explicitly use consultant workflow as the RC1 audit trace surface with IDs from the ledger.
5. Standardize safe-state UX across owner, admin, candidate, consultant placement, and consultant commission so each blocker gives a safe reason and next action.
