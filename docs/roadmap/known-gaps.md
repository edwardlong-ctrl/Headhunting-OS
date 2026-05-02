# Known Gaps

## Task 19A/19B/19C Auth Baseline, Controller Migration, and Hardening Complete; Extended Identity Features Deferred

- Task 19A adds V15 auth persistence baseline through `identity.user_account.password_hash` and `identity.session`.
- Spring Security now exists with a stateless filter chain, JWT issuance/validation, persisted refresh-token-backed session lifecycle, and `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`.
- Current scope now includes JWT-backed product-controller enforcement. Consultant/client-safe/document product endpoints read authenticated identity from Spring Security rather than temporary role/org headers.
- Remaining gaps:
  - No multi-organization membership/session switching exists yet; `identity.user_account` remains directly organization-scoped in this baseline.
  - No SSO/OIDC/external identity provider integration exists yet.
  - No password reset, MFA, email verification, lockout, or auth-audit hardening flow exists yet.

## Task 18C Consultant Shortlist CRUD + Sub-entity CREATE Complete; UPDATE/DELETE and Client Portal Deferred

- Task 18C completes the Consultant write API surface by adding Shortlist CREATE and UPDATE endpoints (with optimistic locking), plus CompanyContact, JobRequirement, and JobScorecard CREATE endpoints as sub-resources.
- `ShortlistPersistencePort.update()` and `JdbcShortlistPersistencePort.update()` with optimistic-locking JDBC implementation (`WHERE organization_id = ? AND version = ?`, `SET version = version + 1`).
- `ShortlistService.updateShortlist()` domain service method delegates to the port.
- `FieldAccessPolicy.decideConsultantAccess()` extended to allow CREATE and UPDATE on SHORTLIST alongside COMPANY and JOB.
- Five new request DTOs: `ShortlistCreateRequest`, `ShortlistUpdateRequest`, `CompanyContactCreateRequest`, `JobRequirementCreateRequest`, `JobScorecardCreateRequest`.
- `ConsultantApiCommandService` extended with 5 new methods: `createShortlist()`, `updateShortlist()`, `createCompanyContact()`, `createJobRequirement()`, `createJobScorecard()`.
- `ConsultantShortlistController` now has `@PostMapping` and `@PutMapping("/{shortlistId}")`.
- `ConsultantCompanyController` now has `@PostMapping("/{companyId}/contacts")`.
- `ConsultantJobController` now has `@PostMapping("/{jobId}/requirements")` and `@PostMapping("/{jobId}/scorecard")`.
- All sub-entity CREATE endpoints return the parent detail response (already includes nested sub-entity lists).
- Remaining gaps:
  - No UPDATE endpoints for sub-entities (CompanyContact, JobRequirement, JobScorecard). Sub-entities can only be created, not modified.
  - No DELETE endpoints for any Consultant resource (Company, Job, Shortlist, or any sub-entity).
  - No Soft-delete or archive behavior for any entity.
  - No Client-portal endpoints (read or write).
  - No Candidate/CandidateProfile write endpoints.
  - No batch operations.
  - No filtering/search on write responses.
-  - Real auth/login/session/Spring Security now exists, product endpoints are on JWT-backed `SecurityContext`, and Task 19C closes the current baseline hardening slice; remaining auth work is now longer-horizon identity capability backlog.

## Task 20 Document Storage v1 Baseline; Full Document Management Deferred

- Task 20 adds V13 migration with `mime_type`, `file_size_bytes`, `original_filename`, `scan_status` columns and `uq_source_item_org_content_hash` partial unique index on `intake.source_item`.
- `DocumentStore` interface, `DocumentStoreKey`, and `InMemoryDocumentStore` provide an object storage abstraction with S3-compatible key convention `{org_id}/{source_item_id}/{hash[:16]}/{filename}`.
- `VirusScanPort` and `NoOpVirusScanPort` are placeholder contracts only — no real virus scanning is implemented.
- `DocumentUploadService` validates MIME type against a fixed whitelist (PDF/DOCX 25MB, images 10MB, text 5MB), computes SHA-256 content hashes, deduplicates by (organization_id, content_hash), and idempotently returns existing SourceItem on duplicate.
- `ConsultantDocumentController` provides `POST /api/consultant/documents/upload` (multipart) and `GET /api/consultant/documents/{sourceItemId}/download` (proxy download). Download uses proxy streaming (not presigned URLs) and validates organization scope.
- No real virus scanning exists — `scan_status` defaults to `not_scanned` and `NoOpVirusScanPort` always returns CLEAN.
- No AI extraction or text parsing from uploaded documents (deferred to Task 22).
- No Client or Candidate upload endpoints (deferred to portal tasks 31/32).
- No presigned URL download (proxy-only for v1).
- No `MinioDocumentStore` or `FileSystemDocumentStore` production implementations exist — only `InMemoryDocumentStore` for tests.
- V2 `recruiting.source_item` and V4 `intake.source_item` tables remain separate (no merge).
- No `FieldAccessPolicy` entries for DOCUMENT resource type or UPLOAD action exist.
- No MinIO instance is configured in `docker-compose.yml` for production use.

## Task 22 Document Intelligence v1 Complete; OCR/STT, AI Claiming, and UI Productization Deferred

- Task 22 adds the first document-intelligence baseline through `DocumentParsingService`, `DocumentIntelligenceExtractionService`, `JdbcDocumentIntelligencePersistencePort`, and `ConsultantDocumentController` parse/evidence endpoints.
- `intake.parsed_document`, `intake.parsed_document_chunk`, and `intake.parsed_document_span` now persist evidence artifacts derived from uploaded documents.
- The current real parsing surface now exists for:
  - TXT
  - PDF
  - DOCX
- Image/OCR/STT inputs now fail closed into explicit non-success states instead of silently pretending parsing succeeded.
- Remaining gaps:
  - No real OCR/STT/file-conversion execution worker yet; images remain `PENDING_EXTERNAL_PROCESSING`.
  - No AI task queue/worker, retry scheduler, or long-running execution orchestration yet.
  - No ClaimLedger proposal append, review-queue append, WorkflowEvent append, or canonical write-back from document parsing outputs yet.
  - No client-safe evidence exposure or frontend review UI yet.
  - Task 23 backend/API now adds governed AI extraction and clean-fact generation, but broader productization still remains: no frontend review UI, no async worker orchestration, and no automatic reviewer workflow.
  - No automatic human review workflow, AI governance API/UI, or broader product integration yet.
  - No multi-provider product routing; the current real provider baseline is still DeepSeek only.

