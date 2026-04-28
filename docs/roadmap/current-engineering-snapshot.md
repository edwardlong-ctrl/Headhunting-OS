# Current Engineering Snapshot

This file contains mutable short-term engineering state. Update it after future successful main merges.

## Current Main Baseline

- main HEAD before Task 10B worktree: `25ddd848d8927089d734bd64ab7096a887e011b8`
- latest merged commit before Task 10B: Add AI task run governance persistence
- Task 10B worktree validation: focused AI write-back policy/AITaskRun metadata suite, 38 tests, 0 failures/errors; full backend Maven suite, 442 tests, 0 failures/errors, 1 existing skip
- merge status: Task 10B current worktree; do not self-reference the final commit here

## Completed Major Tasks

- Task 0: Source of Truth ✅
- Task 1: Production Skeleton ✅
- Task 2: Truth Layer Foundation ✅
- Task 3: Truth Layer Persistence & Domain Services ✅
- Task 3F: Roadmap / status / known gaps materialized ✅
- Task 4: WorkflowEvent / Audit Foundation ✅
- Task 5: Governed Intake Minimal Slice ✅
- Task 6: Candidate Canonical Profile minimal slice ✅
- Task 7A: Client-safe projection contract / policy / vocabulary ✅
- Task 7B: Client-safe projection service / read-model boundary ✅
- Task 7C: Re-identification placeholder / Task 7 regression closure ✅
- Task 7: Client-safe Projection & Privacy Boundary ✅ for current backend kernel scope
- Task 8A: Identity / RBAC / ABAC contract and evaluator skeleton ✅
- Task 8B: Service-level permission enforcement on minimal sensitive backend boundaries ✅
- Task 8C: Five-portal boundary negative tests/docs closure ✅
- Task 8: Identity / RBAC / ABAC Kernel ✅ for current backend kernel scope
- Task 9A: Internal-safe DTO / API contract skeleton ✅
- Task 9B: Client-safe controller boundary + no internal entity leakage tests ✅
- Task 9C: API regression/docs closure ✅
- Task 9: API Boundary ✅ for current backend kernel scope
- Task 10A: AITaskRun / AI Governance Skeleton ✅
- Task 10B: Write-back target + human review status policy ✅

## Current Truth/Kernel Capabilities

- ClaimLedger append persistence exists.
- ReviewEvent append persistence exists.
- WorkflowEvent append/audit foundation exists.
- CanonicalWriteGate exists and must be used before canonical writes.
- CanonicalWriteService boundary exists.
- CandidateProfile minimal canonical field write exists through the gated transaction path.
- CandidateProfile lineage/stale/conflict metadata persistence exists.
- `ClientSafeCandidateCard` now exists as a backend-only anonymous contract.
- `ClientVisibleCandidateFieldPolicy` now denies forbidden and unknown client-visible candidate fields.
- `RedactionLevel` now defines L0-L4 vocabulary, with L4 separated from anonymous client-safe card exposure.
- `ClientSafeCandidateProjectionService` now projects an internal candidate/profile-like snapshot into `ClientSafeCandidateCard` only.
- The minimal projection boundary validates selected client-visible fields through `ClientVisibleCandidateFieldPolicy`, rejects L4, and blocks exact raw sensitive value carryover into safe output text.
- A deterministic backend-only `ReidentificationRiskAssessmentService` placeholder now records obvious re-identification risk categories and returns allow/generalize/review/block decisions.
- Task 7 regression coverage now proves the client-safe contract, forbidden-field policy, L0-L4 vocabulary, projection/read-model boundary, raw exposure negative cases, and re-identification placeholder.
- `identityaccess` now defines backend-only role, resource, action, field-classification, relationship-scope, access-request, and access-decision contracts.
- `PermissionEvaluator` / `FieldAccessPolicy` now provide a deterministic no-database, no-Spring-Security evaluator skeleton that is deny-by-default, denies Client raw Candidate/CandidateProfile and unsafe fields, allows Client only to read `CLIENT_SAFE_CANDIDATE_CARD` at `CLIENT_SAFE` / `GENERALIZED` levels, and allows Candidate self-scoped safe profile reads only with explicit `SELF` scope.
- `PermissionEnforcer` / `AccessDeniedException` now provide a reusable backend-only fail-closed service guard that preserves `AccessDecision` reason codes and safe explanations.
- `ClientSafeCandidateProjectionService` now requires an explicit `AccessRequest` before projecting a `ClientSafeCandidateCard`.
- `CandidateProfileAccessService` now provides a minimal access-checked backend facade/guard for raw Candidate/Profile reads and sensitive candidate actions before delegating to profile service methods.
- Task 8C regression coverage now proves five-portal and automation-role deny-by-default behavior across Owner, Consultant, Client, Candidate, Admin, System, and AI assistant; client-safe card remains the only Client-readable candidate-facing output at this layer; raw Candidate/CandidateProfile, unsafe fields, identity-disclosed/L4 anonymous access, sensitive actions, role-alone canonical-write/disclosure bypasses, and unknown vocabulary remain denied.
- `apiboundary` now defines a minimal backend API DTO contract skeleton: response envelope bounded to API-safe response bodies, safe error/access-denied/validation DTOs, client-safe candidate card response DTO, contract rules, and a mapper from `ClientSafeCandidateCard` only.
- Task 9A API boundary tests prove the client-safe API DTO omits raw Candidate/CandidateProfile, SourceItem, InformationPacket, ClaimLedger, ReviewEvent, WorkflowEvent, raw candidate/profile ids, PII, raw source, consultant notes, and L4 identity-disclosed fields.
- Task 9B adds the first minimal client-safe controller endpoint: `GET /api/client-safe/candidate-cards/{anonymousCardRef}`. The path uses the `card_` anonymous card reference, requires explicit temporary access-context headers, delegates to a safe query facade/port returning `ClientSafeCandidateCard`, maps only through `ClientSafeCandidateCardResponseMapper`, and returns the existing API-safe envelope.
- Task 9B controller tests prove successful responses expose only the client-safe DTO/envelope, omit raw ids/PII/raw source/consultant notes/exact employer/project/product/chip/L4 identity fields, fail closed on missing/denied/identity-disclosed access context, sanitize denials, reject raw UUID path refs, expose no raw Candidate/Profile/governance types, and add no raw Candidate/Profile endpoints.
- Task 9C closes the current backend API boundary scope with regression tests proving anonymous-card-only request paths, raw id rejection, fail-closed temporary access context, missing/unsupported context denial, sanitized denied/not-found/internal-error envelopes, successful DTO/envelope-only responses, no raw domain/governance type exposure in controller/facade/port/mapper surfaces, no broad/raw/disclosure/unlock/consent endpoint surface, and stricter API-visible text sanitization for DTO/error text.
- Task 10A adds minimal AITaskRun metadata auditability: explicit `CREATED/RUNNING/SUCCEEDED/FAILED/CANCELLED` status vocabulary, task/model/prompt/schema version validation, safe failure reason validation, requested-by/correlation/causation metadata, and append/readback PostgreSQL persistence through `AITaskRunService` / `JdbcAITaskRunPort`.
- Task 10B adds explicit AI write-back target and human-review status vocabulary plus deterministic `AITaskGovernancePolicy` decisions for metadata validation. It accepts no-write-back and claim-ledger proposal metadata, requires approved human review plus CanonicalWriteGate for canonical targets, requires client-safe boundary semantics for client-visible projection targets, and blocks consent/disclosure/unlock, workflow-action, and commercial/placement targets in this kernel.

