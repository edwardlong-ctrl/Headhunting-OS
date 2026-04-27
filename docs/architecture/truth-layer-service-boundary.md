# Truth Layer Service Boundary

## Current Boundary Chain

`ClaimLedgerService`
→ `ReviewEventService`
→ `CanonicalWriteGate`
→ `CanonicalWriteService`
→ `WorkflowEventService`

## ClaimLedgerService

- Appends claims.
- Does not write canonical facts.
- Uses `governance.claim_ledger_item`.

## ReviewEventService

- Appends human review events.
- Records `bulk_flag`, `risk_tier`, `decision`, `reason`.
- Does not promote facts.
- Does not mutate ClaimLedger verification status.
- Uses `governance.review_event`.

## WorkflowEventService

- Appends audit/workflow events.
- Does not validate transitions.
- Does not mutate entity state.
- Uses `workflow.workflow_event`.

## CanonicalWriteGate

- Pure domain gate.
- Blocks unsafe `system_inference`, weak/implied intent, conflicting claims, forbidden/internal-only client visibility, unsafe T3/T4 paths.
- Bulk approve cannot become `candidate_confirmed` or `external_verified`.

## CanonicalWriteService

- Gate-first boundary.
- Requires claim snapshot and review evidence.
- Does not bypass `CanonicalWriteGate`.
- Allowed boundary appends a `WorkflowEvent`.
- Canonical persistence is explicitly deferred.
- Does not write CandidateProfile.

## Transaction Boundary

- Current `CanonicalWriteTransactionBoundary` is skeleton/no JDBC rollback coordination.
- Future work must implement real transaction coordination before canonical multi-table writes.

## Forbidden Current Misreadings

- Do not treat `CanonicalWriteService` as CandidateProfile persistence.
- Do not treat `WorkflowEventService` as workflow engine.
- Do not treat `ReviewEventService` as verification promotion.
- Do not treat `ClaimLedgerService` as canonical fact storage.
- Do not treat transaction boundary skeleton as real rollback.
- Do not expose raw Candidate to Client.