## Task 11 Matching / Evidence Kernel Closed for Backend Scope; Matching Engine Deferred

- Task 11A adds a backend-only `matching` package for evidence-backed MatchReport scoring contracts and deterministic score-cap policy.
- `MatchReport` uses opaque `match_report_`, `job_ref_`, and `match_subject_` references rather than raw Candidate or CandidateProfile objects.
- `MatchScore` validates the v2.1 1-5 score vocabulary for both overall score and dimension scores.
- Required dimensions are represented explicitly: technical fit, industry fit, seniority fit, salary fit, location fit, motivation fit, availability fit, evidence strength, and culture/manager fit.
- `ScoreConfidence` is explicit as low, medium, and high.
- `EvidenceCoverage` is bounded to a 0.0-1.0 ratio and tracks independent evidence count plus independent high-trust evidence count.
- Provenance placeholders include source category, source strength, bounded provenance weight, assertion-strength awareness, and authenticity-risk level.
- Version placeholders include ontology version and industry-pack version on the MatchReport contract.
- `ScoreCapPolicy` is deterministic and fail-closed for the current metadata-only scope.
- The score-cap policy caps insufficient independent high-trust evidence to max 4, cold industry packs to max 3, keyword-only evidence without project evidence to max 3, weak-signal intent to max 3, stale ontology or stale industry-pack metadata to max 4, and high authenticity risk to max 4 with review/additional-evidence flags.
- High re-identification risk blocks client delivery pending privacy review; it does not make the MatchReport a safe client-delivery DTO.
- `MatchReport` is not a canonical fact and is not a client-safe API response.
- Task 11A tests prove no raw Candidate/Profile, SourceItem/InformationPacket, ClaimLedger/ReviewEvent/WorkflowEvent/AITaskRun internals, raw source text, PII, consultant-private notes, API/controller/UI, persistence, AI/model call, canonical fact write, CandidateProfile mutation, or governance-event write is added by the matching package.
- Task 11A does not implement real AI matching, real candidate scoring, model calls, prompt execution, model routing, queue/worker behavior, API/controller, frontend/UI, database migration, persistence, client-facing match report delivery, Consent/Disclosure/Unlock, commercial/placement behavior, canonical facts, CandidateProfile mutation, ClaimLedgerItem append, ReviewEvent append, or WorkflowEvent append.
- Task 11B adds a deterministic backend-only MatchReport generation service that assembles `MatchReport` from safe opaque refs, safe requested scores, safe evidence coverage/provenance inputs, policy metadata, and generated-at/version metadata.
- Task 11B evidence coverage remains a placeholder: it tracks required dimensions, covered/missing dimensions, weak-signal-only dimensions, independent evidence counts, high-trust independent evidence counts, and bounded coverage/confidence impact without reading raw source text or creating claims/reviews/workflow events.
- Task 11B provenance weighting remains a placeholder: it distinguishes external verified, candidate confirmed, consultant attested, human acknowledged, AI extracted, system inference, weak signal, and unknown provenance categories; unknown provenance fails closed for generation; AI/system/weak signals can lower confidence or trigger score caps but do not create facts.
- Task 11B applies the existing `ScoreCapPolicy` before returning a report and preserves the cap decision/safe reason on the final `MatchReport`.
- Task 11B is complete only for deterministic backend-only generation from safe scoring inputs. It does not implement real AI matching, real scoring, model calls, prompt execution, model routing, persistence, API/controller/UI, client-facing delivery, canonical fact writes, CandidateProfile mutation, ClaimLedgerItem append, ReviewEvent append, or WorkflowEvent append.
- Task 11C adds matching/evidence regression closure coverage proving MatchReport and generation contracts remain opaque-ref-only, non-canonical, not client-safe API output, free of raw Candidate/Profile/source/governance leakage, deterministic across score/evidence/provenance metadata, and bounded by score-cap policy before return.
- Task 11C proves Task 11 code adds no real AI/model service call, prompt execution, model routing, worker queue, persistence, database migration, canonical write, CandidateProfile mutation, ClaimLedgerItem append, ReviewEvent append, WorkflowEvent append, API/controller, or UI surface.
- Task 11 is complete only for the current backend kernel scope: MatchReport contracts exist, 1-5 score and dimension vocabularies exist, score confidence and evidence coverage metadata exist, provenance weighting placeholder exists, deterministic ScoreCapPolicy exists, MatchReportGenerationService placeholder exists, and regression tests prove no AI execution, no persistence, no canonical mutation, and no client/API exposure.
- Broader gaps remain: no real AI matching, no real industry ontology calibration, no model routing, no prompt execution, no matching persistence, no client-facing match report delivery, no matching API/controller/UI, and no outcome-label feedback loop.

## Task 7 Backend Client-safe Boundary Exists; Full Privacy Pipeline Deferred

- Task 7A adds a backend-only `ClientSafeCandidateCard` contract using opaque anonymous/card identifiers, generalized profile fields, safe summary fields, safe evidence summary placeholders, and safe match narrative placeholders.
- Task 7A adds `ClientVisibleCandidateFieldPolicy` as an explicit deny-by-default forbidden-field policy for client-visible candidate fields.
- Task 7A adds L0-L4 `RedactionLevel` vocabulary aligned to v2.1 privacy semantics.
- `L4_IDENTITY_DISCLOSED` is vocabulary only in Task 7A and is rejected by `ClientSafeCandidateCard`; it does not grant disclosure permission or implement unlock behavior.
- Raw Candidate, CandidateProfile, SourceItem, InformationPacket, ClaimLedgerItem, ReviewEvent, WorkflowEvent, raw source text, raw document URLs, direct identity fields, consultant internal notes, and raw backend identifiers are not exposed through the Task 7A card contract.
- Task 7B adds a backend-only `ClientSafeCandidateProjectionService` and internal candidate/profile-like snapshot that return `ClientSafeCandidateCard` only.
- Task 7B validates selected client-visible field paths through `ClientVisibleCandidateFieldPolicy`, denies forbidden and unknown field selections, rejects L4 anonymous projection, and blocks exact raw sensitive value carryover into safe output text.
- Task 7C adds a deterministic backend-only re-identification placeholder with feature, level, decision, assessment, and service objects.
- Task 7C covers required unsafe categories: exact company + rare title + exact year, exact current employer, exact project/product/chip code name, public identifier before consent, exact location/address, direct contact/profile URL, small-team unique ownership claim, and overly specific identifying achievement number.
- Task 7C high-risk or L4 assessments cannot be treated as safe anonymous client output.
- Task 7 is complete only for the current backend kernel scope: client-safe contract, forbidden-field policy, L0-L4 vocabulary, projection/read-model boundary, raw exposure negative tests, and re-identification placeholder.
- No API/controller/UI, RBAC/ABAC, Consent/Disclosure/Unlock, real re-identification scorer, real redaction pipeline, automatic text rewriting, database migration, AI/model wiring, or real identity disclosure behavior exists from Task 7C.

