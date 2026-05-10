# Known Gaps

## Task 59 Onboarding Playbooks Closed; Public Launch Readiness Still Deferred

- Task 59 now packages customer onboarding, consultant training, client
  training, candidate consent FAQ, admin setup, data import, risk review, and
  go-live checklist material for repeatable controlled-pilot onboarding.
- The playbooks preserve the product boundary: AI assists intake, matching,
  follow-up, and workflow coordination, while humans and backend service gates
  own facts, consent, disclosure, commercial terms, and audit-sensitive state.
- Remaining gaps:
  - Public SaaS onboarding still depends on remaining production tasks and
    release acceptance.
  - Real integrations remain Task 49 scope.
  - Governed real customer import remains Task 55 scope.
  - Support tooling, report/legal-audit packages, and release gates remain
    Tasks 56-58 scope.

## Task 54 Performance/Load/Cost Targets Closed; Live Performance Evidence Still Deferred

- Task 54 now defines latency, throughput, and AI cost target envelopes for
  pilot and expected production workloads.
- `PerformanceCostPolicies` and
  `scripts/performance/task54_performance_load_cost_harness.py` provide a
  deterministic bounded harness for budget-policy evidence.
- The harness passes with `PASS failures=0`; expected-production
  `interview-feedback-structurer` cost is intentionally classified as `WATCH`
  because projected monthly cost is near budget.
- Remaining gaps:
  - No deployed API p95/p99 evidence exists yet.
  - No stable browser timing evidence exists yet.
  - Provider billing integration is still required to convert provider-neutral
    `costUnits` into actual money.
  - Expected-production rows remain capacity-planning assumptions, not staging
    load-test proof.

## Task 53 DR/BCP Baseline Closed; Managed Production DR Still Deferred

- Task 53 now records backup schedule, local restore drill evidence, migration
  rollback invariants, document/object recovery, AI provider outage playbook,
  notification provider outage playbook, and incident severity levels.
- The local restore drill recovered a PostgreSQL dump and document archive into
  an isolated database/document root, validated pilot data, and booted the API
  against the restored database with health check success.
- Remaining gaps:
  - No managed cloud database backup policy has been executed.
  - No managed object storage versioning or cross-region replication has been
    configured.
  - No real production email/SMS/WeChat failover has been executed.
  - No production AI multi-provider failover has been executed.
  - No public production incident communications process has been tested.

## Task 52 Security Compliance Baseline Closed; Certification and Extended Controls Still Deferred

- Task 52 now records the production security compliance baseline: threat
  model, access review, privacy/data-retention runbook, key/secret rotation
  runbook, dependency vulnerability remediation, pen-test issue remediation,
  security regression suite, and issue register.
- The baseline includes regression coverage through
  `SecurityComplianceBaselineDocumentationTest` and records dependency-scan
  closure evidence from the Task 52 branch.
- Remaining gaps:
  - This is not a SOC 2 report, ISO certification, or public penetration-test
    attestation.
  - MFA, SSO/OIDC, persistent lockout, password reset, and email verification
    remain deferred.
  - Distributed/gateway rate limiting remains deferred.
  - Product-wide field-level access audit rollout remains deferred.
  - Exact production AI prompt/model-output retention windows remain a
    governance dependency before public SaaS launch.

## Task 48 Commercial Finance Hardening Closed; Accounting Replacement Still Deferred

- Task 48 now hardens the placement-to-paid lifecycle with fee agreement
  snapshots, invoice readiness gates, invoice sent/paid ordering, guarantee
  state enforcement, commission calculation inputs, Owner revenue reporting,
  and read-only accounting export handoff.
- The accounting export is explicitly a CSV handoff and does not replace the
  official accounting system.
- Remaining gaps:
  - No invoice issuing, payment collection, tax handling, GL posting, or
    accounting-system integration exists.
  - Broad export/legal-audit packages remain Task 57 scope.
  - Support actions remain Task 56 scope.
  - Tenant-wide finance hardening remains Task 51 scope.

## Task 44 AI Task Registry Coverage Closed; Full AI Orchestration Still Deferred

- Task 44 now gives all 28 v2.1 production AI task definitions a governed
  registry entry with task id, version, prompt version, prompt/schema/eval
  resources, human-review policy, write-back target policy, and model route
  inspection.
- Admin `/admin/ai-task-registry` now lists production definitions even before
  any run history exists, and includes aggregate run cost, latency, failure,
  eval result registration, and replay history counts when AITaskRun history is
  present.
