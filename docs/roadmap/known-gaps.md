# Known Gaps

## Canonical Persistence Deferred

- `CanonicalWriteService` is a gate/audit boundary, not a profile writer.
- `canonicalPersistencePerformed=false` is intentional.
- `recruiting.candidate` and `recruiting.candidate_profile` exist in V2, but Task 3D intentionally did not implement CandidateProfile writes.

## Transaction Boundary Skeleton

- `CanonicalWriteTransactionBoundary` is currently a skeleton/abstraction.
- It does not provide real JDBC rollback coordination.
- Do not rely on it for multi-table atomicity yet.

## Consent / Disclosure Not Implemented

- `ConsentRecord` and `DisclosureRecord` are expected by product spec but not yet implemented as behavior.
- Unlock/disclosure gate is not implemented.
- No identity disclosure behavior exists.
- Known alignment skip for consent/disclosure gap remains intentional until Task 12 or earlier dedicated workstream.

## AITaskRun / AI Governance Not Implemented

- No real AI model wiring.
- No AITaskRun persistence implementation yet.
- No prompt/model/schema version tracking yet.
- No `write_back_target` enforcement beyond current boundary concepts.

## Client-safe Projection Not Implemented

- Raw Candidate must never be exposed to Client.
- `ClientSafeCandidateCard` does not exist yet.
- Redaction L0-L4 does not exist yet.
- Re-identification risk scorer does not exist yet.

## Identity / RBAC / ABAC Not Implemented

- Five portal route shells exist, but backend role/permission enforcement is not production-ready.
- Field-level access control remains future work.

## Workflow Engine Not Implemented

- WorkflowEvent append exists.
- Workflow action vocabulary and audit policy exist after Task 4A.
- WorkflowEvent idempotency, correlation, and causation guardrails exist after Task 4B.
- WorkflowEvent audit query/read model skeleton exists after Task 4C.
- Workflow transition audit skeleton exists after Task 4D.
- Task 4D records transition audit events with `before_state` and `after_state` through the existing `WorkflowEvent` append boundary.
- Task 4D is backend-internal only.
- Workflow action audit still exists only at append-boundary validation level.
- No state machine.
- No transition legality validation.
- No entity-state lookup or mutation is performed by WorkflowEvent policy validation.
- No entity-state lookup or mutation is performed by `WorkflowTransitionAuditService`.
- No SLA/automation workflow engine.
- Full workflow engine remains future work.
- Task 5 Governed Intake Minimal Slice remains future work.

## Workflow Read Model Remaining Gaps

- Task 4C adds a backend-internal, read-only audit query/read model for `WorkflowEvent`.
- It is not an API/controller layer.
- It is not UI integration.
- It is not a client-safe projection.
- It does not expose raw Candidate/Profile payloads or business entity internals.
- It does not join Candidate, Company, Job, Consent, Disclosure, Placement, or Commission tables.
- It is not dashboard analytics, full reporting, full-text search, generic repository search, or arbitrary SQL filtering.
- Correlation and causation identifiers are queryable for backend audit lineage, but no user-facing timeline/query API exists yet.

## API Boundary Not Implemented

- No REST/API DTO layer for truth layer.
- No client-safe response contract.
- No controller boundary tests.

## UI / AI / Access Boundaries Not Implemented

- No UI integration exists for WorkflowEvent audit guardrails.
- No real AI model wiring exists for workflow actions.
- No Consent/Disclosure behavior exists.
- No RBAC/ABAC implementation exists.
- No Client-safe projection or redaction behavior exists.
- No CandidateProfile canonical persistence exists.