## Canonical Persistence Minimal Path Exists; Metadata Hardened; Full Profile Deferred

- Task 6D adds the first real but minimal canonical CandidateProfile field write path.
- Task 6E hardens CandidateProfile lineage, stale, and conflict metadata persistence for that field-write surface.
- Task 6F regression-covers and documents the completed minimal Task 6 path.
- The write path exists only through `CanonicalWriteService` after `CanonicalWriteGate` allows the request.
- `CanonicalWriteTransactionBoundary` wraps the allowed WorkflowEvent audit and CandidateProfile field write.
- `canonicalPersistencePerformed=true` now means the minimal CandidateProfile field upsert succeeded.
- `canonicalPersistencePerformed=false` remains expected for gate-blocked, review-blocked, and audit-only attempts with no explicit CandidateProfile target.
- `recruiting.candidate` and `recruiting.candidate_profile` exist in V2; Task 6D writes only one explicit CandidateProfile field through the backend service boundary and does not write raw Candidate records.
- Task 6A defines CandidateProfile domain contracts and field vocabulary.
- Task 6B implements backend-internal CandidateProfile persistence.
- Task 6C hardens the canonical write transaction boundary.
- Task 6F proves the current safe chain from ClaimLedgerItem + ReviewEvent evidence to gated transaction-boundary audit/profile write, plus gate-blocked no-write behavior, rollback no-partial-write behavior, wrong-organization isolation, and no client/API/UI/projection exposure.
- Full CandidateProfile behavior, broad field families, conflict resolution, stale detection, API/UI exposure, client-safe projection service/read model, Consent/Disclosure, RBAC/ABAC, and real AI extraction remain deferred.

## Transaction Boundary Hardened; Full Canonical Flow Still Deferred

- Task 6C replaces the no-op/skeleton transaction boundary with `SpringCanonicalWriteTransactionBoundary`.
- The boundary uses Spring `PlatformTransactionManager` and `TransactionTemplate` for real Spring/JDBC transaction coordination.
- Successful callback commit and failing callback rollback behavior are covered by focused unit and PostgreSQL/Testcontainers integration tests.
- Runtime callback failures propagate and roll back participating JDBC writes.
- Checked callback failures are explicitly wrapped in `CanonicalWriteTransactionException` and roll back participating JDBC writes.
- Task 6D uses this boundary for the first minimal allowed CandidateProfile field write.
- This is still not a generic canonical write engine and does not implement full CandidateProfile.
- `CanonicalWriteGate` remains mandatory and must not be bypassed.

## CandidateProfile Persistence Minimal Write Exists; Full Promotion Deferred

- Task 6A adds pure backend-owned CandidateProfile contract vocabulary.
- Task 6B adds a backend-internal `CandidateProfileService`, `CandidateProfilePersistencePort`, and `JdbcCandidateProfilePersistencePort`.
- Task 6B reuses the existing V2 `recruiting.candidate_profile` table and adds no new migration, table, index, client-facing view, API DTO table, or competing profile table.
- Task 6B stores field status wire values in `field_status_map`, field values/lineage/conflict/staleness documents in `metadata.candidate_profile_fields`, and source claim id summaries in `source_claim_ids`.
- Task 6E keeps that existing JSONB metadata shape and hardens read/write preservation for ClaimLedgerItem, ReviewEvent, WorkflowEvent, SourceItem, InformationPacket, IntakeExtractionRun, source-span, external-evidence, stale, and conflict metadata.
- Task 6B create/read/upsert operations are organization-scoped and require the candidate row to belong to the same organization before a profile row can be created.
- The contract covers `CandidateProfile`, `CandidateProfileId`, `CandidateId`, `CandidateProfileVersion`, `CandidateProfileField`, `CandidateProfileFieldPath`, `CandidateProfileFieldStatus`, source lineage references, conflict metadata, and staleness metadata.
- Field statuses now include `AI_EXTRACTED`, `HUMAN_ACKNOWLEDGED`, `CONSULTANT_ATTESTED`, `CANDIDATE_CONFIRMED`, `EXTERNAL_VERIFIED`, `SYSTEM_INFERENCE`, `CONFLICTING`, `NEEDS_CONFIRMATION`, `STALE`, `UNVERIFIED`, and `LIKELY_CURRENT`.
- Bulk approve remains capped at `HUMAN_ACKNOWLEDGED`; it must not produce `CANDIDATE_CONFIRMED` or `EXTERNAL_VERIFIED`.
- `SYSTEM_INFERENCE` remains forbidden as fact and internal hint only.
- `CONFLICTING` must block overwrite/client-visible verified fact statements in later tasks.
- `NEEDS_CONFIRMATION` must block shortlist/consent/disclosure readiness in later tasks.
- Source lineage references support auditability only; ClaimLedgerItem, ReviewEvent, SourceItem, InformationPacket, IntakeExtractionRun, and WorkflowEvent references are not proof by themselves.
- Task 6E preserves lineage for auditability and uncertainty only; lineage does not resolve proof, create facts, trigger profile writes, or mutate source records.
- Task 6E preserves stale metadata and validates stale reason/time-range consistency, but it does not implement stale detection or background refresh.
- Task 6E preserves conflict metadata and requires source-backed conflict evidence for `CONFLICTING`, but it does not resolve conflicts, overwrite canonical value automatically, or implement reviewer decision flow.
- Task 6F adds regression closure that lineage is persisted/read back without becoming proof by itself, stale metadata persists without a stale detection engine, and conflict metadata persists without automatic conflict resolution or canonical value overwrite.
- ClaimLedgerItem remains claim input, not fact by itself.
- ReviewEvent remains review evidence, not fact promotion by itself.
- Task 6D allows governed-intake ClaimLedgerItem plus ReviewEvent evidence to flow to one explicit CandidateProfile field only after `CanonicalWriteGate` allows it and `CanonicalWriteService` runs the transaction boundary.
- Low-authority governed-intake placeholder claims remain blocked by the existing gate and do not write CandidateProfile.
- Task 6D does not mutate ClaimLedger verification status, does not mutate ReviewEvent, and does not treat ReviewEvent as fact promotion.
- No CandidateProfile REST/API/controller/DTO, UI, redaction pipeline, RBAC/ABAC, Consent/Disclosure, AI model wiring, or real AI extraction exists after Task 7B.

