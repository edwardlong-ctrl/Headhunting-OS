# Backend/Data/Governance Evidence Index For RC1 Pilot Readiness

Date: 2026-06-08
Scope: bounded component audit for `docs/release/RC1-pilot-readiness-plan.md`
Execution: read-only except this report file; tests not run.

## Evidence Base

Read:
- `docs/release/RC1-pilot-readiness-plan.md`
- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`

Bounded searches only:
- `services/core-api`
- `scripts/release`
- `docs/release`
- `packages/contracts`

Terms searched: `PilotAcceptanceGate`, `ClaimLedger`, `AITaskRun`, `WorkflowEvent`, `Consent`, `Disclosure`, `Placement`, `Commission`, `revenue`, `accounting`, `Flyway`, `migration`, `pilot data`.

## What The RC1 Plan Covers Well

- It correctly makes RC1 gate-driven rather than feature-driven, with a single success exit of `ready-for-controlled-pilot` and a full transaction chain from intake through placement, commission/revenue, and admin/owner audit trace (`docs/release/RC1-pilot-readiness-plan.md:5`, `docs/release/RC1-pilot-readiness-plan.md:15`, `docs/release/RC1-pilot-readiness-plan.md:17`).
- It preserves the core backend/data invariants from the current spec: AI outputs claims, backend owns truth, PostgreSQL is the truth source, every key transition creates `WorkflowEvent`, and clients cannot read raw `Candidate` before unlock/disclosure (`docs/specs/CURRENT_SPEC.md:13`, `docs/specs/CURRENT_SPEC.md:15`, `docs/specs/CURRENT_SPEC.md:17`, `docs/specs/CURRENT_SPEC.md:19`, `docs/specs/CURRENT_SPEC.md:21`; mirrored in `docs/release/RC1-pilot-readiness-plan.md:38-47`).
- It has a clear evidence strength model and explicitly says P0 transaction steps require runtime/API evidence plus persistence or workflow/audit evidence; screenshots alone are insufficient for canonical write, consent, unlock/disclosure, placement, commission, or governance/audit claims (`docs/release/RC1-pilot-readiness-plan.md:108-121`, `docs/release/RC1-pilot-readiness-plan.md:127-142`).
- It separately gates migration application and avoids treating filename validation as PostgreSQL proof. The release migration script checks Flyway filenames/order, then runs `TruthLayerPostgresMigrationIntegrationTest` (`scripts/release/validate-migrations.sh:7-48`), and that test applies all migrations to real PostgreSQL through Flyway/Testcontainers (`services/core-api/src/test/java/com/recruitingtransactionos/coreapi/truthlayer/TruthLayerPostgresMigrationIntegrationTest.java:76-91`).
- It covers pilot-data safety better than most release plans: default isolated synthetic data, no production data, explicit refusal to rebuild against the default local DB without confirmation, and an isolated container path for RC1-03A (`docs/release/RC1-pilot-readiness-plan.md:26`, `docs/release/RC1-pilot-readiness-plan.md:626-641`, `docs/release/RC1-pilot-readiness-plan.md:643-690`).
- It acknowledges the exact commercial-closure gap: Task 42 browser flows stop before placement/commission, and RC1 cannot succeed without current runtime/API plus persistence/audit evidence for placement and commission (`docs/release/RC1-pilot-readiness-plan.md:1017-1030`, `docs/release/RC1-pilot-readiness-plan.md:1053-1080`).
- It has concrete backend surfaces to map against: placement and commission persistence tables exist (`services/core-api/src/main/resources/db/migration/V10__create_product_data_model_completion.sql:480-560`), consultant placement/commission APIs exist (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantPlacementController.java:45-106`, `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantCommissionController.java:45-81`), owner revenue/accounting APIs exist (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueController.java:36-48`), and admin observability can query workflow events and AI task runs (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/admin/AdminObservabilityController.java:73-107`, `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/admin/AdminObservabilityController.java:141-171`).

## What Is Still Under-Specified Or Risky

### P0 - Manual golden-path evidence wording can under-prove non-commercial P0 steps

