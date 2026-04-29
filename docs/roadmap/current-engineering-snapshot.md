# Current Engineering Snapshot

This file contains mutable short-term engineering state. Update it after future successful main merges.

## Current Main Baseline

- current main HEAD: `02e8700c30de38a9ae73ac34b18d9d759d6d72bf`
- latest merged commit: `02e8700` Harden consent disclosure chain persistence
- current validation snapshot: full backend Maven suite reached 533 tests, 0 failures/errors, 1 existing skip; frontend typecheck/build and whitespace diff checks were re-run for the current roadmap drift fix.
- merge status: main contains Task 14; roadmap next task is intentionally pending human prioritization.

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
- Task 10C: AI governance regression/docs closure ✅
- Task 10: AI Governance Kernel ✅ for current backend kernel scope
- Task 11A: MatchReport scoring contracts and score-cap policy skeleton ✅
- Task 11B: MatchReport generation service / evidence coverage / provenance weighting placeholder ✅
- Task 11C: Matching / evidence regression and docs closure ✅
- Task 11: Matching / Evidence Kernel ✅ for current backend kernel scope
- Task 12A: Consent / Disclosure Protection first backend-only kernel ✅
- Task 12B: Consent / Disclosure persistence and audited service boundary ✅ for current backend kernel scope
- Task 13A: Five-portal UI shell and client-safe candidate-card route ✅ for the current integrated slice
- Task 13B: Real client-safe candidate card backend query slice ✅
- Task 14: Consent / Disclosure production hardening ✅ for the current backend kernel scope

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
- Task 10C closes the current AI governance backend kernel scope with regression coverage proving AITaskRun persistence stores model/prompt/schema/task version metadata, safe status metadata, write-back target metadata, and human-review metadata only; it does not call AI/model services, execute prompts, route models, queue workers, retry/async work, execute write-back, invoke `CanonicalWriteService`, write canonical facts, mutate CandidateProfile, or append ClaimLedgerItem/ReviewEvent/WorkflowEvent rows.
- Task 10 is complete only for the current backend kernel scope: AITaskRun metadata contract and persistence exist, model/prompt/schema/task version fields exist, write-back target vocabulary exists, human-review status vocabulary exists, deterministic fail-closed governance policy exists, and regression tests prove no AI execution, no write-back execution, and no canonical mutation.
- Task 11A adds a backend-only `matching` package with `MatchReport`, opaque match/job/subject references, 1-5 `MatchScore`, required dimension scores, score confidence, bounded evidence coverage, provenance/source-strength/weight placeholders, assertion-strength and authenticity-risk awareness, ontology/industry-pack version placeholders, and generated-at metadata.
- Task 11A adds deterministic `ScoreCapPolicy` / `ScoreCapDecision` contracts that cap insufficient independent high-trust evidence to max 4, cold industry packs to max 3, keyword-only evidence without project evidence to max 3, weak-signal intent to max 3, stale ontology or stale industry-pack metadata to max 4, and high authenticity risk to max 4 with review/additional-evidence flags. High re-identification risk blocks client delivery pending privacy review.
- Task 11A regression coverage proves the contract does not expose raw Candidate/Profile, SourceItem/InformationPacket, ClaimLedger/ReviewEvent/WorkflowEvent/AITaskRun internals, raw source text, PII, consultant notes, API/controller/UI, persistence, AI/model calls, canonical fact writes, CandidateProfile mutation, or governance-event writes.
- Task 11A is contracts/policy/test only. `MatchReport` is not a canonical fact and is not a client-safe API output.
- Task 11B adds a backend-only deterministic `MatchReportGenerationService` plus safe `MatchReportGenerationRequest` / `MatchReportGenerationResult` value objects. The service accepts only opaque match/job/subject refs, requested `MatchScore` values, safe evidence signals, policy metadata, ontology/industry-pack version strings, and generated-at metadata.
- Task 11B adds a deterministic evidence coverage placeholder that tracks required dimensions, covered dimensions, missing evidence dimensions, weak-signal-only dimensions, independent evidence count, independent high-trust evidence count, bounded `EvidenceCoverage`, and score confidence impact.
- Task 11B adds a deterministic provenance weighting placeholder covering `EXTERNAL_VERIFIED`, `CANDIDATE_CONFIRMED`, `CONSULTANT_ATTESTED`, `HUMAN_ACKNOWLEDGED`, `AI_EXTRACTED`, `SYSTEM_INFERENCE`, `WEAK_SIGNAL`, and `UNKNOWN` categories. Unknown provenance fails closed for generation; AI/system/weak-signal provenance cannot support high-confidence top scoring alone.
- Task 11B generation applies existing `ScoreCapPolicy` before returning the `MatchReport`, caps dimension scores to the final cap, preserves safe cap decisions/reasons, and keeps generated reports non-canonical and not client-safe API output.
- Task 11B regression coverage proves no raw Candidate/Profile, SourceItem/InformationPacket, raw source text, PII, consultant notes, internal audit data, API/controller/UI, persistence, AI/model calls, canonical fact writes, CandidateProfile mutation, or ClaimLedgerItem/ReviewEvent/WorkflowEvent writes are added.
- Task 11C adds matching/evidence regression closure coverage proving MatchReport and generation contracts remain opaque-ref-only, non-canonical, not client-safe API output, free of raw Candidate/Profile/source/governance leakage, deterministic across score/evidence/provenance metadata, and bounded by score-cap policy before return.
- Task 11C regression coverage proves no real AI/model service call, prompt execution, model routing, worker queue, persistence, database migration, canonical write, CandidateProfile mutation, ClaimLedgerItem append, ReviewEvent append, WorkflowEvent append, API/controller, or UI surface is added by Task 11 code.
- Task 11 is complete only for the current backend kernel scope: MatchReport contracts exist, 1-5 score and dimension vocabularies exist, score confidence and evidence coverage metadata exist, provenance weighting placeholder exists, deterministic ScoreCapPolicy exists, MatchReportGenerationService placeholder exists, and regression tests prove no AI execution, no persistence, no canonical mutation, and no client/API exposure.
- Task 12A adds a backend-only `consentdisclosure` package with minimal immutable `ConsentRecord`, `DisclosureRecord`, `UnlockDecision`, `DisclosureLevel`, status/review vocabulary, and audit-boundary command/result contracts.
- `ConsentDisclosureProtectionPolicy` is a pure deterministic fail-closed policy. It allows existing anonymous L0/L1/L2 client-safe levels, allows L3 only with confirmed consent, denies raw Candidate/raw CandidateProfile exposure, and requires confirmed non-expired/non-revoked consent plus approved human unlock decision plus approved disclosure record plus WorkflowEvent/audit boundary metadata for L4 identity disclosure.
- Task 12A regression coverage proves L4/identity disclosure cannot be granted by role alone, unlock/disclosure cannot bypass the new protection policy, missing/invalid/expired/revoked/not-human-approved states fail closed, allowed L4 decisions carry an explicit T4 `DISCLOSURE_IDENTITY_DISCLOSED` audit command, and no API/controller/UI/persistence/AI/canonical-write surface is added.
- Task 12B adds the first PostgreSQL-backed consent/disclosure/unlock persistence slice through `V8__add_consent_disclosure_persistence.sql`, narrow backend-internal append/read ports plus JDBC adapters, and a deterministic `ConsentDisclosureService` that reads persisted consent/unlock/disclosure state, reuses `ConsentDisclosureProtectionPolicy`, returns safe allow/deny/requires-review results, appends `WorkflowEvent` only on allowed audited L4 transitions, and persists the resulting identity-disclosed boundary without mutating raw Candidate/Profile or bypassing `CanonicalWriteGate`.
- Task 12B regression and PostgreSQL/Testcontainers coverage proves organization-scoped append/readback for `ConsentRecord` / `UnlockDecision` / `DisclosureRecord`, fail-closed denial for missing/mismatched/expired/revoked/not-human-approved persisted state, deferred job/fee/prior-contact/prior-application checks return explicit review reasons rather than silent allow, allowed L4 requests append exactly one audited `DISCLOSURE_IDENTITY_DISCLOSED` `WorkflowEvent` plus one resulting disclosure boundary, and no API/controller/UI/auth/session/direct client-read behavior is added.
- Task 13A adds the route-aware five-portal web shell while preserving Consultant as one unified portal and keeping the v2.0/v2.1 portal taxonomy intact. It adds the narrow Client portal entry flow for anonymous candidate cards, fail-closed client-safe loading states, the typed frontend helper for the existing client-safe card endpoint, and a narrow Vite `/api` dev proxy without adding raw Candidate/Profile client exposure, identity-disclosed client read behavior, auth/session/Spring Security, or backend-truth drift.
- Task 13B adds a narrow PostgreSQL-backed `ClientSafeCandidateCardQueryPort` implementation for the existing `GET /api/client-safe/candidate-cards/{anonymousCardRef}` endpoint. It reads only backend-owned client-safe projection metadata from `recruiting.candidate_profile`, rebuilds an internal projection snapshot, reuses `ClientSafeCandidateProjectionService` plus the re-identification boundary, and fails closed to unavailable when data is missing, ambiguous, invalid, L4/identity-disclosed, cross-organization, or carrying raw sensitive values.
- Task 13B regression and PostgreSQL/Testcontainers coverage proves the existing endpoint can return a real safe success-state card from backend data, while preserving sanitized denial/unavailable behavior, anonymous `card_` references only, client-safe DTO mapping only, organization scope, no raw Candidate/Profile client exposure, no L4 identity-disclosed output, no Spring Security/auth/session, no broad workflow/API expansion, and compatibility with the existing Task 13A route through runtime-configured temporary organization scope.
- Task 14 hardens the backend Consent / Disclosure slice without expanding product surface. It keeps `L3_CONSENTED_DETAIL` separate from identity disclosure, binds approved disclosure records to the requested consent/unlock chain, makes final disclosure persistence retry-safe, adds organization-scoped consent/disclosure linkage hardening through `V9__harden_consent_disclosure_org_scope_links.sql`, enforces runtime denial for legacy cross-organization unlock approvers, and preserves fail-closed L4 redaction-level checks without adding API/controller/UI/auth/session/Spring Security or identity-disclosed client reads.

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
- Task 10C is complete only for AI governance regression/docs closure.
- Task 10 is complete only for the current backend kernel scope.
- Task 11A is complete only for MatchReport scoring contracts, evidence/provenance placeholder vocabulary, and deterministic score-cap policy tests.
- Task 11B is complete only for deterministic backend-only MatchReport generation from safe scoring inputs, evidence coverage/provenance placeholders, and ScoreCapPolicy integration.
- Task 11C is complete only for matching/evidence regression and docs closure.
- Task 11 is complete only for the current backend kernel scope.
- Task 12A is complete only for backend contracts, vocabulary, pure fail-closed policy, and regression tests.
- Task 12B is complete only for the current backend kernel scope: PostgreSQL persistence for consent/disclosure/unlock records exists, a backend-internal audited service boundary exists, allowed audited L4 transitions append `WorkflowEvent` plus resulting disclosure boundary records, and regression tests prove no raw Candidate/Profile mutation, no broad API/UI/auth surface, and no identity-disclosed client read behavior.
- Task 13A is complete only for the current integrated frontend slice: route-aware five-portal shell exists, Consultant remains one unified portal, the Client route can open anonymous candidate cards through the existing narrow endpoint, and fail-closed safe UI states exist. It does not add raw Candidate/Profile client exposure, identity-disclosed client read behavior, auth/session/Spring Security, or broad product workflow expansion.
- Task 13B is complete only for a real backend-internal client-safe candidate card query/read-model slice behind the existing endpoint. It does not add broad shortlist service behavior, frontend UI changes, production auth/session, Spring Security, L4 identity disclosure, Consent/Disclosure workflow expansion, workflow engine behavior, or raw Candidate/Profile API exposure.
- Task 14 is complete only for the current backend kernel scope: the consent/disclosure service and persistence layer are hardened for chain binding, L3/L4 separation, retry-safe final disclosure persistence, organization-scoped linkage, and legacy cross-organization approver denial. It still does not add Consent/Disclosure/Unlock API/controller/UI, real auth/session enforcement, Spring Security, full workflow execution, prior-contact/prior-application review flow, fee-agreement validation, job-activation lookup, or identity-disclosed Client read behavior.
- No real AI matching, model routing, prompt execution, AI task queue/worker, matching persistence, matching API/controller/UI, client-facing match report delivery, or real industry ontology calibration exists yet.
- No outcome-label feedback loop exists yet.
- No real re-identification risk scorer exists beyond the deterministic Task 7C placeholder.
- No broad REST controller/API surface or UI yet; only the existing client-safe candidate-card read endpoint exists, now backed by a narrow Task 13B PostgreSQL client-safe projection query slice.
- No real auth/login/session system yet.
- No Spring Security yet.
- No Consent/Disclosure/Unlock API/controller/UI, real workflow execution, real auth/session enforcement, prior-contact/prior-application review flow, fee-agreement validation, job-activation lookup, or identity-disclosed client read behavior exists yet.
- No identity-disclosed Client access behavior yet.
- No complete product-wide RBAC/ABAC enforcement yet.
- No real redaction pipeline or automatic text rewriting yet.
- No real AI extraction/model wiring yet.
- No model routing, prompt execution, AI task queue/worker, actual write-back execution, automatic human review workflow, canonical write execution from AI governance, or AI governance API/UI yet.
- No workflow engine or transition legality validation yet.
- No stale detection engine.
- No conflict resolution workflow.
- No full CandidateProfile engine.
- Blocked canonical attempts still have no separate persisted audit ledger.
- recruiting.* source/packet cleanup/deprecation remains deferred.

## Next Recommended Task

Task 15: Product Readiness Bridge, using:

- `docs/roadmap/product-scope-after-kernel.md`
- `docs/roadmap/productization-roadmap.md`
- `docs/roadmap/productization-roadmap.zh-CN.md`
- `docs/roadmap/pilot-readiness-checklist.md`

Task 14 remains Production Kernel completion for the current backend kernel
scope. It is not full product completion, not Usable v1, and not pilot-ready by
itself.

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