## Task 12A Consent / Disclosure Protection Kernel Exists; Full Unlock Flow Deferred

- Task 12A adds backend-only `ConsentRecord`, `DisclosureRecord`, `UnlockDecision`, disclosure-level/status/review vocabulary, and audit-boundary command/result contracts.
- `ConsentDisclosureProtectionPolicy` is pure and deterministic. It does not read databases, call services, mutate canonical facts, append WorkflowEvent rows, or expose raw Candidate/Profile data.
- Task 12B adds persisted `privacy.consent_record`, `privacy.unlock_decision`, and `privacy.disclosure_record` tables plus a backend-internal audited `ConsentDisclosureService` boundary, but only for backend persistence and service-level decisioning.
- Existing anonymous L0/L1/L2 client-safe projection behavior remains outside identity disclosure and can stay allowed at this policy layer.
- L3 consented detail requires confirmed consent but still does not allow raw Candidate/Profile or full identity exposure.
- L4 identity disclosure requires confirmed non-expired/non-revoked consent, approved human unlock decision, approved disclosure record, and explicit T4 `DISCLOSURE_IDENTITY_DISCLOSED` WorkflowEvent/audit boundary metadata.
- Raw Candidate and raw CandidateProfile exposure remain denied even when consent, disclosure, unlock, and audit metadata are present.
- Task 12A tests prove missing, invalid, expired, revoked, or not-human-approved consent/disclosure/unlock state fails closed, and role alone cannot grant L4 disclosure.
- No Consent/Disclosure REST/controller/API or UI exists yet.
- Real auth/login/session infrastructure now exists, and product-endpoint enforcement now runs through JWT-backed `SecurityContext` with Task 19C strong session revocation.
- No full WorkflowEvent-driven workflow engine, transition legality validation, prior-contact/prior-application review flow, fee-agreement validation, job-activation lookup, or identity-disclosed Client read behavior exists yet.

## Task 10 Metadata Governance + Task 21 Audited Execution Baseline Exist; Full AI Productization Deferred

- Task 10A adds minimal AITaskRun metadata auditability only.
- `AITaskRunStatus` is now an explicit small vocabulary: `CREATED`, `RUNNING`, `SUCCEEDED`, `FAILED`, and `CANCELLED`.
- AITaskRun append commands validate task version, input schema version, output schema version, prompt version, model provider/name, completion ordering, and safe single-line failure reasons for failed runs.
- `JdbcAITaskRunPort` can append, update, and read back AITaskRun audit records, preserving task/model/prompt/schema versions, requested-by, correlation, causation, target entity reference, source references, validated input/output payloads, optional write-back target metadata, tool-calls, cost units, trace refs, replay lineage, timing, failure reason, and created timestamp.
- V7 adds AITaskRun governance metadata columns and hardens the status/completion/failure-reason database constraints.
- Task 10B adds explicit `AITaskWriteBackTarget` vocabulary: `NONE`, `NO_WRITE_BACK`, `CLAIM_LEDGER_PROPOSAL`, `REVIEW_QUEUE`, `HUMAN_REVIEW_REQUIRED`, `CANONICAL_CANDIDATE_PROFILE`, `CLIENT_SAFE_PROJECTION`, `JOB_PROFILE`, `COMPANY_PROFILE`, `CONSENT_DISCLOSURE`, `WORKFLOW_ACTION`, and `COMMERCIAL_OR_PLACEMENT`.
- Task 10B adds explicit `AITaskHumanReviewStatus` vocabulary: `NOT_REQUIRED`, `REQUIRED`, `PENDING`, `APPROVED`, `REJECTED`, `NEEDS_REVISION`, and `EXPIRED`.
- Task 10B adds `AITaskGovernancePolicy` / `AITaskGovernanceDecision` for deterministic metadata validation with safe reason codes, safe explanations, human-review-required, canonical-gate-required, and future consent/disclosure/unlock gate flags.
- Task 10B validates AITaskRun write-back target and human-review status metadata before append; it reuses existing `write_back_target` and `human_review_status` persistence fields and adds no migration.
- Task 10C adds regression/docs closure for the current backend kernel scope.
- Task 10C proves AITaskRun persistence stores model/prompt/schema/task version metadata, safe status metadata, write-back target metadata, and human-review metadata only.
- Task 10C proves AITaskRun persistence does not call real AI/model services, execute prompts, route models, enqueue workers, perform retries or async execution, execute write-back, invoke `CanonicalWriteService`, write canonical facts, mutate CandidateProfile, append ClaimLedgerItem, append ReviewEvent, or append WorkflowEvent.
- Task 10C proves the write-back/review policy remains deterministic and fail-closed: unknown target/status deny by default, `NONE` / `NO_WRITE_BACK` remains metadata-only, claim-ledger proposal does not become fact, canonical targets require approved human review and canonical gate metadata, AI/System self-approval is denied, client-safe projection requires client-safe boundary semantics, and consent/disclosure/unlock, workflow-action, and commercial/placement targets remain blocked or future-gated.
- Task 10C strengthens `packages/contracts/schemas/ai-task-run.schema.json` so write-back target and human-review status vocabularies are explicit metadata-only schema vocabulary, not executable write-back or approval behavior.
- Task 10 is complete only for the current backend kernel scope: AITaskRun metadata contract exists, AITaskRun metadata persistence exists, model/prompt/schema/task version fields exist, write-back target vocabulary exists, human-review status vocabulary exists, deterministic fail-closed governance policy exists, and regression tests prove no AI execution, no write-back, and no canonical mutation.
- Task 21 adds the first real AI execution baseline on top of that governance kernel: prompt registry, schema validator, model router, DeepSeek provider adapter, replay, and two audited executable tasks.
- Broader AI gaps remain: no document intelligence/OCR/text extraction, no AI task queue/worker, no retries scheduler, no actual write-back execution, no AI governance API/UI, no automatic human review workflow, and no canonical write execution from AI governance.