Observed evidence:
- The plan's global rule says P0 transaction steps require runtime/API plus persistence or workflow/audit evidence, and names canonical write, consent, unlock/disclosure, placement, commission, and governance/audit as screenshot-insufficient (`docs/release/RC1-pilot-readiness-plan.md:120-121`).
- RC1-09 then says each manual step can record "at least one of" screenshot, API response, log, DB row/query, WorkflowEvent/audit, or E2E scenario evidence (`docs/release/RC1-pilot-readiness-plan.md:1117-1126`).
- The stricter exception in RC1-09 names only placement, commission, and audit trace (`docs/release/RC1-pilot-readiness-plan.md:1130`), while the transaction ledger also requires AI task, claim ledger, canonical write, consent, unlock, disclosure, placement, commission, and WorkflowEvent IDs (`docs/release/RC1-pilot-readiness-plan.md:1132-1141`).

Inference:
- A worker following RC1-09 literally could mark canonical write, candidate consent, or unlock/disclosure as evidenced with a screenshot/API summary only, even though those are backend-owned truth or governance states under the source of truth (`docs/specs/CURRENT_SPEC.md:13-21`; `docs/specs/v2.1/product-spec-v2.1.md:889-895`, `docs/specs/v2.1/product-spec-v2.1.md:907-913`).

Recommendation:
- Amend the RC1 checklist so canonical write, consent, unlock/disclosure, placement, commission, and governance/audit all require runtime/API evidence plus DB row and/or `WorkflowEvent`/audit evidence tied to the same transaction IDs.

### P1 - Commercial closure has implementation surfaces, but the plan does not prescribe exact correlation queries

