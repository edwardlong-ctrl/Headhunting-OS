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

Task 7: Client-safe Projection & Privacy Boundary ✅

- 7A: ClientSafeCandidateCard contract + forbidden-field policy + L0-L4 vocabulary ✅
- 7B: client-safe projection service/read-model skeleton without API/UI exposure ✅
- 7C: re-identification placeholder plus Task 7 regression/docs closure ✅

Task 7A is a safe contract/policy/test slice only: it defines an anonymous
client-safe card, an explicit deny-by-default forbidden-field policy, and L0-L4
privacy vocabulary. Task 7B adds the minimal backend projection boundary from
an internal candidate/profile-like snapshot to `ClientSafeCandidateCard` only,
with field-policy enforcement, L4 rejection, and exact raw sensitive value
carryover blocking. Task 7C adds a deterministic backend-only re-identification
placeholder and regression closure. Task 7 is complete only for the current
backend kernel scope: client-safe contract, forbidden-field policy, L0-L4
vocabulary, projection/read-model boundary, raw exposure negative tests, and
re-identification placeholder. There is still no REST/API/controller/DTO/UI,
RBAC/ABAC, Consent/Disclosure/Unlock, real re-identification scorer, real
redaction pipeline, automatic text rewriting, or identity disclosure behavior.

Task 8: Identity / RBAC / ABAC Kernel ✅

- 8A: role/resource/action/field-policy contracts + evaluator skeleton ✅
- 8B: service-level permission enforcement on minimal sensitive backend boundaries ✅
- 8C: five-portal boundary negative tests/docs closure ✅

Task 8A is complete only for backend contracts and deterministic evaluator
skeleton scope. Task 8B adds a backend-only fail-closed `PermissionEnforcer`,
requires explicit `AccessRequest` context for client-safe projection, and adds a
minimal raw Candidate/Profile service guard. Task 8C adds five-portal boundary
negative regression tests for Owner, unified Consultant, Client, Candidate,
Admin, System, and AI assistant. Task 8 is complete only for the current backend
kernel scope: role/resource/action/field policy contracts, deterministic
`PermissionEvaluator`, fail-closed `PermissionEnforcer`, sensitive backend guard
slice, and five-portal boundary negative tests. Task 8 still adds no
API/controller/UI, auth/login/session, Spring Security, Consent/Disclosure/Unlock,
identity-disclosure behavior, or complete product-wide RBAC/ABAC enforcement.

Task 9: API Boundary ✅

- 9A: internal-safe DTO / API contract skeleton ✅
- 9B: client-safe controller boundary + no internal entity leakage tests ✅
- 9C: API regression/docs closure ✅

Task 9A adds only the minimal backend API DTO contract skeleton: response
envelope bounded to API-safe response bodies, safe error/access-denied/validation
DTOs, client-safe candidate card response DTO, contract rules, and a mapper from
`ClientSafeCandidateCard` to the API DTO. It adds no REST controllers, HTTP
endpoints, Spring Security, auth, session behavior, API runtime behavior, UI,
Consent/Disclosure/Unlock behavior, or identity disclosure behavior. Raw
Candidate and CandidateProfile remain not client-exposed.

Task 9B adds the first minimal client-safe controller boundary for reading one
anonymous client-safe candidate card by `card_` reference. The controller requires
explicit temporary access-context headers, fails closed on missing/denied context,
delegates to a safe query facade/port that returns `ClientSafeCandidateCard`, and
maps only through `ClientSafeCandidateCardResponse`. It adds no raw
Candidate/Profile endpoints, no broad REST API, no Spring Security, no
auth/login/session, no frontend/UI, no Consent/Disclosure/Unlock, and no identity
disclosure behavior.

Task 9C closes Task 9 for the current backend kernel scope only. The API-safe
DTO/envelope contracts exist, the client-safe candidate-card controller boundary
exists, the temporary header-based access context is fail-closed, sanitized API
error/denial responses exist, and API leakage regression tests cover the current
slice. Task 9 still adds no real auth/login/session, no Spring Security, no
frontend/UI, no Consent/Disclosure/Unlock, no identity disclosure workflow, no
broad API surface, and no production auth context. The temporary header-based
context remains non-production and fail-closed.

Task 10: AI Governance Kernel ✅

- 10A: AITaskRun contract / persistence / model-prompt-schema version fields ✅
- 10B: write-back target + human review status policy ✅
- 10C: governance regression/docs closure ✅

Task 10A adds minimal metadata auditability only: explicit AITaskRun status
vocabulary, task/model/prompt/schema version validation, safe failure reason
validation, requested-by/correlation/causation metadata, V7 database constraint
hardening, and append/readback PostgreSQL persistence. Task 10B adds explicit
write-back target and human-review status vocabulary plus metadata-only policy
decisions for AITaskRun governance. Task 10C closes the current backend kernel
scope with regression tests and docs closure proving AITaskRun metadata
persistence, deterministic fail-closed governance policy, no AI execution, no
actual write-back execution, no canonical fact write, no CandidateProfile
mutation, and no ClaimLedgerItem/ReviewEvent/WorkflowEvent writes from
AITaskRun governance. Task 10 is complete only for this backend kernel scope:
there is still no real AI model call, model routing, prompt execution, AI task
queue/worker, automatic human review workflow, canonical write execution from AI
governance, AI governance API/controller, or UI.

Task 11: Matching / Evidence Kernel ⏳

- 11A: MatchReport scoring contracts + score-cap policy skeleton ✅
- 11B: MatchReport generation service / evidence coverage / provenance weighting placeholder ✅
- 11C: matching/evidence regression/docs closure ⏳

Task 11A adds a backend-only `matching` package with opaque MatchReport/job/
subject references, 1-5 score validation, required dimension score vocabulary,
explicit score confidence, bounded evidence coverage, provenance/source-strength
placeholders, assertion-strength and authenticity-risk awareness, ontology and
industry-pack version placeholders, and a deterministic score-cap policy. The
policy caps insufficient independent high-trust evidence, cold industry packs,
keyword-only evidence without project evidence, weak-signal intent, stale
ontology/industry-pack metadata, and high authenticity risk according to v2.1
rules. High re-identification risk blocks client delivery pending privacy review.
Task 11A is contract/policy/test only: it adds no real AI matching, model calls,
prompt execution, model routing, persistence, API/controller/UI, client-facing
delivery, canonical fact writes, CandidateProfile mutation, or
ClaimLedgerItem/ReviewEvent/WorkflowEvent writes.

Task 11B adds a minimal deterministic backend-only MatchReport generation
service that accepts safe opaque refs and scoring/evidence/provenance metadata,
builds bounded evidence coverage, builds deterministic provenance summary
metadata, applies `ScoreCapPolicy` before returning a `MatchReport`, and keeps
the report non-canonical and not client-safe API output. It still adds no real
AI matching, model calls, prompt execution, model routing, persistence,
API/controller/UI, client-facing delivery, canonical fact writes,
CandidateProfile mutation, or ClaimLedgerItem/ReviewEvent/WorkflowEvent writes.
Task 11 is not complete.

## Next Tasks

Task 11C: matching/evidence regression/docs closure

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