## Governed Intake Minimal Slice Closed; Downstream Work Deferred

- Task 5A now provides backend-owned `SourceItem` and `InformationPacket` contracts, persistence ports/adapters, and `intake.source_item`, `intake.information_packet`, and `intake.information_packet_source_item` tables.
- Task 5B now provides backend-owned `IntakeExtractionRun` and `IntakeExtractionOutputEnvelope` contracts, a deterministic placeholder extractor, a narrow extraction-run persistence port/adapter, and `intake.extraction_run`.
- Task 5B extraction output is stored as JSONB in `intake.extraction_run.output_json`.
- Task 5B performs no real AI extraction, no LLM call, no OCR/STT/file conversion, and no semantic parsing.
- Task 5B extraction output is an intermediate envelope only. It is not canonical fact storage, ClaimLedger, ReviewEvent, CandidateProfile persistence, client-safe projection, or CanonicalWrite output.
- Task 5B explicitly sets output-envelope guardrail fields such as `real_ai_extraction_performed=false`, `claim_ledger_append_allowed=false`, and `canonical_write_allowed=false`.
- Task 5B leaves `InformationPacket.processingStatus` updates intentionally deferred to a future governed-intake lifecycle task.
- Task 5C now provides a backend-owned extraction-output to ClaimLedger bridge skeleton.
- Task 5C reads `intake.extraction_run` output envelopes and validates `intake.information_packet` lineage before appending through `ClaimLedgerService`.
- Task 5C default deterministic placeholder output creates no fake business claims and returns no ClaimLedger append because placeholder metadata is not bridge-eligible.
- Task 5C only appends explicitly marked operational `CLAIM_CANDIDATE` fixture fields under `intake.bridge_eligible.*`; these records are internal-only claims, not canonical candidate/company/job facts.
- Task 5C `ClaimLedgerItem` rows remain claims, not facts; ClaimLedger-to-canonical promotion is still blocked by future review/gate work.
- Task 5C adds V6 `claim_ledger_org_source_span_idx` for narrow source-span idempotency lookup and deterministic duplicate suppression.
- Task 5D now provides a backend-owned governed-intake ClaimLedger-to-ReviewEvent bridge skeleton.
- Task 5D reads only `governance.claim_ledger_item` records by organization-scoped claim id, requires Task 5C governed-intake `intake.*` lineage, and appends review evidence only through `ReviewEventService`.
- Task 5D ReviewEvent append is review evidence, not fact promotion. It does not mutate ClaimLedger verification status, does not call CanonicalWrite, and does not write CandidateProfile.
- Task 5D stores claim lineage in `governance.review_event.claim_ledger_item_id` and deterministic `source_span_ref` values. It added no new migration or table.
- Task 5D duplicate behavior is deterministic: identical bridge requests return the existing review event id; materially different review evidence is allowed as a new review event.
- Task 5E now provides a backend-owned governed-intake ClaimLedgerItem-plus-ReviewEvent to CanonicalWrite boundary integration skeleton.
- Task 5E reads only exact organization-scoped `ClaimLedgerItem` and `ReviewEvent` rows through narrow lookup ports, validates that the ReviewEvent belongs to the ClaimLedgerItem, requires Task 5C/5D governed-intake `intake.*` lineage, and calls only `CanonicalWriteService`.
- Task 5E keeps `CanonicalWriteGate` mandatory and does not bypass it. Current Task 5C low-authority governed-intake claims remain blocked by the existing gate rather than promoted to fact.
- Task 5E allowed boundary attempts, where gate-allowable governed-intake-lineage fixtures are used, append only the existing `WorkflowEvent` audit from `CanonicalWriteService` and report `canonicalPersistencePerformed=false` unless a later explicit CandidateProfile target is supplied.
- Task 6D extends that bridge with an optional explicit CandidateProfile target that is passed to `CanonicalWriteService`; the bridge still does not call lower-level CandidateProfile persistence directly.
- Task 5E/6D do not mutate ClaimLedger verification status, do not mutate ReviewEvent, do not write raw Candidate/Profile persistence, do not query business target entities, and do not implement API/UI exposure.
- Task 5E adds no new migration, table, index, or API-facing view. It relies on existing `governance.claim_ledger_item`, `governance.review_event`, and `workflow.workflow_event` audit/idempotency behavior.
- Task 5E duplicate behavior is deterministic for allowed boundary audits through existing WorkflowEvent idempotency. Gate-blocked attempts append no audit row under the current CanonicalWriteService design, so there is no DB-enforced blocked-attempt ledger yet.
- Task 5F now regression-covers the full safe minimal chain from `SourceItem` / `InformationPacket` through deterministic extraction output envelope, ClaimLedgerItem claim, ReviewEvent evidence, CanonicalWriteService boundary attempt, CanonicalWriteGate decision, and no canonical persistence.
- Task 5F verifies default placeholder output appends no business ClaimLedger claims, bridge-eligible fixtures append claims but not facts, ReviewEvent remains evidence rather than fact promotion, CanonicalWriteGate is mandatory, allowed boundary fixtures still report `canonicalPersistencePerformed=false`, and blocked canonical attempts had no separate persisted audit ledger at Task 5F time (resolved by Task 17 V11 `governance.canonical_write_attempt`).
- Task 6D adds the first minimal allowed canonical write beyond Task 5F: allowed governed-intake fixtures with an explicit existing CandidateProfile target write one field, while default low-authority placeholder claims remain blocked and non-persistent.
- Task 6E keeps that write single-field and gated, while preserving claim, review, workflow, and governed-intake source-span lineage in CandidateProfile field metadata.
- These Task 5A `intake.*` governed-intake operational records coexist with earlier V2 skeleton schema artifacts: `recruiting.source_item` and `recruiting.information_packet`.
- `SourceItem` and `InformationPacket` are intake/provenance records, not canonical facts.
- Neither the Task 5A `intake.*` table family nor the earlier V2 `recruiting.*` source/packet table family is canonical fact storage, CandidateProfile persistence, ClaimLedger, or a canonical profile.
- For the Task 5C, Task 5D, and Task 5E bridges, `intake.*` is the operational governed-intake source. Earlier `recruiting.source_item` and `recruiting.information_packet` remain V2 skeleton artifacts and are not read or written by these bridges.
- Future cleanup, deprecation, or migration of the earlier `recruiting.*` source/packet skeleton remains a schema cleanup gap.
- Governed AI extraction now exists for the Task 23 backend/API slice through `GOVERNED_AI_V1`, but it is still constrained to the governed-intake pipeline rather than a broad AI product surface.
- Full canonical persistence from governed intake remains future work beyond the Task 6D/6E minimal single-field path.
- No default-placeholder business ClaimLedger append from intake exists.
- Governed intake CanonicalWrite boundary attempts can now perform the Task 6D minimal field write only with an explicit CandidateProfile target after gate allow.
- No CandidateProfile persistence exists from intake outside the Task 6D gated CanonicalWriteService path.
- Candidate publish now fails closed unless an existing candidate/profile target is supplied; the current Task 23 candidate path supports only the mapped stable AI keys that land on canonical CandidateProfile field paths (`profile.headline`, `profile.summary`, `skills.primary_skills`, `experience.projects`, `experience.timeline_highlights`), requires matchable evidence quotes during extraction, and company/job publish remains blocked until a governed canonical write plus audit path is designed.
- Consultant intake API exposure now exists for backend/API scope (`extract`, `review`, `decide`, `publish`), but no frontend review UI exists yet.
- No Consent/Disclosure, RBAC/ABAC, Client-safe projection API/UI, redaction pipeline, unlock/disclosure, or client exposure exists for governed intake.
- Task 5 Governed Intake Minimal Slice is closed as a safe, regression-covered backend chain. Task 6F closes one gated CandidateProfile field write and metadata regression coverage; downstream privacy/access surfaces, full profile behavior, conflict resolution, stale detection, and `recruiting.*` source/packet cleanup remain future work.