- Remaining gaps:
  - Registry-only tasks are still not broad executable business orchestrators.
  - No worker queue, async scheduling, retry dashboard, or product-wide AI
    operation console is added by Task 44.
  - Broad governed write-back execution remains deferred to later workflow,
    data-quality, and final acceptance tasks.

## Task 43 Portal Route Depth Closed; Production Readiness Still Deferred

- Task 43 now has a frontend route contract for the v2.0/v2.1 named Owner,
  Consultant, Client, Candidate, and Admin route sets.
- Client route gaps after Task 42 are closed with backend-connected pages or
  aliases for dashboard, profile, AI-intake job submission, job-scoped
  shortlist continuity, unlock request continuity, and follow-ups.
- Candidate route gaps are closed with upload, AI review, and status aliases
  mapped to the existing document/profile/timeline backend surfaces.
- Strict route parameter names are now aligned with the spec for client
  anonymous candidate review and candidate opportunity/consent detail routes;
  existing compatibility aliases remain available where already present.
- Admin `/admin/integrations` is now present in the portal and reads from the
  Admin governance API boundary.
- Remaining gaps:
  - Task 43 is route-depth and workflow-continuity closure, not full production
    integration implementation.
  - Remaining Tasks 49-51, 55-58, and 60 still need integrations,
    governance/eval console depth, multi-organization hardening, import and
    migration, support workflows, reporting/export/legal audit packages,
    release management, and final full-product acceptance.

## Task 42 Pilot E2E Acceptance Gate Passed; Production Readiness Still Deferred

- Task 42 now has an explicit backend acceptance-gate model and report artifact
  that tracks 8 pilot flows, 10 negative gates, and 8 validation/operations
  gates.
- Current result is `CONTROLLED_PILOT_READY` for the Task 42 Usable v1 gate;
  partial unit, controller, and integration coverage is no longer treated as
  pilot readiness.
- A Playwright harness now proves five Task 38 seed accounts can sign in through
  the real Consultant, Client, Candidate, Owner, and Admin portal UIs against a
  live API/web stack.
- The same harness now proves S01-S08 business flows through normal product UI
  and API paths.
- Task 38 pilot data rebuild, validate, export, and guarded reset commands have
  current Task 42 execution evidence.
- Task 39 backup / restore has current Task 42 evidence, including a post-E2E
  business-state restore with API health/document availability and a clean-seed
  restore whose `pilot:data:validate` check passed.
- Remaining gaps:
  - Task 42 does not certify public production operation, managed cloud
    deployment, HTTPS/domain setup, production incident process, MFA/SSO, or
    product-wide security certification.
  - Remaining Tasks 49-51, 55-58, and 60 still need to broaden integrations,
    governance/eval console depth, multi-organization hardening, import and
    migration, support workflows, reporting/export/legal audit packages,
    release management, and final full-product acceptance.

## Task 41 Security and Privacy Hardening v1 Baseline Exists; Production Security Still Deferred

- Task 41 adds a controlled-pilot backend hardening baseline for login input
  policy, auth/document rate limiting, unsafe upload filename rejection,
  UUID/email URL-path masking in request logs, explicit Admin disclosure-audit
  export permission, persistent access audit for Task 41 sensitive
  document/export surfaces, data-retention policy baseline, vulnerability scan
  baseline docs, pinned Maven dependency-check configuration, and focused
  privacy/security regressions.
- Remaining gaps:
  - No MFA, password reset, email verification, SSO/OIDC, account lockout
    persistence, or multi-organization membership switching.
  - Rate limiting is in-memory and per-node, not distributed or gateway-level.
  - No product-wide field-level access audit exists beyond the Task 41
    sensitive document/export surfaces.
  - No complete product-wide PII log audit proves all existing loggers are safe.
  - No automated data-retention/deletion executor exists.
  - No formal dependency vulnerability remediation report or penetration test
    exists.
  - No production malware scanner is shipped by this task.

## Task 40 Observability v1 Baseline Exists; Product-wide Operations Still Deferred

- Task 40 adds request correlation middleware for `/api/**`, including safe
  `X-Request-Id` preservation/generation, response headers, MDC propagation, and
  safe request log fields.
- Staging and production profiles now use key-value structured log patterns for
  timestamp, level, service, request id, organization id, actor role, error
  code, logger, and sanitized message text.
