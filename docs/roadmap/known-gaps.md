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
- Workflow action audit exists only at append-boundary level.
- No state machine.
- No transition legality validation.
- No SLA/automation workflow engine.

## API Boundary Not Implemented

- No REST/API DTO layer for truth layer.
- No client-safe response contract.
- No controller boundary tests.
