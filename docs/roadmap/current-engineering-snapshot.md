# Current Engineering Snapshot

This file contains mutable short-term engineering state. Update it after future successful main merges.

## Current Main Baseline

- main HEAD before Task 9B worktree: `e612963d720c17e93d857817a0e21dd42cd0c2c1`
- latest merged commit before Task 9B: Add internal-safe API DTO contract skeleton
- Task 9B focused worktree validation: client-safe controller boundary test class, 8 tests, 0 failures, 0 errors
- merge status: Task 9B current worktree; do not self-reference the final commit here

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

## Current Known Gaps

- Task 7 is complete only for the current backend kernel scope.
- Task 8A is complete only for backend contract/evaluator-skeleton scope.
- Task 8B is complete only for minimal backend service-level enforcement on client-safe projection and raw Candidate/Profile guard surfaces.
- Task 8 is complete only for the current backend kernel scope: role/resource/action/field policy contracts exist, deterministic `PermissionEvaluator` exists, fail-closed `PermissionEnforcer` exists, sensitive backend guard slice exists, and five-portal boundary negative tests exist.
- Task 9A is complete only for internal-safe API DTO/contract skeleton and contract-test scope.
- Task 9B is complete only for the first client-safe controller boundary and no-internal-entity-leakage test scope.
- Task 9 remains incomplete: 9C API regression/docs closure remains.
- No real re-identification risk scorer exists beyond the deterministic Task 7C placeholder.
- No broad REST controller/API surface or UI yet; only the Task 9B client-safe candidate-card read endpoint exists.
- No real auth/login/session system yet.
- No Spring Security yet.
- No Consent/Disclosure/Unlock implementation yet.
- No identity-disclosed Client access behavior yet.
- No complete product-wide RBAC/ABAC enforcement yet.
- No real redaction pipeline or automatic text rewriting yet.
- No real AI extraction/model wiring yet.
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

Task 9C: API regression/docs closure

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
