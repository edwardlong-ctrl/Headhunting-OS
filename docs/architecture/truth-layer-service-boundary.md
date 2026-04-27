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
- Validates workflow action vocabulary and audit policy before append.
- Validates optional idempotency, correlation, and causation identifiers before append.
- Uses idempotency only to prevent accidental duplicate appends: duplicate equivalent payloads return the existing WorkflowEvent id, while duplicate different payloads are rejected as conflicts.
- Stores correlation id to group audit events for one business operation.
- Stores causation id to link an event to the prior event, request, or boundary that caused it; null remains valid for root events.
- Rejects unknown action codes and policy-unsafe audit requests.
- Does not validate transitions.
- Does not mutate entity state.
- Does not query or update the target entity.
- Does not expose broad workflow search/list/read-model behavior; the only lookup is idempotency-key scoped.
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
- Allowed boundary propagates idempotency/correlation/causation identifiers when the command supplies them.
- Canonical persistence is explicitly deferred.
- Does not write CandidateProfile.

## Transaction Boundary

- Current `CanonicalWriteTransactionBoundary` is skeleton/no JDBC rollback coordination.
- Future work must implement real transaction coordination before canonical multi-table writes.

## Forbidden Current Misreadings

- Do not treat `CanonicalWriteService` as CandidateProfile persistence.
- Do not treat `WorkflowEventService` as workflow engine.
- Do not treat WorkflowEvent action policy validation as transition legality validation.
- Do not treat WorkflowEvent idempotency/correlation/causation guardrails as a workflow engine, SLA engine, automation engine, API, or UI.
- Do not treat `ReviewEventService` as verification promotion.
- Do not treat `ClaimLedgerService` as canonical fact storage.
- Do not treat transaction boundary skeleton as real rollback.
- Do not expose raw Candidate to Client.
- Do not treat Task 4B as ConsentRecord, DisclosureRecord, RBAC/ABAC, Client-safe projection, AI model wiring, or CandidateProfile canonical persistence.