## Minimal Client-safe Projection Service Exists; Product Privacy Pipeline Still Deferred

- Raw Candidate must never be exposed to Client.
- `ClientSafeCandidateCard` exists as a backend contract after Task 7A.
- L0-L4 redaction vocabulary exists as policy vocabulary after Task 7A.
- Task 7B adds a minimal backend projection service/read model that returns only `ClientSafeCandidateCard`.
- Task 7B does not add client-facing serialization, API, controller, UI, RBAC/ABAC, Consent, Disclosure, Unlock, identity disclosure, database migration, or AI/model wiring.
- Task 7C adds only a deterministic placeholder for re-identification assessment; it does not perform real scoring.
- No real redaction implementation exists yet.
- Real re-identification risk scoring does not exist yet.

## Task 8 Identity Access Kernel Exists for Current Backend Scope

- Task 8A adds backend-only role vocabulary for Owner, unified Consultant, Client, Candidate, Admin, System, and AI assistant.
- Task 8A adds explicit resource/action vocabulary and field-classification policy contracts for deny-by-default evaluation.
- Task 8A adds `AccessRequest`, `AccessDecision`, relationship-scope, `FieldAccessPolicy`, and `PermissionEvaluator` skeletons.
- The evaluator is deterministic and has no database, external service, Spring Security, API/controller, login/session, or real-user dependency.
- Client raw `Candidate` and raw `CandidateProfile` reads remain denied.
- Client unsafe field classifications remain denied, including PII, raw source, consultant-private, internal audit, and consent/disclosure records.
- Client can read only `CLIENT_SAFE_CANDIDATE_CARD` at `CLIENT_SAFE` / `GENERALIZED` field levels in this skeleton.
- Candidate profile reads require explicit `SELF` relationship scope and are limited to safe/generalized field levels in this skeleton.
- Admin, System, and AI assistant roles do not bypass canonical-write or disclosure gates by role alone.
- Task 8B adds `PermissionEnforcer` / `AccessDeniedException` as a backend-only fail-closed service guard that preserves `AccessDecision` reason codes and safe explanations.
- Task 8B updates `ClientSafeCandidateProjectionService` so projection requires an explicit `AccessRequest` before a `ClientSafeCandidateCard` can be returned.
- Task 8B adds a minimal `CandidateProfileAccessService` facade/guard for raw Candidate/Profile reads and sensitive candidate actions before any raw CandidateProfile service delegation.
- Task 8B proves Client raw Candidate/Profile access, PII/raw-source/consultant-private field access, L4 anonymous projection, high-risk re-identification projection, Client disclose/unlock attempts, and AI/Admin/System role-alone bypass attempts are denied at backend service boundaries.
- Task 8C adds five-portal boundary negative regression coverage for Owner, unified Consultant, Client, Candidate, Admin, System, and AI assistant.
- Task 8C proves deny-by-default and no role-based gate bypass across raw Candidate/Profile reads, unsafe field classifications, sensitive actions, identity-disclosed/L4 anonymous access, canonical-write-like requests, disclosure/unlock requests, unknown vocabulary, and guarded service facades.
- Task 8C proves `ClientSafeCandidateCard` remains the only Client-readable candidate-facing output at this layer, while raw Candidate and raw CandidateProfile remain denied to Client.
- Task 8 is complete only for the current backend kernel scope: role/resource/action/field policy contracts exist, deterministic `PermissionEvaluator` exists, `PermissionEnforcer` exists, a sensitive backend guard slice exists, and five-portal boundary negative tests exist.
- Real auth/login/session baseline now exists. Spring Security and JWT are now the enforcement source for product controllers, with Task 19C adding strong session checks before principal establishment.
- No API/controller/UI exists for this access layer.
- No Consent/Disclosure/Unlock product workflow exists beyond the current backend-only Task 12A/12B/14 policy, persistence, service, and hardening kernel.
- No identity-disclosed Client access behavior exists.
- No complete product-wide RBAC/ABAC enforcement exists beyond the Task 8B/8C backend guard surfaces and regression tests.

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
- Task 5 governed-intake minimal slice exists, but the broader Governed Intake workflow engine remains future work.

