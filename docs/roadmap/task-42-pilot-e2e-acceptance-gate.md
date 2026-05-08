# Task 42 Pilot E2E Acceptance Gate

## Gate Result

Current result: **NOT READY for Usable v1 / Controlled Commercial Pilot**.

This is an honest acceptance gate, not a new readiness claim. The system has
substantial backend/API/UI coverage across the pilot chain, but Task 42 cannot
call the product pilot-ready until the eight end-to-end pilot flows pass through
normal product workflows and the required operational validations have current
run evidence.

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

The gate returns `NOT_READY` unless every requirement is passed and backed by
explicit evidence. Partial regression coverage is not allowed to masquerade as
end-to-end pilot readiness.

## Eight Pilot Flow Status

| Flow | Current gate status | Evidence exists | Blocking gap |
| --- | --- | --- | --- |
| Consultant CV + note -> AI claims -> review -> canonical profile | Partial | Document upload, document intelligence, AI parser, governed canonical write tests | No single pilot E2E proves the full consultant CV-to-canonical path on seed data |
| Client/company JD -> AI job draft -> clarification -> consultant activation | Partial | Client command/query and job intake tests | JD file upload and AI job-draft extraction are not covered by a single pilot E2E |
| MatchReport -> evidence-backed explanation -> score cap | Partial | Match generation, matching controller, JDBC persistence tests | No browser E2E proves the matching surface against the pilot walkthrough |
| Anonymous shortlist -> client-safe preview | Partial | Shortlist builder, client command, client-safe card persistence/query tests | No browser E2E proves consultant send plus client preview as one pilot flow |
| Candidate opportunity/consent -> authorization | Partial | Candidate portal query, consent controller, consent workflow tests | No pilot E2E proves seeded candidate opportunity plus consent confirmation |
| Client shortlist review -> unlock request | Partial | Client command and unlock workflow tests | No browser E2E proves review, selection, and unlock request together |
| Consultant approve unlock -> DisclosureRecord -> identity disclosed | Partial | Consultant unlock, unlock workflow, disclosed candidate controller tests | No pilot E2E proves consultant approval and client identity read together |
| Client feedback -> outcome label -> suggested updates enter review | Partial | Client command, consultant review controller, feedback review tests | No E2E proves feedback, outcome loop, and consultant review queue on one pilot path |

## Negative Gate Status

Most negative gates have focused regression evidence today:

- Client raw Candidate/Profile access is denied.
- Anonymous client-safe card responses stay inside safe DTO boundaries.
- L4 identity disclosure requires consent and consultant approval.
- AI task execution cannot directly write canonical facts.
- AI cannot approve its own write-back.
- Bulk approve cannot produce `candidate_confirmed` or `external_verified`.
- Disclosure prerequisite checks exist and fail closed.
- Candidate portal access remains self-scoped.
- Admin governance surfaces do not bypass domain fact services.

The remaining partial negative gate is high re-identification risk on shortlist
send: redaction and shortlist seams have coverage, but there is no pilot E2E
showing unresolved high risk blocks an actual send flow.

## Current Validation Evidence

The Task 42 gate patch has current evidence for:

- `rtk git diff --check`
- `rtk npm run typecheck:web`
- `rtk npm run build:web`
- `rtk docker info`
- `PATH=/opt/homebrew/bin:$PATH rtk mvn -f services/core-api/pom.xml test`

Readiness remains blocked by missing current evidence for:

- Browser E2E tests for all eight pilot flows
- Task 38 pilot data rebuild / validate / export / guarded reset
- Backup / restore validation

The full Maven suite passed for this gate patch. That is still not sufficient to
claim pilot readiness because browser E2E, pilot CLI, and backup/restore evidence
are mandatory Task 42 gates.

## Next Work Required To Pass Task 42

1. Add a real browser E2E harness for the five portal surfaces, preferably
   Playwright, with pilot-seed login for Consultant, Client, Candidate, Owner,
   and Admin accounts.
2. Build eight E2E scenarios that run through product APIs/UI only; do not use
   seed shortcuts or direct database mutation to manufacture pass state.
3. Re-run Task 38 pilot data CLI rebuild / validate / export / guarded reset
   against PostgreSQL.
4. Execute the Task 39 backup / restore runbook and record evidence.
5. Run the full required validation chain and update this gate from `NOT_READY`
   only when all requirements are passed with evidence.