- Admin-only observability APIs now expose safe WorkflowEvent search,
  ReviewEvent search, AITaskRun trace/search, and disclosure audit export.
- AITaskRun trace/search returns safe task/model/prompt/schema/cost/latency/
  replay metadata without reading or returning raw input/output/tool payloads.
- Disclosure audit export reports explicit `missing_*` reason codes when
  records or links are absent instead of inferring facts, and includes safe
  consent version/status plus unlock approver metadata when those records are
  explicitly linked.
- Remaining gaps:
  - No external observability vendor, OpenTelemetry collector, Prometheus
    backend, or distributed tracing exists.
  - No frontend dashboard exists for error summaries, AI cost/latency summaries,
    or incident exploration.
  - No product-wide log audit proves every existing logger masks PII; Task 40
    only verifies the new request-correlation and observability surfaces.
  - No owner raw audit export exists; owner-facing observability should remain
    aggregated unless a later task defines safe owner DTOs.
  - Disclosure requester metadata is not yet linked by a dedicated safe read
    model; Task 40 reports `missing_requester_link` rather than guessing from
    adjacent events.
  - PostgreSQL/Testcontainers coverage for the new observability JDBC readers is
    still narrower than the full plan; existing WorkflowEvent/AITaskRun/
    ReviewEvent/Disclosure persistence suites remain the deeper persistence
    coverage.

## Task 19A/19B/19C Auth Baseline, Controller Migration, and Hardening Complete; Extended Identity Features Deferred

- Task 19A adds V15 auth persistence baseline through `identity.user_account.password_hash` and `identity.session`.
- Spring Security now exists with a stateless filter chain, JWT issuance/validation, persisted refresh-token-backed session lifecycle, and `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`.
- Current scope now includes JWT-backed product-controller enforcement. Consultant/client-safe/document product endpoints read authenticated identity from Spring Security rather than temporary role/org headers.
- Remaining gaps:
  - No multi-organization membership/session switching exists yet; `identity.user_account` remains directly organization-scoped in this baseline.
  - No SSO/OIDC/external identity provider integration exists yet.
  - No password reset, MFA, email verification, lockout, or auth-audit hardening flow exists yet.

## Task 18C Consultant Shortlist CRUD + Sub-entity CREATE Complete; UPDATE/DELETE and Broader Write Breadth Deferred

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
  - Client portal write/read endpoints now exist through Tasks 25 and 32; the remaining gap is broader portal/API breadth, not total absence of client endpoints.
  - No Candidate/CandidateProfile write endpoints.
  - No batch operations.
  - No filtering/search on write responses.
  - Real auth/login/session/Spring Security now exists, product endpoints are on JWT-backed `SecurityContext`, and Task 19C closes the current baseline hardening slice; remaining auth work is now longer-horizon identity capability backlog.

## Task 29 Shortlist Builder v1 Complete; Client Review Complete, Delivery Execution Deferred

- Task 29 closes the first consultant-side shortlist builder/send slice on top of Task 18C shortlist CRUD and Task 27 match reports.
- `ShortlistBuilderService` now composes shortlist state, shortlist cards, pre-send checks, and delivery preview placeholders.
- Consultant shortlist commands now include candidate-card create/update and explicit send-to-client approval through:
  - `ConsultantShortlistController`
  - `ConsultantApiCommandService`
  - `ShortlistCandidateCardCreateRequest`
  - `ShortlistCandidateCardUpdateRequest`
  - `ShortlistSendRequest`
- `ConsultantShortlistDetailResponse` now includes client-safe shortlist card comparison data, pre-send checks, and delivery preview placeholder fields.
- Consultant portal shortlist UI now supports candidate selection, card updates, comparison display, pre-send checks, preview text, and explicit send action.
- Workflow audit coverage now exists for shortlist draft creation, ready-for-review promotion, return-to-draft rollback, candidate shortlisted, shortlist card remove/restore, shortlist send, and job shortlist progression.
- Consultant workflow timeline/audit drawer now renders shortlist card composition transitions from `cardStatus` when the top-level shortlist status is unchanged, so remove/restore events no longer collapse into no-op-looking `draft -> draft` labels.
- Remaining gaps:
  - Client portal shortlist review, unlock request capture, and shortlist feedback now exist through Task 32; final identity disclosure release and approval workflow still remain Task 33 scope.
  - No real PDF/email/WeChat delivery execution yet; Task 29 only ships safe preview/placeholder content and status progression.
  - Task 30 hardening now routes shortlist send re-evaluation and the audited client-safe query path through `RedactionAuditService`; real outbound delivery execution still remains deferred.