## Workflow Read Model Remaining Gaps

- Task 4C adds a backend-internal, read-only audit query/read model for `WorkflowEvent`.
- It is not an API/controller layer.
- It is not UI integration.
- It is not a client-safe projection.
- It does not expose raw Candidate/Profile payloads or business entity internals.
- It does not join Candidate, Company, Job, Consent, Disclosure, Placement, or Commission tables.
- It is not dashboard analytics, full reporting, full-text search, generic repository search, or arbitrary SQL filtering.
- Correlation and causation identifiers are queryable for backend audit lineage, but no user-facing timeline/query API exists yet.

## Task 9 API Boundary Slice Exists; Broad API Deferred

- Task 9A adds a minimal backend `apiboundary` package with API-safe response envelope bounded to API-safe response bodies, error/access-denied/validation response DTOs, a client-safe candidate card response DTO, a mapper from `ClientSafeCandidateCard` only, and contract rules for safe field allowlisting and error text sanitization.
- Task 9A contract tests prove the client-safe API DTO contains only anonymous/generalized/client-safe fields and does not contain raw Candidate, CandidateProfile, SourceItem, InformationPacket, ClaimLedgerItem, ReviewEvent, WorkflowEvent, raw candidate/profile ids, PII, raw source fields, consultant notes, or L4 identity-disclosed fields.
- Task 9A access-denied response tests prove denial DTOs preserve safe reason codes while avoiding stack traces, raw ids, internal entity details, and unsafe exception text.
- Task 9B adds the first minimal controller boundary: `GET /api/client-safe/candidate-cards/{anonymousCardRef}` reads by anonymous `card_` reference, was introduced with explicit temporary access-context headers, delegates to a safe query facade/port returning `ClientSafeCandidateCard`, and maps only to `ClientSafeCandidateCardResponse` inside the API-safe envelope. The active controller path now authenticates through Spring Security/JWT after Task 19B/19C.
- Task 9B tests prove successful responses do not contain raw candidate/profile ids, full name, email, phone, LinkedIn URL, raw source text, consultant notes, exact employer, exact project/product/chip name, L4 identity fields, or raw internal entity/governance types; missing/denied/non-client/identity-disclosed access fails closed with sanitized responses; raw UUID path refs are rejected; no raw Candidate/Profile endpoints exist.
- Task 9C adds API regression closure for the current backend kernel scope: anonymous-card-only request paths, raw id rejection, fail-closed access context handling, missing/unknown/unsupported context denial, sanitized denied/not-found/internal-error envelopes, successful DTO/envelope-only responses, reflection/source checks for controller/facade/port/mapper boundaries, public DTO/error text leakage checks, and endpoint-surface checks.
- Task 9 is complete only for the current backend kernel scope: API-safe DTO/envelope contracts exist, the client-safe candidate-card controller boundary exists, the authenticated access context is fail-closed, sanitized API error/denial responses exist, and API leakage regression tests exist.
- Task 13A adds the route-aware five-portal shell while preserving Consultant as one unified portal, keeping the v2.0/v2.1 portal taxonomy intact, and exposing only a narrow Client portal entry flow for anonymous client-safe candidate cards. It adds fail-closed client-safe loading states and a typed frontend helper for the existing endpoint, but it does not add raw Candidate/Profile client exposure, identity-disclosed client read behavior, auth/session/Spring Security, or backend-truth drift.
- Task 13B adds a real backend-internal PostgreSQL query/read-model implementation behind the existing client-safe candidate-card endpoint. It reads only safe projection metadata from `recruiting.candidate_profile`, reuses the existing projection and re-identification boundaries, and fails closed for missing/ambiguous/invalid/unsafe data.
- Task 14 hardens the backend Consent / Disclosure persistence and service path only. It preserves fail-closed L3/L4 separation, binds approved disclosure records to the requested consent/unlock chain, makes final disclosure persistence retry-safe, adds organization-scoped linkage hardening in `V9`, and denies legacy cross-organization unlock approvers at runtime.
- Only the existing client-safe candidate-card read endpoint exists; no raw Candidate/Profile API endpoints, broad REST API, or general API runtime layer exists yet.
- Broad frontend product UI, Consent/Disclosure/Unlock product workflow, and identity disclosure workflow still do not exist here. Spring Security/auth/login/session now exist, and product controllers now enforce JWT-backed identity rather than temporary header context.

## UI / AI / Access Boundaries Not Implemented

- No UI integration exists for WorkflowEvent audit guardrails.
- No broad real AI model wiring exists for workflow actions outside the current governed-intake task path.
- API/controller integration now exists for governed intake through `ConsultantIntakeController`, but no governed-intake frontend UI exists yet.
- No API/controller/UI integration exists for CandidateProfile.
- No Consent/Disclosure API/controller/UI or broad workflow behavior exists beyond the current backend-only Task 12A/12B/14 kernel.
- No broad service-level RBAC/ABAC enforcement exists beyond the Task 8B/8C minimal projection/raw CandidateProfile guard surfaces and five-portal boundary tests.
- No broad client-safe product UI or real redaction behavior exists. Task 13A adds only a narrow route-aware portal shell plus anonymous client-safe candidate-card flow, and Task 9B/13B together provide only the existing narrow endpoint behind it.
- No full governed-intake or CanonicalWriteService-driven CandidateProfile implementation exists beyond the narrowed Task 23 existing-target candidate publish path and Task 6E metadata hardening; broad profile CRUD/update surfaces still do not exist.

## Task 16 Product Data Model Baseline Complete; DB Org-Scope Hardening RESOLVED (V12)

- **RESOLVED** by Task 16-Hardening V12 migration (`V12__harden_product_data_model_org_scope.sql`).
- V12 adds UNIQUE (id, organization_id) on 7 parent tables: `recruiting.company`, `recruiting.job`, `recruiting.candidate`, `recruiting.candidate_profile`, `recruiting.shortlist`, `recruiting.placement`, `recruiting.candidate_company_interaction`.
- V12 adds 19 composite FOREIGN KEY (parent_id, organization_id) REFERENCES parent (id, organization_id) across all child tables with NOT NULL parent FK columns.
- 19 redundant simple FKs from V10 are dropped (composite FKs fully subsume them).
- 8 cross-org negative integration tests added proving the database rejects cross-org child inserts independently of service-layer checks.
- Remaining gaps (not covered by V12): nullable FK columns (`interaction.job_id`, `document.source_item_id`) and cross-cutting identity FK (`commission.consultant_id`) intentionally excluded. These do not pose the same org-isolation risk and are documented as design decisions.

