# Production Kernel Roadmap v0.3

## Source of Truth

- Product spec source of truth remains `docs/specs/CURRENT_SPEC.md`.
- v2.1 is the current product source of truth.
- v2.0 UI / portal definitions must be preserved.
- This roadmap is an engineering execution roadmap, not a replacement for product specs.

## Current Phase Summary

- Production Kernel is building the backend-owned truth, audit, governance, and access foundation.
- This is not demo-first and not MVP-first.
- Completed work so far is production-kernel infrastructure, not a full user-facing product.

## Completed Tasks

Task 0: Source of Truth ✅

Task 1: Production Skeleton ✅

Task 2: Truth Layer Foundation ✅

- 2A: Contracts-first truth-layer design docs ✅
- 2B: Truth-layer contract + V2 migration skeleton ✅
- 2C: Live PostgreSQL/Flyway migration verification ✅
- 2D: Canonical Write Gate skeleton ✅
- 2E: Domain/Contract/Migration alignment tests ✅
- 2F: Truth-layer negative policy tests ✅
- 2G: Minimal persistence port contracts ✅

Task 3: Truth Layer Persistence & Domain Services ✅

- 3A: ClaimLedger append persistence ✅
- 3B: ReviewEvent append persistence ✅
- 3C: WorkflowEvent append persistence ✅
- 3D: CanonicalWriteService transaction boundary skeleton ✅
- 3E: Service boundary hardening / regression sweep ✅
- 3F: Roadmap / status / known gaps materialization ⏳

## Next Tasks

Task 4: WorkflowEvent / Audit Foundation

- 4A: Workflow action vocabulary + audit policy
- 4B: Idempotency / correlation / causality guardrails
- 4C: Audit query/read model skeleton
- 4D: Transition audit skeleton, still not full workflow engine

Task 5: Governed Intake Minimal Slice

- SourceItem
- InformationPacket
- deterministic extraction placeholder
- ClaimLedger append
- ReviewEvent
- CanonicalWrite boundary
- no real AI yet

Task 6: Candidate Canonical Profile

- profile field status
- source lineage
- stale/conflict metadata
- real canonical profile persistence only after transaction boundary is hardened

Task 7: Client-safe Projection & Privacy Boundary

- ClientSafeCandidateCard
- redaction L0-L4
- forbidden field tests
- re-identification placeholder
- raw Candidate never exposed to Client

Task 8: Identity / RBAC / ABAC Kernel

Task 9: API Boundary & Contract Tests

Task 10: AITaskRun / AI Governance Skeleton

Task 11: Matching / Evidence Skeleton

Task 12: Consent / Disclosure Protection

Task 13: Five Portal Backend-approved UI Integration

Task 14: Production Hardening

## Execution Rule

Every future task must state:

- parent task
- subtask
- goal
- forbidden scope
- validation commands
- whether commit/merge is allowed