## Task 30 Privacy Redaction and Re-identification v1 Complete; Calibration and Admin Tooling Deferred

- Task 30 closes the privacy-kernel slice of the v2.1 anonymity-by-default invariant.
- New baseline:
  - `privacy.reidentification_risk_assessment` table (V25) with composite PK `(organization_id, reidentification_risk_assessment_ref)`, score range CHECK constraint, optional FK to `workflow.workflow_event`, and dual indexes on `(org, candidate_card_id, recorded_at)` and `(org, decision, recorded_at)`.
  - V25 also extends the `workflow.workflow_event` namespace allowlist to include `'privacy'`.
  - Three new generalization policies in `clientsafeprojection`:
    - `CompanyNameGeneralizationPolicy` with curated semiconductor / chip / cloud-hyperscaler vocabulary.
    - `ProjectChipNameRedactionPolicy` with declared exact name redaction + known chip-family pattern set + chip-context-gated generic shape detector.
    - `RareTitleYearCombinationRiskRule` with chief / head-of / founding / CTO-CEO etc. vocabulary + four-digit year detector.
  - `ClientSafeSummaryPipeline` orchestrates all three across every projected text field and returns redaction explanations.
  - `ReidentificationRiskAssessmentService` now produces a deterministic risk score in `[0.0, 1.0]` and exposes `assessWithPipeline(...)` combining redaction + assessment.
  - New `privacyredaction` package with `ReidentificationRiskAssessmentPort` + `JdbcReidentificationRiskAssessmentPort`, `PersistedReidentificationRiskAssessment`, `RedactionAuditService` (orchestrator), `RedactionAuditWorkflowEntityIds`, and `PrivacyRedactionConfiguration`.
  - Two new workflow action codes: `REIDENTIFICATION_RISK_ASSESSED` (T2 audit-only) and `CLIENT_SAFE_REDACTION_BLOCKED` (T2 audit-only). New entity type `REIDENTIFICATION_ASSESSMENT`.
  - The spec acceptance scenario "top chip company + unique title + exact year + chip code name" is covered end-to-end by `RedactionAuditPostgresIntegrationTest` (Testcontainers Postgres, V1-V25 applied) and proves BLOCK + HIGH risk + employer/chip generalization + per-organization scope.
- Current completed baseline:
  - `RedactionAuditService` is now wired into `ShortlistBuilderService` send-time re-evaluation and the Spring-managed audited client-safe query path, so both surfaces persist `privacy.reidentification_risk_assessment` rows and emit workflow audit events before client delivery.
  - The assessment/workflow-event write path now runs inside a dedicated Spring transaction boundary, and persisted assessments store the linked `workflow_event_id`.
- Remaining gaps:
  - The curated company / chip vocabulary is intentionally narrow; v1 prioritizes the acceptance scenario. Production rollout will need a maintained vocabulary pack — likely as part of the industry-pack baseline (Task 28 follow-up).
  - The risk score is hand-curated weights, not a learned classifier. v1 chose policy-as-code for explainability and audit; a future task may introduce calibrated scoring once outcome-label feedback is available.
  - Task 37 adds the first Admin governance UI and API surface, including privacy-redaction visibility inside the real Admin portal, but deeper assessment browsing, richer filtering, and dedicated persisted-assessment re-query tooling still remain follow-up work.

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
  - No client-safe evidence exposure yet.
  - Task 23 backend/API plus Task 24 now add governed AI extraction, clean-fact generation, and the first consultant-facing upload/review/publish UI, but broader productization still remains: no async worker orchestration, no automatic reviewer workflow, and no broader company/job canonical publish path.
  - No automatic human review workflow or broader AI product integration yet. Task 37 now adds the first owner/admin governance API/UI and admin-side AI governance workbench surface, but deeper runtime execution control, richer task-level replay tooling, and automatic reviewer orchestration remain future work.
  - No multi-provider product routing; the current real provider baseline is still DeepSeek only.

