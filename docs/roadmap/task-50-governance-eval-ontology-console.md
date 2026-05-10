# Task 50: Governance, Eval, and Ontology Production Console

Date: 2026-05-10

Branch: `main`

## Scope

Task 50 makes AI quality, model routing, ontology drift, cost/latency,
redaction, review quality, and AI resume authenticity risk visible as
Admin/Owner product surfaces.

The implementation is a read-only governance console. It aggregates existing
signals from the AI task registry/run tables, observability, Task 47 industry
pack calibration, Task 54 cost policies, privacy redaction risk assessments,
review events, claim ledger rows, and match-report authenticity risk fields. It
does not mutate canonical facts, switch live providers, activate real providers,
or bypass disclosure, redaction, review, domain-service, or canonical-write
policy.

## Delivered Sections

- Admin `eval-dashboard`: failed AI task runs, schema-shaped failures,
  hallucination-prone claims, required human review counts, and safe failure
  rows.
- Admin `negative-cases`: deterministic auditable negative fixtures generated
  from AI task definitions, ontology drift/negative cases, and redaction risk
  patterns. No live LLM call is made.
- Admin `review-quality`: low-quality review patterns, bulk acknowledgement
  vs verified distinction, failed-audit rows, and superseded/reopened review
  patterns.
- Admin `model-routing`: configured task route inspection, model/provider
  configuration status, route overrides, and fail-closed provider config state.
- Admin `cost-latency`: Task 54 budget rows over completed `AITaskRun`
  observations, preserving `WATCH`, `CRITICAL`, and `EVIDENCE_MISSING` instead
  of hiding missing data behind fake zeroes.
- Admin `ontology-drift`: Task 47 industry-pack review queue, stale review
  deadlines, drift-signal counts, and pack review items.
- Admin `redaction-incidents`: privacy redaction and re-identification
  assessments, high/critical risks, workflow linkage, and blocked/constrained
  decisions without raw Candidate/Profile identifiers.
- Admin `ai-resume-authenticity-risk`: deterministic authenticity-risk
  surfacing from match reports and recorded authenticity-risk-assessor outputs.
  This is not a claimed ML resume-fraud detector.
- Owner `ai-quality`: a narrower summary of AI failures, hallucination risk,
  stale ontology warnings, privacy incidents, low-quality reviews, and high
  authenticity risk with safe admin follow-up pointers.

## Backend/API Changes

- Added `governanceconsole.GovernanceConsoleReadService` as the Task 50
  aggregation service.
- Added `observability.PerformanceCostDashboardPolicy` as a public dashboard
  wrapper over the existing Task 54 budget policy semantics.
- Extended Admin governance endpoints under `/api/admin`:
  - `/eval-dashboard`
  - `/negative-cases`
  - `/review-quality`
  - `/model-routing`
  - `/cost-latency`
  - `/ontology-drift`
  - `/redaction-incidents`
  - `/ai-resume-authenticity-risk`
- Extended Owner `/api/owner/ai-quality` to use the Task 50 summary when the
  console read service is wired.
- Kept responses inside the existing governance DTO allowlist:
  `GovernanceSectionResponse`, `GovernanceMetricResponse`, and
  `GovernanceItemResponse`.

## Frontend Changes

- Extended `AdminPortal.tsx` with Task 50 navigation and routes:
  `eval-dashboard`, `negative-cases`, `ontology-drift`,
  `redaction-incidents`, `cost-latency`, and
  `ai-resume-authenticity-risk`.
- Made Admin default routing land on `eval-dashboard`.
- Kept the existing governance section renderer and editable-config behavior;
  Task 50 sections are read-only.
- Updated Owner/Admin empty states so missing instrumentation is not presented
  as a successful zero.
- Extended the portal route contract test with the new Task 50 Admin routes.

## Policy Boundaries

