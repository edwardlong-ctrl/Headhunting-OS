# Truth Layer Service Boundary

## Current Boundary Chain

`ClaimLedgerService`
→ `ReviewEventService`
→ `CanonicalWriteGate`
→ `CanonicalWriteService`
→ `WorkflowEventService`

Transition audit side:

`WorkflowTransitionAuditService`
→ `WorkflowEventService`

Read-only audit side:

`WorkflowAuditQueryService`
→ `WorkflowAuditReadPort`
→ `workflow.workflow_event`

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
- Does not expose broad workflow search/list/read-model behavior; append idempotency lookup remains idempotency-key scoped.
- Task 4C audit read-model behavior is separated into `WorkflowAuditQueryService` and `WorkflowAuditReadPort`.
- Uses `workflow.workflow_event`.

## WorkflowTransitionAuditService

- Backend-internal service boundary for recording requested workflow state-transition audit events.
- Validates `WorkflowTransitionAuditRequest` syntax and policy shape before append.
- Requires organization id, entity namespace/type/id, action code, actor type/id, AI involvement, source type, occurred-at timestamp, `before_state`, and `after_state`.
- Rejects equal `before_state` and `after_state`.
- Rejects unknown action codes.
- Rejects append-only audit actions that are not configured as `WorkflowActionPolicy.stateTransition`.
- Uses existing `WorkflowActionRegistry` policy validation for reason and T3/T4 human final actor requirements.
- Maps to the existing `WorkflowEventService.append` boundary so idempotency, correlation, and causation behavior remain unchanged.
- Does not validate legal `from_state -> to_state` paths.
- Does not implement a workflow engine or state machine.
- Does not query, join, update, or mutate Job, Candidate, Shortlist, Consent, Disclosure, Placement, Commission, ClaimLedger, ReviewEvent, or AITaskRun records.
- Does not expose API/controller/UI behavior.

## WorkflowAuditQueryService

- Backend-internal read-only service for appended `WorkflowEvent` audit records.
- Requires `organization_id` for every query.
- Validates limit, offset, and occurred-at range.
- Supports narrow audit filters only: event id, entity type/id, action code, actor type/id, correlation id, causation id, idempotency key, and occurred-at range.
- Returns deterministic ordering: `occurred_at DESC`, then `workflow_event_id DESC`.
- Does not append, update, delete, or mutate any state.
- Does not validate transition legality.
- Does not authorize users yet; RBAC/ABAC remains future work.
- Does not expose API/controller/UI behavior.
- Does not produce client-safe projections.

## WorkflowAuditReadPort / JdbcWorkflowAuditReadPort

- Read-only port and JDBC adapter for the `workflow.workflow_event` audit read model.
- Reads only from `workflow.workflow_event`.
- Does not replace `WorkflowEventPort`.
- Does not join Candidate, Company, Job, Consent, Disclosure, Placement, or Commission tables.
- Does not expose raw Candidate/Profile payloads, source document contents, metadata JSON, or business entity internals.
- Does not implement full-text search, arbitrary SQL filters, generic sorting, dashboard analytics, or broad repository behavior.
- Uses existing Task 4B columns and indexes for idempotency, correlation, causation, entity timeline, and event id lookup.

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
- Do not treat `WorkflowTransitionAuditService` as a workflow engine, state machine, SLA engine, automation engine, entity mutator, entity repository, API, or UI.
- Do not treat Task 4D transition audit validation as legal `from_state -> to_state` validation.
- Do not treat WorkflowEvent action policy validation as transition legality validation.
- Do not treat WorkflowEvent idempotency/correlation/causation guardrails as a workflow engine, SLA engine, automation engine, API, or UI.
- Do not treat Task 4C audit read model as API, UI, client-safe projection, RBAC/ABAC, dashboard analytics, or workflow engine.
- Do not treat `ReviewEventService` as verification promotion.
- Do not treat `ClaimLedgerService` as canonical fact storage.
- Do not treat transaction boundary skeleton as real rollback.
- Do not expose raw Candidate to Client.
- Do not treat Task 4C as ConsentRecord, DisclosureRecord, RBAC/ABAC, Client-safe projection, AI model wiring, or CandidateProfile canonical persistence.
