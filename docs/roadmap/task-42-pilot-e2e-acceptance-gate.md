# Task 42 Pilot E2E Acceptance Gate

## Gate Result

Current result: **CONTROLLED PILOT READY for the Task 42 Usable v1 gate**.

This is an honest acceptance gate, not a new readiness claim. The system has
substantial backend/API/UI coverage across the pilot chain, and Task 42 now has
current evidence for the eight required end-to-end pilot flows plus the required
operational validations. This does not make the product public-SaaS-ready or
production-certified; it closes the controlled pilot acceptance gate for the
current v2.1 scope.

## Implemented Gate Artifact

The backend now includes a deterministic gate model:

- `PilotAcceptanceGate.task42Baseline()`
- `PilotAcceptanceReport`
- `PilotAcceptanceRequirement`
- `PilotAcceptanceCategory`
- `PilotAcceptanceRequirementStatus`

`PilotAcceptanceGateTest` locks the Task 42 contract to:

- 8 pilot acceptance flows
- 10 negative privacy / permission / canonical-write / AI-boundary gates
- 8 validation commands or operational evidence gates

The gate returns `CONTROLLED_PILOT_READY` only when every requirement is passed
and backed by explicit evidence. Partial regression coverage is not allowed to
masquerade as end-to-end pilot readiness.

## Current Browser E2E Harness

Task 42 now has a real Playwright harness at `tests/e2e` and a root command:

- `rtk npm run test:e2e:pilot`

The current suite proves that the five Task 38 seed accounts can sign in through
the real portal UI against a live Spring Boot API and Vite web app:

- Consultant: `/consultant`
- Client: `/client`
- Candidate: `/candidate`
- Owner: `/owner`
- Admin: `/admin`

The same harness now also contains the serial S01-S08 business-flow suite in
`tests/e2e/pilot-business-flows.spec.ts`.

## Eight Pilot Flow Status

| Flow | Current gate status | Evidence exists | Blocking gap |
| --- | --- | --- | --- |
| Consultant CV + note -> AI claims -> review -> canonical profile | Passed | `tests/e2e/pilot-business-flows.spec.ts` S01 plus governed intake regressions | None for Task 42 |
| Client/company JD -> AI job draft -> clarification -> consultant activation | Passed | S02 plus client command/query and job intake regressions | None for Task 42 |
| MatchReport -> evidence-backed explanation -> score cap | Passed | S03 plus match generation/controller/JDBC regressions | None for Task 42 |
| Anonymous shortlist -> client-safe preview | Passed | S04 plus shortlist/client-safe persistence regressions | None for Task 42 |
| Candidate opportunity/consent -> authorization | Passed | S05-S06 plus candidate consent regressions | None for Task 42 |
| Client shortlist review -> unlock request | Passed | S05 plus client command and unlock workflow regressions | None for Task 42 |
| Consultant approve unlock -> DisclosureRecord -> identity disclosed | Passed | S06-S07 plus consultant unlock and disclosed candidate regressions | None for Task 42 |
| Client feedback -> outcome label -> suggested updates enter review | Passed | S08 plus feedback outcome-loop regressions | None for Task 42 |

## Negative Gate Status

The negative gates have focused regression evidence today:

- Client raw Candidate/Profile access is denied.
- Anonymous client-safe card responses stay inside safe DTO boundaries.
- L4 identity disclosure requires consent and consultant approval.
- AI task execution cannot directly write canonical facts.
- AI cannot approve its own write-back.
- Bulk approve cannot produce `candidate_confirmed` or `external_verified`.
- Disclosure prerequisite checks exist and fail closed.
- Candidate portal access remains self-scoped.
- Admin governance surfaces do not bypass domain fact services.
- High re-identification risk blocks shortlist send through
  `ShortlistBuilderServiceTest#sendToClientFailsWhenIncludedCardHasHighReidentificationRisk`
  and the redaction/re-identification regression suite.

## Current Validation Evidence

The Task 42 gate patch has current evidence for:

- `rtk git diff --check`
- `rtk npm run typecheck:web`
- `rtk npm run build:web`
- `rtk docker info`
- `PATH=/opt/homebrew/bin:$PATH rtk mvn -f services/core-api/pom.xml test`
- `rtk npm run pilot:data:rebuild`
- `rtk npm run pilot:data:validate`
- `rtk npm run pilot:data:export`
- `RTO_PILOT_DATA_ALLOW_RESET=true rtk npm run pilot:data:reset`
- `RTO_E2E_API_PORT=8093 RTO_E2E_WEB_PORT=4193 rtk npm run test:e2e:pilot`
- `artifacts/task42-backup-restore-20260509/evidence.md`

The backup/restore evidence includes both a post-E2E business-state restore with
API health and document availability, and a clean-seed database restore whose
`pilot:data:validate` check returned exit code 0.

## Remaining Work After Task 42

Task 42 does not certify public production operation. The next roadmap work
should continue Tasks 43-60: production operations, broader security hardening,
managed deployment, richer portal coverage, support workflows, and non-pilot
industry/product depth.
