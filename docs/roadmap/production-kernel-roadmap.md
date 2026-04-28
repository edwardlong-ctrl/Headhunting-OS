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

Task 4: WorkflowEvent / Audit Foundation ✅

- 4A: Workflow action vocabulary + audit policy ✅
- 4B: Idempotency / correlation / causality guardrails ✅
- 4C: Audit query/read model skeleton ✅
- 4D: Transition audit skeleton, still not full workflow engine ✅

Task 5: Governed Intake Minimal Slice ✅

- 5A: SourceItem / InformationPacket governed-intake operational tables ✅
- 5B: deterministic extraction placeholder output envelope ✅
- 5C: ClaimLedgerItem claim bridge from governed-intake lineage ✅
- 5D: ReviewEvent evidence bridge from governed-intake ClaimLedgerItem ✅
- 5E: CanonicalWriteService boundary bridge with mandatory gate ✅
- 5F: end-to-end regression and documentation closure ✅

Task 5 is a safe minimal slice only: no real AI, no canonical persistence, no
CandidateProfile persistence, no API/UI, and no client exposure.

Task 6: Candidate Canonical Profile ✅

- 6A: CandidateProfile contracts and field status vocabulary ✅
- 6B: backend-internal CandidateProfile persistence skeleton ✅
- 6C: real CanonicalWriteTransactionBoundary rollback behavior ✅
- 6D: first gated minimal CandidateProfile field write path ✅
- 6E: lineage / stale / conflict metadata persistence hardening ✅
- 6F: CandidateProfile regression and documentation closure ✅

Task 6 is a safe minimal backend slice only: governed-intake ClaimLedgerItem
plus ReviewEvent evidence can reach CanonicalWriteGate, run through the
transaction boundary, append WorkflowEvent audit, and write exactly one explicit
CandidateProfile field when the gate allows. Lineage, source-span, stale, and
conflict metadata are regression-covered for the backend-internal profile
surface. There is still no Client-safe projection, no REST/API/controller/DTO/UI,
no RBAC/ABAC, no Consent/Disclosure, no real AI extraction, no stale detection
engine, no conflict resolution workflow, and no full CandidateProfile engine.

## Next Tasks

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
