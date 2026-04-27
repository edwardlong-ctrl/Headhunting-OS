# Implementation Status

## Current Git Main Milestones

- `beb71d4` Add product specs as source of truth: established `docs/specs/CURRENT_SPEC.md`, v2.1 as current product source of truth, and preserved v2.0 as UI / portal baseline.
- `f1b2e0a` Initialize production kernel skeleton: created the first production-kernel repo skeleton for the production-first Recruiting Transaction OS.
- `45f979a` Document contracts-first truth layer design: added the Task 2A contracts-first truth-layer design documents.
- `1aca18b` Add truth layer contract skeleton: introduced the initial truth-layer Java/domain contract skeleton.
- `09571ee` Verify truth layer migrations against PostgreSQL: verified Flyway/PostgreSQL truth-layer migration behavior.
- `22bdf98` Add truth layer canonical write gate skeleton: introduced the first CanonicalWriteGate boundary.
- `fc18e0e` Add truth layer alignment tests: checked alignment across domain contracts, documentation, and migration shape.
- `4d90e2c` Add truth layer negative policy tests: added negative policy coverage for unsafe truth-layer behavior.
- `8a8b670` Add truth layer persistence port contracts: added minimal append-oriented persistence port contracts.
- `1ffc5d5` Implement claim ledger append persistence: implemented append persistence for ClaimLedger records.
- `53d6469` Implement review event append persistence: implemented append persistence for ReviewEvent records.
- `eac26cd` Implement workflow event append persistence: implemented append persistence for WorkflowEvent records.
- `9f6e097` Add canonical write transaction boundary: added the CanonicalWriteTransactionBoundary skeleton.
- `e55069c` Harden truth layer service boundaries: hardened service boundaries and regression coverage through Task 3E.
- Task 4A: added stable workflow action/entity/risk/actor/AI involvement vocabulary, a workflow audit policy registry, and append-boundary policy validation for `WorkflowEventService`.
- Task 4B: added `WorkflowEvent` idempotency, correlation, and causation guardrails at the audit append boundary.
- Task 4C: added a backend-internal, read-only `WorkflowEvent` audit query/read model skeleton.
- Task 4D current worktree: adds a backend-internal `WorkflowTransitionAuditService` / `WorkflowTransitionAuditRequest` skeleton for recording requested workflow state-transition audit events with `before_state` and `after_state`.

## Current Test State

- Full Maven backend reached 119 tests, 0 failures/errors, 1 existing skip after Task 4A.
- Full Maven backend reached 131 tests, 0 failures/errors, 1 existing skip after Task 4B.
- Task 4B added focused unit and PostgreSQL/Testcontainers coverage for idempotency/correlation/causation behavior.
- Task 4C adds focused unit and PostgreSQL/Testcontainers coverage for read-only audit query behavior and boundaries.
- Task 4D adds focused unit and PostgreSQL/Testcontainers coverage for transition audit request validation, transition-action classification, idempotency/correlation/causation propagation, persistence, read-model visibility, and organization isolation.
- Docker/Testcontainers PostgreSQL is part of required validation.
- `docker info` must pass before full Maven validation.
- Maven command:

```sh
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

## Current Truth Layer Capabilities

- `ClaimLedgerService` appends to `governance.claim_ledger_item`.
- `ReviewEventService` appends to `governance.review_event`.
- `WorkflowEventService` appends to `workflow.workflow_event`.
- `WorkflowEventService` validates known workflow action vocabulary and audit policy before append.
- `WorkflowEventService` validates idempotency, correlation, and causation identifiers before append.
- `WorkflowEventService` returns the existing event for duplicate equivalent idempotency-key appends and rejects duplicate different payloads as idempotency conflicts.
- `WorkflowActionRegistry` defines one policy per stable action code, including allowed entity types, risk tier, before/after-state requirements, reason requirements, and AI-only finalization limits.
- `WorkflowAuditQueryService` provides a backend-internal, read-only audit read-model boundary for `workflow.workflow_event` records.
- `WorkflowAuditReadPort` supports narrow audit filters by organization, event id, entity, action, actor, correlation, causation, idempotency key, and occurred-at range.
- `JdbcWorkflowAuditReadPort` reads only from `workflow.workflow_event`; it does not join Candidate, Company, Job, Consent, Disclosure, Placement, or Commission tables.
- `WorkflowTransitionAuditService` provides a backend-internal transition audit boundary that records requested state-transition audit events through `WorkflowEventService`.
- `WorkflowTransitionAuditService` requires `before_state` and `after_state`, rejects equal states, rejects unknown action codes, and rejects action policies that are not configured as state transitions.
- `WorkflowTransitionAuditService` preserves existing `WorkflowEvent` idempotency, correlation, and causation behavior by mapping to the existing append command.
- `CanonicalWriteService` uses `CanonicalWriteGate` and appends audit `WorkflowEvent` for allowed boundary attempts, propagating idempotency/correlation/causation identifiers when supplied.
- Canonical persistence is explicitly deferred.
- `CanonicalWriteTransactionBoundary` is skeleton/no JDBC rollback coordination.
- No endpoint/API/UI/AI wiring exists for this flow yet.

## Current Non-capabilities

- No CandidateProfile canonical persistence.
- No raw Candidate/Profile persistence.
- No workflow engine.
- No SLA/automation workflow engine.
- No transition validation.
- No transition legality validation; WorkflowEvent policy validation is audit request validation only.
- No legal `from_state -> to_state` validation in the Task 4D transition audit skeleton.
- No target entity lookup or state mutation in `WorkflowTransitionAuditService`.
- No API layer.
- No UI integration.
- No AI model integration.
- No Consent/Disclosure implementation.
- No Client-safe projection.
- No RBAC/ABAC implementation.
- No dashboard analytics or generic repository search.
- No CandidateProfile canonical persistence.