## Current Known Gaps

- Task 7 is complete only for the current backend kernel scope.
- Task 8A is complete only for backend contract/evaluator-skeleton scope.
- Task 8B is complete only for minimal backend service-level enforcement on client-safe projection and raw Candidate/Profile guard surfaces.
- Task 8 is complete only for the current backend kernel scope: role/resource/action/field policy contracts exist, deterministic `PermissionEvaluator` exists, fail-closed `PermissionEnforcer` exists, sensitive backend guard slice exists, and five-portal boundary negative tests exist.
- Task 9A is complete only for internal-safe API DTO/contract skeleton and contract-test scope.
- Task 9B is complete only for the first client-safe controller boundary and no-internal-entity-leakage test scope.
- Task 9 is complete only for the current backend kernel scope: API-safe DTO/envelope contracts, one client-safe candidate-card controller boundary, fail-closed temporary access context, sanitized API error/denial responses, and API leakage regression tests.
- Task 10A is complete only for AITaskRun governance metadata contract and append/readback persistence.
- Task 10B is complete only for write-back target vocabulary, human-review status vocabulary, metadata-only policy decisions, and AITaskRun metadata validation.
- Task 10 is not complete. Remaining scope is Task 10C governance regression/docs closure.
- No real re-identification risk scorer exists beyond the deterministic Task 7C placeholder.
- No broad REST controller/API surface or UI yet; only the Task 9 client-safe candidate-card read endpoint exists.
- No real auth/login/session system yet.
- No Spring Security yet.
- No Consent/Disclosure/Unlock implementation yet.
- No identity-disclosed Client access behavior yet.
- No complete product-wide RBAC/ABAC enforcement yet.
- No real redaction pipeline or automatic text rewriting yet.
- No real AI extraction/model wiring yet.
- No model routing, prompt execution, AI task queue/worker, actual write-back execution, or AI governance API/UI yet.
- No workflow engine or transition legality validation yet.
- No stale detection engine.
- No conflict resolution workflow.
- No full CandidateProfile engine.
- Blocked canonical attempts still have no separate persisted audit ledger.
- recruiting.* source/packet cleanup/deprecation remains deferred.

## Current Leftover Cleanup Note

Task 6F worktree was not deleted because it contains an untracked Vim swap file:

`/Users/edwardlong/.codex/worktrees/fc08/New project/services/core-api/src/test/java/com/recruitingtransactionos/coreapi/candidateprofile/.CandidateProfileContractTest.java.swp`

This worktree has already been merged to main, but cleanup was safely skipped. Do not force-delete it. Close the relevant Vim/editor/session and confirm recovery is unnecessary before deleting the swap file/worktree.

## Next Recommended Task

Task 10C: AI governance regression/docs closure

## Future Prompt Strategy

Future Codex prompts should normally include only:

- task name
- current main HEAD
- instruction to read `docs/roadmap/codex-task-operating-rules.md`
- instruction to read `docs/roadmap/current-engineering-snapshot.md`
- local task-relevant files to inspect
- goal
- forbidden scope
- validation commands if not already referenced
- final report requirements if special additions are needed