## Task 11 Matching / Evidence Kernel Closed for Backend Scope; Task 28 Seeds One Real Industry Pack; Broader Matching Engine Still Deferred

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
- Task 11 remains the matching/evidence kernel foundation, and Task 27 now closes the first consultant-internal productization slice: MatchReport persistence exists, consultant matching GET/POST API and portal workspace exist, backend-owned assembly now uses JobRequirement/JobScorecard/CandidateProfile/active parsed documents/authenticity-task output, and regression tests prove no client-safe leakage or canonical mutation from this slice.
- Task 28 now adds the first real industry ontology calibration slice:
  - persisted `industry_pack`, `ontology_version`, `skill_concept`, and role-family template data
  - one real seeded pack: `semiconductor`
  - consultant job-level pack selection
  - real pack key/maturity/selection-reason metadata on stored match reports
  - deterministic semiconductor anti-pattern downgrade behavior for at least DV/Verification, Physical Design, and DFT confusion cases
  - post-review hardening preserves legacy job updates without pack erasure and keeps historical `match_report` pack metadata, `ontologyStale`, and legacy `PARTIAL` coverage semantics truthful instead of fabricating defaults
- Task 47 expands the industry-pack baseline from one real pack to all eight
  v2.1 packs. `semiconductor` is now marked `production`; `general`,
  `finance`, `healthcare`, `internet_ai`, `sales`, `executive_search`, and
  `manufacturing` are seeded with gold cases, negative cases, anti-patterns,
  score caps, drift signals, active ontology versions, role templates, and
  Admin review-queue visibility.
- Broader gaps remain: no real learned AI matching, no learned calibration
  executor, no automatic ontology update from outcome labels, no admin
  industry-pack editing UI, no client-facing match report delivery, and no
  Task 42 browser E2E proof for the full match-to-shortlist-to-disclosure
  release path. Task 33 adds the first identity-disclosed client read path
  after approved unlock, and Task 35 adds the first interaction-scoped interview
  feedback and outcome-loop baseline, but broader cross-job ontology learning
  and admin eval-feedback tooling remain future work. The current consultant
  matching API/controller/UI baseline remains internal evidence-aware review
  only, while Task 29 now covers the consultant-side shortlist builder/send
  slice and Task 32 covers the first client review surface.

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
- Task 7C by itself did not add API/controller/UI, RBAC/ABAC, Consent/Disclosure/Unlock, database migration, AI/model wiring, or identity disclosure behavior. Later Task 30 adds a real deterministic redaction pipeline plus persisted re-identification audit, while broader UI/disclosure/productization still remains deferred.

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
- Task 12 itself did not add Consent/Disclosure REST/controller/API or UI; later Task 33 adds the first candidate consent, consultant unlock, and client disclosed-candidate product path.
- Real auth/login/session infrastructure now exists, and product-endpoint enforcement now runs through JWT-backed `SecurityContext` with Task 19C strong session revocation.
- No full WorkflowEvent-driven workflow engine, prior-contact/prior-application review flow, fee-agreement validation, or Task 42 browser E2E proof exists yet.

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
- Broader AI gaps remain: no document intelligence/OCR/text extraction, no AI task queue/worker, no retries scheduler, no actual write-back execution, no automatic human review workflow, and no canonical write execution from AI governance. Task 37 now adds the first owner/admin governance API/UI surface and governance-config workbench, but not full AI runtime control-plane productization.

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
- Consultant intake API exposure and the first Consultant Portal v1 frontend now exist for upload, extract, review, decide, publish, workflow, follow-up, and blocked-action handling. Remaining gaps are narrower productization items such as richer async worker orchestration, broader company/job canonical publish, and deeper follow-up automation beyond the current real consultant queue.
- No Consent/Disclosure, RBAC/ABAC, Client-safe projection API/UI, redaction pipeline, unlock/disclosure, or client exposure exists for governed intake.
- Task 5 Governed Intake Minimal Slice is closed as a safe, regression-covered backend chain. Task 6F closes one gated CandidateProfile field write and metadata regression coverage; downstream privacy/access surfaces, full profile behavior, conflict resolution, stale detection, and `recruiting.*` source/packet cleanup remain future work.

## Minimal Client-safe Projection Service Exists; Product Privacy Pipeline Still Deferred