Observed evidence:
- RC1-08A requires mapping placement/commission implementation and says `passed` requires runtime/API plus persistence/audit evidence (`docs/release/RC1-pilot-readiness-plan.md:1042-1080`).
- Backend APIs exist for consultant placement creation/status transitions (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantPlacementController.java:55-106`), consultant commission creation/payment/withholding (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantCommissionController.java:55-81`), and owner revenue/accounting export (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueController.java:36-48`).
- Placement and commission service transitions append workflow audit events (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/service/PlacementWorkflowService.java:97-104`, `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/service/PlacementWorkflowService.java:386-406`; `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/service/CommissionWorkflowService.java:81-88`, `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/service/CommissionWorkflowService.java:216-232`).
- Admin observability can query workflow events by entity/action/correlation filters (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/admin/AdminObservabilityController.java:73-107`; `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/observability/ObservabilityReadService.java:88-96`).

Inference:
- The plan identifies the needed evidence type but does not give a concrete backend evidence recipe: placement row, commission row, owner revenue/accounting response, and workflow-event query all correlated by the same transaction ledger IDs. Without that recipe, RC1 evidence could become a collection of plausible but disconnected source/test/API snippets.

Recommendation:
- Add a commercial-closure evidence table with required endpoints and correlation checks:
  - `POST /api/consultant/placements`
  - placement status transition endpoint used
  - `POST /api/consultant/commissions`
  - `POST /api/consultant/commissions/{id}/mark-paid` or explicit not-paid state
  - `GET /api/owner/revenue`
  - `GET /api/owner/revenue/accounting-export`
  - `GET /api/admin/observability/workflow-events?entityType=placement|commission&entityId=...`
  - persisted `recruiting.placement` and `recruiting.commission` rows for the same IDs.

### P1 - Historical PilotAcceptanceGate must remain evidence index input, not pass evidence

Observed evidence:
- `PilotAcceptanceGate.task42Baseline()` is explicitly a Task 42 baseline and marks many requirements as already passed (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotacceptance/PilotAcceptanceGate.java:11-18`, `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotacceptance/PilotAcceptanceGate.java:149-173`).
- It includes pilot data validation commands as historical acceptance material (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/pilotacceptance/PilotAcceptanceGate.java:180-187`).
- The RC1 plan correctly warns that `PilotAcceptanceGate` can guide re-checks but is not current RC1 pass evidence unless command/runtime/persistence/audit proof is collected in this RC1 run (`docs/release/RC1-pilot-readiness-plan.md:1175-1179`).

Inference:
- The plan covers this, but it remains a high-risk evidence-index trap because the class name sounds like a current gate while its contents are historical and pre-commercial-closure.

Recommendation:
- In the backend/data checklist, require every `PilotAcceptanceGate` reference to be labeled `historical-baseline-only` unless paired with current RC1 run evidence.

### P2 - Contract schemas are useful invariants but not current implementation proof

Observed evidence:
- `truth-layer.rules.schema.json` says AI output must enter `ClaimLedgerItem`, `WorkflowEvent` is required, and `AITaskRun` records AI actions, but also says this is a Task 2B rule carrier and `implemented_behavior` is `false` (`packages/contracts/schemas/truth-layer.rules.schema.json:6-13`).
- `ClaimLedgerItem`, `AITaskRun`, and `WorkflowEvent` schemas define the desired data contracts and comments, but schemas alone do not prove runtime enforcement (`packages/contracts/schemas/claim-ledger-item.schema.json:4-20`, `packages/contracts/schemas/ai-task-run.schema.json:4-43`, `packages/contracts/schemas/workflow-event.schema.json:4-24`).

Inference:
- These schemas are good checklist anchors, but an RC1 evidence index should not use them as readiness evidence without service, test, API, or persistence proof.

Recommendation:
- Classify contract-schema evidence as E0/source-contract only. Pair it with current backend tests, API output, or persisted rows before marking any capability `implemented`.

### P2 - Migration count is currently exact but brittle for future RC1 reruns

Observed evidence:
- The migration apply test asserts exactly 34 executed migrations (`services/core-api/src/test/java/com/recruitingtransactionos/coreapi/truthlayer/TruthLayerPostgresMigrationIntegrationTest.java:85-91`).
- The release script separately validates filename contiguity from the migration directory (`scripts/release/validate-migrations.sh:12-44`).

Inference:
- This is acceptable for the current checkout, but adding a migration requires updating the hard-coded test expectation or RC1 migration validation will fail for bookkeeping reasons before product risk is evaluated.

Recommendation:
- Keep the exact count if intentional, but add a note to the migration gate checklist: "When adding migrations, update `TruthLayerPostgresMigrationIntegrationTest` expected versions and required table/index lists in the same patch."

## Suggested Backend/Data Gate Checklist

1. Migration gate:
   - Run `rtk npm run release:migrations`.
   - Evidence must show filename contiguity and Testcontainers PostgreSQL Flyway application.
   - Record migration count and applied version list.

2. Pilot data gate:
   - Prove datasource shape before mutation.
   - Prefer isolated container path.
   - Record `pilot:data:rebuild` and `pilot:data:validate` results without URL, username, password, or real data.

3. Truth-layer gate:
   - For AI draft claims, record `AITaskRun` ID, `ClaimLedgerItem` ID, review event if present, and canonical write target.
   - For canonical writes, require persisted canonical field evidence plus `WorkflowEvent` or canonical-write attempt evidence.

4. Consent/disclosure gate:
   - Record consent record ID, unlock request ID, disclosure record ID, prerequisite decision, and related `WorkflowEvent`/audit evidence.
   - Do not accept screenshot-only evidence.

5. Placement/commission gate:
   - Create or locate one placement through backend/API runtime.
   - Create or locate one commission for that placement.
   - Record persisted placement and commission rows or safe API summaries.
   - Record `WorkflowEvent` evidence for placement and commission entity IDs.
   - Record owner revenue summary and accounting handoff response.

6. Governance/audit gate:
   - Query admin observability for the same transaction ledger IDs.
   - Require workflow-event trace IDs for canonical write, consent/disclosure, placement, and commission.
   - Require AI task run trace for AI-assisted steps.

7. Evidence classification:
   - Treat specs, schemas, `PilotAcceptanceGate`, and source code as E0/E1 mapping only unless paired with current command/runtime/persistence/audit proof.
   - Mark service-test-only commercial closure as `partial`, not RC1-ready.
   - Mark any skipped or waived gate as not sufficient for `ready-for-controlled-pilot` unless RC1-11 explicitly allows it; current RC1-11 does not allow required gates to be only waived.

## Blockers In This Audit

- Tests were not run by user instruction.
- Browser/runtime/API behavior was not exercised by user instruction.
- No broad inspection outside the bounded search paths was performed.