- Backend remains the source of truth.
- PostgreSQL remains the target source of truth for persisted signals.
- AI outputs remain claims, not facts.
- Task 50 does not add canonical fact writes or workflow mutations.
- Tenant filtering is enforced in read queries and controller policy tests.
- Client/candidate raw data is not exposed in governance console responses.
- Model routing is inspection-only except for the pre-existing governed config
  boundary.
- Cost units remain provider-neutral Task 54 units, not real provider billing.
- Authenticity risk uses existing deterministic/eval/review signals only.
- Task 58 release gate and Task 60 final acceptance gate remain out of scope.

## Tests Added

- `GovernanceConsoleReadServicePostgresIntegrationTest`
  - loads all eight Task 50 Admin sections from a real migrated PostgreSQL
    schema;
  - proves eval failures, schema risks, hallucination-risk claims, deterministic
    negative cases, cost `WATCH`, cost `EVIDENCE_MISSING`, redaction incidents,
    authenticity risk, and Owner summary behavior;
  - proves cross-organization filtering and no raw candidate id leakage in the
    Task 50 console text.
- `AdminGovernanceControllerMappingTest`
  - proves Task 50 Admin routes use the focused console read service and Admin
    governance permission boundary.
- `OwnerGovernanceControllerPolicyTest`
  - proves Owner `ai-quality` uses the Task 50 summary while keeping Owner
    access policy.
- `portalRouteContract.test.ts`
  - proves the new Admin route keys are present in the frontend route contract.

## Validation Evidence

Focused TDD red evidence:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=GovernanceConsoleReadServicePostgresIntegrationTest,AdminGovernanceControllerMappingTest,OwnerGovernanceControllerPolicyTest test
rtk npm --workspace @rto/web run test -- portalRouteContract.test.ts
```

Result: backend failed at compile because the Task 50 console service/policy
did not exist; frontend failed because `AdminPortal.tsx` did not yet contain
`eval-dashboard`.

Focused green evidence before full closeout:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=GovernanceConsoleReadServicePostgresIntegrationTest test
rtk mvn -f services/core-api/pom.xml -Dtest=AdminGovernanceControllerMappingTest,OwnerGovernanceControllerPolicyTest,GovernanceReadServiceTest,PerformanceCostBudgetPolicyTest test
rtk npm --workspace @rto/web run test -- portalRouteContract.test.ts
```

Result: the focused Task 50 integration test passed with 14 tests, controller
mapping/policy and existing governance/cost policy tests passed with 15 tests,
and the portal route contract passed with 5 tests.

Final local closeout evidence:

```bash
rtk git diff --check
rtk docker info
rtk mvn -f services/core-api/pom.xml -Dtest=GovernanceConsoleReadServicePostgresIntegrationTest,AdminGovernanceControllerMappingTest,OwnerGovernanceControllerPolicyTest test
rtk mvn -f services/core-api/pom.xml -Dtest=AdminGovernanceControllerMappingTest,OwnerGovernanceControllerPolicyTest,GovernanceReadServiceTest,GovernanceReadServicePostgresIntegrationTest,ObservabilityReadServiceTest,AITaskRunnerServiceTest,JdbcIndustryPackReadPortIntegrationTest,RedactionAuditPostgresIntegrationTest test
rtk mvn -f services/core-api/pom.xml test
rtk npm --workspace @rto/web run test
rtk npm run typecheck:web
rtk npm run build:web
```

Results: whitespace check passed; Docker was reachable; Task 50 targeted
backend set passed with 20 tests; broader backend acceptance subset passed with
36 tests; full Maven passed with 1160 tests, 0 failures, 0 errors, and 3
skipped; web Vitest passed with 9 files and 38 tests; TypeScript checking and
web production build passed.

## Remaining Gaps

- Task 50 does not add live provider activation or live provider health
  governance beyond available config/status inspection.
- Task 50 does not add an ML resume-fraud detector.
- Task 50 does not add Admin editing UI for ontology packs.
- Task 50 does not add external BI/legal/accounting systems.
- Task 58 release management and Task 60 final acceptance remain separate
  production-readiness gates.