- Raw Candidate must never be exposed to Client.
- `ClientSafeCandidateCard` exists as a backend contract after Task 7A.
- L0-L4 redaction vocabulary exists as policy vocabulary after Task 7A.
- Task 7B adds a minimal backend projection service/read model that returns only `ClientSafeCandidateCard`.
- Task 7B does not add client-facing serialization, API, controller, UI, RBAC/ABAC, Consent, Disclosure, Unlock, identity disclosure, database migration, or AI/model wiring.
- Task 7C adds only a deterministic placeholder for re-identification assessment; it does not perform real scoring.
- No broad redaction implementation exists beyond the current deterministic client-safe summary pipeline and its shortlist/query integrations.
- No learned or calibrated re-identification scoring exists yet; the current scorer is deterministic and policy-coded.

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
- Task 33 now adds the first Consent/Disclosure/Unlock product workflow beyond the earlier Task 12A/12B/14 backend kernel, while Task 8 still proves role-alone L4/identity access is denied.
- Identity-disclosed Client access behavior now exists only through the approved Task 33 unlock/disclosure path; it is not granted by generic Client role.
- No complete product-wide RBAC/ABAC enforcement exists beyond the Task 8B/8C backend guard surfaces and regression tests.

## Workflow Engine Remaining Gaps

- Task 26 upgrades the earlier append-only workflow foundation into a real legality/preview layer for the current product slice.
- WorkflowEvent append, idempotency, correlation, causation, and backend read-model foundations still exist and remain the audit source of truth.
- Workflow action vocabulary now includes state-machine legality rules, canonical preview targets, and blocker reasons for the current job/candidate/shortlist/consent/disclosure scope plus placement/commission read-model lookup baseline.
- `WorkflowTransitionAuditService` now performs preview-time legality checks, fail-closed current-state validation through entity-state lookup, and append-boundary audit recording for requested transitions.
- Consultant workflow API/UI now expose timeline before/after state, current entity status, legal next actions, and blockers.
- Workflow mutation still remains service-owned; Task 26 does not introduce a generic engine that directly mutates arbitrary entity rows.
- No persisted SLA/automation workflow task queue or full BPMN-style engine exists yet.
- Task 45 now covers deterministic due dates, reminder thresholds, escalation thresholds, owner assignment, consultant queue output, Admin rule visibility, and timeline export; external dispatch and persisted runtime task orchestration remain future work.
- Task 37 now adds the first Owner/Admin workflow analytics and cross-portal governance surfaces, but broader workflow drill-down depth, automation-aware operating views, and richer cross-portal workflow control remain future work.
- Task 5 governed-intake minimal slice exists, but the broader governed intake orchestration engine remains future work.

## Workflow Read Model Remaining Gaps

- Task 4C adds a backend-internal, read-only audit query/read model for `WorkflowEvent`.
- Task 26 adds consultant-facing workflow timeline and entity-state API/UI integration on top of the audit/read-model foundation.
- It is not a client-safe projection.
- It does not expose raw Candidate/Profile payloads or business entity internals.
- It is still not dashboard analytics, full reporting, full-text search, generic repository search, or arbitrary SQL filtering.
- Correlation and causation identifiers remain queryable for backend audit lineage, but only the consultant workflow slice is surfaced today.

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
- API/controller integration and the first governed-intake frontend UI now exist through `ConsultantIntakeController` plus the unified Consultant Portal intake/upload/review/publish surfaces.
- No API/controller/UI integration exists for CandidateProfile.
- No Consent/Disclosure API/controller/UI or broad workflow behavior exists beyond the current backend-only Task 12A/12B/14 kernel.
- No broad service-level RBAC/ABAC enforcement exists beyond the Task 8B/8C minimal projection/raw CandidateProfile guard surfaces and five-portal boundary tests.
- No broad client-safe product UI exists. Task 13A adds only a narrow route-aware portal shell plus anonymous client-safe candidate-card flow, while Task 30 hardening now adds real backend redaction behavior behind the shortlist send and client-safe candidate-card query paths.
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
- Task 18A is complete only for the original Consultant read-only API layer on companies, jobs, and shortlists. Later work extends shortlist detail and command behavior through Task 18C and Task 29, but no Client-safe candidate projection read endpoints exist through the product API layer, no Client portal product API endpoints exist, no filtering beyond optional status (list) and optional companyId/jobId (job/shortlist lists) exists, no full-text search exists, and no composite FK org-scope hardening at DB level for Company/Job/Shortlist child tables has been added. Real auth/login/session/Spring Security now exists, the consultant product API uses JWT-backed `SecurityContext`, and Task 19C closes the baseline auth/session hardening slice.

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
- Remaining gaps: no DELETE endpoints, no Candidate/CandidateProfile write endpoints, and no batch operations. Later Task 18C closes shortlist write endpoints, and Tasks 25/32 add the current client-portal write surface.