## Task 17 Canonical Write Attempt Ledger Complete; Idempotency and FK Hardening Deferred

- Task 17 V11 migration added `governance.canonical_write_attempt` to record all canonical write decisions (allow/block/require_review) with decision type, reason codes, field path, entity type/id, actor, idempotency key, correlation/causation ids, and timestamps.
- `CanonicalWriteAttemptPort` / `JdbcCanonicalWriteAttemptPort` provide append/read persistence. `CanonicalWriteService` now persists an attempt record for every decision type. `CanonicalWriteResult` carries `canonicalWriteAttemptId` for downstream audit linkage.
- This resolves the long-standing gap where blocked canonical attempts had no separate persisted audit ledger.
- Idempotency currently returns the existing attempt on key match without verifying payload equivalence. If a caller replays the same idempotency key with different command payload, the service returns the original attempt id without detecting the mismatch. Future hardening: add an idempotency equivalence hash or command fingerprint to detect replay payload drift.
- V11 `governance.canonical_write_attempt` columns `claim_ledger_item_id`, `review_event_id`, and `workflow_event_id` are ref-only uuid columns without `REFERENCES` constraints (intentional loose ledger design for now). These columns record linkage for audit/query purposes but do not enforce referential integrity at the DB level. Future hardening: document the FK-free design decision explicitly, or add optional composite FKs if referential integrity becomes operationally necessary.

## Task 18A Consultant Read API Layer Complete; Write and Client-Side API Deferred

- Task 18A adds the first v1 product API layer for Consultant read access to companies, jobs, and shortlists through three read-only REST controllers (`ConsultantCompanyController`, `ConsultantJobController`, `ConsultantShortlistController`).
- Generic offset-based pagination infrastructure (`PagedQuery` with builder pattern, DEFAULT_LIMIT=20, MAX_LIMIT=100; sealed `PagedResult<T>`) exists and is reusable by future controllers.
- Six consultant response DTOs exist: `ConsultantCompanySummaryResponse`, `ConsultantCompanyDetailResponse`, `ConsultantJobSummaryResponse`, `ConsultantJobDetailResponse`, `ConsultantShortlistSummaryResponse`, `ConsultantShortlistDetailResponse`. All are sealed as `ApiSafeResponseBody` permits.
- `ResourceType.SHORTLIST` is added to the access-control vocabulary.
- `FieldAccessPolicy` now allows CONSULTANT role READ on COMPANY, JOB, and SHORTLIST resource types. Consultant access to all other resource types remains denied by default.
- `ApiBoundaryContractRules` contains explicit allowlists for all six consultant response types, with public accessor methods for field-name validation.
- `ConsultantApiQueryService` serves as the single facade for consultant reads, enforcing `PermissionEnforcer.requireAllowed()` before delegating to domain services and mappers.
- Leakage and denial `@WebMvcTest` coverage exists for all 6 endpoints, proving:
  - Unauthenticated/wrong-role/invalid-request cases fail closed under JWT-backed controller enforcement.
  - Successful responses return only allowlisted fields with no internal entity leakage.
  - Not-found returns sanitized 404; invalid UUID returns sanitized 400.
  - No raw Candidate, CandidateProfile, PII, internal entity types, stack traces, or internal package names leak through response bodies or denial messages.
- Task 18A is complete only for Consultant read-only access to companies, jobs, and shortlists. No Client-safe candidate projection read endpoints exist through the product API layer. No Client portal product API endpoints exist. No filtering beyond optional status (list) and optional companyId/jobId (job/shortlist lists) exists. No full-text search exists. No shortlist candidate card detail with generalized content exists. No composite FK org-scope hardening at DB level for Company/Job/Shortlist child tables has been added. Real auth/login/session/Spring Security now exists, the consultant product API uses JWT-backed `SecurityContext`, and Task 19C closes the baseline auth/session hardening slice.

## Task 18B (Partial) Consultant Write Endpoints for Company and Job

- Task 18B (partial) adds CREATE and UPDATE operations for Company and Job through the Consultant API boundary.
- `CompanyPersistencePort.update()` and `JobPersistencePort.update()` with optimistic-locking JDBC implementations (`WHERE organization_id = ? AND version = ?`, `SET version = version + 1`).
- `CompanyService.updateCompany()` and `JobService.updateJob()` domain service methods with null-safety guards.
- `FieldAccessPolicy.decideConsultantAccess()` extended to allow CREATE and UPDATE on COMPANY and JOB resources, in addition to existing READ.
- Four request DTOs: `CompanyCreateRequest`, `CompanyUpdateRequest`, `JobCreateRequest`, `JobUpdateRequest` with compact canonical-constructor validation through `ApiBoundaryContractRules.requireNonBlank()`. Update requests include a required `version` field (>= 1) for optimistic locking.
- `ConsultantApiCommandService` facade with `PermissionEnforcer`-based access enforcement, domain-object assembly from request DTOs, and response mapping through existing `ConsultantCompanyResponseMapper`/`ConsultantJobResponseMapper`.
- `@PostMapping` and `@PutMapping` endpoints on `ConsultantCompanyController` and `ConsultantJobController`.
- `HttpMessageNotReadableException` handler added to both controllers for invalid request body deserialization (returns 400).
- `ApiBoundaryRegressionClosureTest` updated to allow POST/PUT only on consultant company/job controllers while still blocking PATCH/DELETE on all controllers.
- 14 new @WebMvcTest write-operation cases in `ConsultantControllerLeakageTest` covering role/org header enforcement, success paths (201/200), and invalid payload (400).
- 5 new PostgreSQL/Testcontainers integration tests in `ConsultantWriteOrgIsolationIntegrationTest` proving organization-scoped isolation (cross-org read/update denied) and optimistic locking (wrong version fails, correct version succeeds and increments).
- Full Maven backend reached 622 tests, 0 failures/errors, 1 existing skip.
- Remaining gaps: no DELETE endpoints, no Shortlist write endpoints, no Client-portal write endpoints, no Candidate/CandidateProfile write endpoints, no batch operations.
