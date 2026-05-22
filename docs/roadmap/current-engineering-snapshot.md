# Current Engineering Snapshot

This file contains mutable short-term engineering state. Update it after future successful main merges.

## Current Main Baseline

- latest local `main` feature baseline after the Task 60 full-product
  acceptance gate: `FULL_PRODUCT_100_READY` for the current v2.1/v2.0
  specification. Task 60 adds the final evidence report and tightens the
  Consultant route contract to assert `/consultant/placements`; it does not
  claim public SaaS launch, managed-cloud signoff, formal certification,
  external systems completion, or customer go-live approval.
- latest pre-Task 43 local `main` baseline commit: `ce0944e` (`Resolve Task 42 local main docs drift`)
- latest production security compliance baseline: Task 52 at `afc6942`.
- latest Task 42 gate work on local `main`: Task 42
  Pilot E2E Acceptance Gate model/report now returns
  `CONTROLLED_PILOT_READY` for the Task 42 Usable v1 gate.
- latest Task 39 baseline commit on main: `984b329` (`Initialize local MinIO deployment bucket`)
- latest product baseline merges on main:
  - Task 60 - Full Product Acceptance Gate, including the final acceptance
    matrix in `docs/roadmap/task-60-full-product-acceptance-gate.md`, the
    missing Consultant `/consultant/placements` route-contract assertion, and
    passing local release-gate evidence with browser E2E on API `8097`, web
    `4197`, and isolated PostgreSQL `55432`.
  - Task 50 - Governance, Eval, and Ontology Production Console, including Admin governance surfaces for eval failures, deterministic negative cases, review quality, model routing inspection with the existing governed config overlay, cost/latency, ontology drift, redaction incidents, and AI resume authenticity risk, plus an Owner `ai-quality` summary. This reuses existing Task 44/47/49/51/54/56/57 boundaries and does not add live provider activation/switching, ontology mutation, external BI/legal/accounting integrations, Task 58 release management, or Task 60 final acceptance.
  - Task 58 - Release Management and Regression Suite, including a CI release-regression workflow, local ordered `release:gate`, Flyway/Testcontainers migration validation, full backend and frontend release chain, deterministic pilot browser E2E wrapper with owned PostgreSQL/API cleanup, privacy/security negative regression suite, AI eval artifact/schema regression suite, and strict release checklist/gate docs. This creates the release safety system only and does not add Task 60 final acceptance or public launch signoff.
  - Task 57 at `cd81acc` - Reporting, Exports, and Legal Audit Packages, including backend-owned reporting export adapters for owner reports, consultant activity, client shortlist feedback, candidate personal data, disclosure audit, placement/commission, and retention evidence with role/scope/visibility policy.
  - Task 56 at `68cef32` - Support and Operations Tooling, including audited support lookup/action contracts, safe failed-notification retry, AI task replay adapter boundary, support transaction boundary, support user lookup, and support action audit persistence.
  - Task 55 at `02fbda9` - Data Import and Migration from Existing Systems, including governed import batch planning, validation/reporting, legacy ATS/CRM mapping contracts, duplicate/import safeguards, rollback/reset planning, and governed-intake import gateway boundaries.
  - Task 49 at `d777456` - Integrations v1, including audited inbound/outbound integration boundaries, no-op provider placeholders for email/SMS/calendar/OCR-STT/ATS-HRIS/webhooks, governed intake integration sink, redaction/disclosure checks, and PostgreSQL integration audit persistence.
  - Task 51 at `c14723a` - Multi-organization Boundary Hardening, including V33 organization-scoped identity constraints, same-organization composite FKs for identity/auth/audit/workflow/governance/notification/commission actor links, tenant-aware access-audit search, tenant-aware Owner export filtering, tenant-aware pilot seed/import preflight, consultant same-organization access enforcement, and explicit audited support/admin impersonation policy.
  - Task 48 at `5dfcf71` - Commercial and Finance Operations Hardening, including fee agreement snapshots, invoice readiness gates, invoice sent/paid/guarantee workflow enforcement, commission calculation inputs, Owner revenue/accounting export read models, and the explicit non-accounting-system boundary.
  - Task 54 at `13cc42a` - Performance, Load, and Cost Targets, including target envelopes, backend budget policy, deterministic local performance/load/cost harness, and cost alert classifications.
  - Task 53 at `add4d5f` - Disaster Recovery and Business Continuity, including backup/restore runbook evidence, migration rollback invariants, local document/object recovery, AI/notification outage playbooks, and incident severity levels.
  - Task 52 at `afc6942` - Production Security Compliance Baseline, including threat model, access review, retention and secret-rotation runbooks, dependency/pen-test remediation tracking, issue register, and compliance documentation regression coverage.
  - Task 59 at `a2173d0` - Pilot-to-Production Onboarding Playbooks, including customer, consultant, client, candidate, admin, data-import, risk-review, and go-live playbooks.
  - Task 47 - Industry Pack Expansion and Calibration baseline, including all 8 v2.1 packs, Task 47 calibration metadata, semiconductor production calibration, seeded non-semiconductor packs, drift signals, and Admin industry-pack review queue visibility.
  - Task 46 — Full Data Lifecycle, Deduplication, Conflict, Stale, and Merge decision/audit baseline, including candidate/company/job duplicate decisions, confirmed-fact merge conflict blocking, stale refresh requests, retention/deletion tombstone decisions, and Owner `data-quality` lifecycle metrics.
  - Task 45 - Full Workflow Automation and SLA Engine baseline, including SLA/reminder/escalation/owner/blocker/next-best-action rules, consultant automation queue, CSV timeline export, Admin workflow rule visibility, and manual override reason enforcement.
  - Task 44 - Full AI Task Registry Production Coverage, including all 28 v2.1 production AI task definitions, prompt/schema/eval artifacts, review/write-back policy, model route inspection, and definition-first Admin AI task registry visibility.
  - Task 43 — Full Portal Depth and UX Completion route-depth closure, including five-portal route contract coverage, strict Client/Candidate spec route parameter alignment, Client/Candidate route-depth continuity, and Admin `/admin/integrations` governance read wiring.
  - `ce0944e` — Task 42 local main docs drift cleanup after the pilot E2E acceptance gate merge.
  - `1755de9` — Task 42 Pilot E2E Acceptance Gate closure, including S01-S08 Playwright business-flow evidence, Task 38 pilot CLI evidence, Task 39 backup/restore evidence, and an updated `PilotAcceptanceGate` / `PilotAcceptanceReport` result of `CONTROLLED_PILOT_READY` for the Task 42 Usable v1 gate.
  - `58529e4` / `b31e2e3` — Task 41: Security and Privacy Hardening v1, including login input policy, auth/document rate limiting, upload filename rejection, URL-path PII masking in request logs, explicit Admin disclosure-audit export permission, persistent access audit for Task 41 sensitive document/export surfaces, data-retention/vulnerability-scan baseline docs, pinned Maven dependency-check configuration, and focused privacy/security regressions.
  - `68647b5` — Task 40: provider-neutral observability, audit search, disclosure audit export, AI task trace/replay visibility, and incident runbook baseline
  - `984b329` — Task 39: provider-neutral deployment baseline follow-up for local MinIO bucket initialization
  - `5a6dd91` — Task 39: staging MinIO deployment endpoint hardening
  - `3864a4b` — Task 39: object storage deployment wiring hardening
  - `fb959ac` — Task 39: backup restore deployment runbook hardening
  - `e594364` — Task 39: deployment profile precedence hardening
  - `9e148db` — Task 38: Pilot Seed Data and Import Tools review-blocker closure
  - `4e4560f` — Task 38: Pilot Seed Data and Import Tools baseline
  - `639cbdf` — Task 37: Owner and Admin Governance v1
  - `66d59e9` — Task 36: Placement and Commission v1
  - `8513ae2` — Task 35: Interview Feedback and Outcome Loop v1
  - `f3721a5` — Task 34: Notification and Follow-up System v1 plus candidate/client portal session closure
  - `2d69ce5` — Task 31: Candidate Portal v1
  - `a74ecd1` — Task 33: Consent, Disclosure, and Unlock End-to-End
  - `aa54f93` — Task 32: Client Portal v1
  - `1a00715` — Task 27: Matching and Evidence v1
  - `c63d79a` — Task 26: Workflow Engine v1
  - `a52d435` — Task 25: Company and Job Intake v1
  - `3ea6473` — Task 20: Document Storage and SourceItem v1
  - `dee64c9` — Task 19A/19B/19C auth baseline, JWT controller migration, and session hardening
- current Task 44 baseline: 28/28 v2.1 production AI task definitions,
  prompt/schema/eval artifact coverage, per-task review/write-back policy,
  default governed model route inspection, and definition-first Admin
  `/admin/ai-task-registry` cost/latency/replay visibility.
- current Task 45 baseline: backend-owned workflow automation and SLA
  coverage now exists for consent, clarification, feedback, interview, offer,
  invoice, and guarantee workflows. Consultant workflow exposes an automation
  queue and CSV timeline export from the existing `WorkflowEvent` read model;
  Admin `/admin/workflow-rules` shows SLA/reminder/escalation coverage; manual
  override requests require a reason. This does not add a full workflow/BPMN
  runtime.
- current Task 46 baseline: backend-owned data lifecycle decisions now
  exist for candidate/company/job duplicate detection, high-confidence duplicate
  blocks, low-confidence warnings with justification, merge proposals,
  confirmed-fact merge conflict blocks, conflict-resolution recording,
  stale-field refresh requests, and retention/deletion policy decisions with
  tombstone protection. Owner `data-quality` metrics now read lifecycle
  decision counts from `workflow.workflow_event`. This is a decision/audit
  baseline, not a mutation executor: it does not add physical row deletion,
  direct merge mutation, fuzzy-search infrastructure, external queues, or
  canonical field overwrite.
- current Task 47 baseline: all 8 v2.1 industry packs now exist with active
  ontology versions, role-family templates, skill concepts, gold cases,
  negative cases, anti-patterns, score caps, drift signals, and review
  deadlines. `semiconductor` is the only pack marked `production`; all other
  packs remain honest seeded packs and surface through Admin industry-pack
  review queue metrics. The active semiconductor v2 ontology preserves the
  existing DV/PD/DFT/analog/firmware role-family surface, and Admin review
  queue logic also reclassifies production packs whose ontology or calibration
  review deadline has expired. This is a backend-owned calibration metadata
  baseline, not a learned calibration executor or admin editing UI.
- current Task 48 baseline: placement-to-paid commercial workflows now have
  backend-owned fee agreement snapshots, invoice readiness gates, invoice
  sent/paid/guarantee state enforcement, commission calculation inputs,
  Owner revenue reporting, and a read-only accounting export handoff. This is
  not invoice issuing, payment collection, tax handling, GL posting, or an
  accounting-system replacement.
- current Task 51 baseline: organization boundaries are hardened across the
  existing backend-owned surfaces for the Task 51 scope. V33 adds an
  organization-scoped `identity.user_account` parent key and same-organization
  composite FKs for identity role/session, access audit, workflow/review,
  canonical-write, AI task, unlock, notification/follow-up, and commission
  actor links. Admin access-audit search, Owner placement/revenue exports, and
  pilot seed/import preflight are tenant-aware. Consultant product access now
  requires explicit same-organization scope, and support/admin impersonation is
  explicitly same-organization, ticketed, break-glass approved, and audited.
  This is boundary hardening, not multi-organization membership/session
  switching, full support tooling, broad legal/reporting export packages, or
  real customer import/migration workflow delivery.
- current Task 49 baseline: external-channel integration boundaries now exist
  as audited backend contracts. Inbound inputs are routed toward governed
  intake/SourceItem-first paths, outbound commands require audit/redaction and
  disclosure-state decisions, and provider implementations remain explicit
  placeholders unless real providers are configured. This is integration
  boundary hardening, not live production provider activation.
- current Task 50 baseline: governance/eval production console depth now exists
  as Admin/Owner product surfaces with read-only observation sections and the
  existing governed `model-routing` config overlay preserved. Admin can locate
  AI task failures, schema/eval/hallucination risks, deterministic negative
  cases, low-quality review patterns, model-routing config issues, Task 54
  cost/latency warnings,
  Task 47 ontology drift/stale deadlines, redaction/re-identification
  incidents, and AI resume authenticity risk without database access. Owner
  `ai-quality` shows a narrower safe summary. This is visibility and triage
  aggregation, not live provider activation/switching, ontology editing,
  support mutation, external BI/legal/accounting integration, Task 58 release
  management, or Task 60 final acceptance.
- current Task 55 baseline: governed import/migration now has planning,
  validation, mapping, duplicate/reporting, rollback/reset, and governed-intake
  gateway contracts. Historical data is modeled as a governed import pipeline,
  not direct writes around SourceItem/InformationPacket/claim/canonical gates.
  This is a backend migration-safety baseline, not customer-specific migration
  execution.
- current Task 56 baseline: support operations now have audited support lookup,
  retry/replay, failed-notification retry, transaction-boundary, and action
  audit contracts. Support actions are constrained to service-owned ports and
  policy checks rather than direct database edits. This is backend support
  tooling, not a complete support UI or external ticketing integration.
- current Task 57 baseline: reporting/export/legal-audit packages now expose
  backend-owned export payload contracts with role, target-scope, consent,
  disclosure, and field-visibility policy for owner, consultant, client,
  candidate, disclosure-audit, placement/commission, and retention-evidence
  export types. This is safe export package productization, not a BI warehouse
  or accounting/legal system replacement.
- current Task 52 baseline: production security compliance documentation now
  records a threat model, access review, retention and secret-rotation
  runbooks, dependency and pen-test remediation process, security issue
  register, and regression coverage. It is not a SOC 2 report, ISO
  certification, public penetration-test attestation, or completion of MFA/SSO,
  distributed rate limiting, product-wide field-level access audit, or exact
  production retention windows.
- current Task 53 baseline: DR/BCP now has local backup/restore drill evidence,
  document/object recovery proof, migration rollback invariants, AI/notification
  outage playbooks, and incident severity levels. It is still local/provider
  neutral evidence, not managed cloud backup execution, multi-region failover,
  external vendor SLA proof, or public production incident communications.
- current Task 54 baseline: performance/load/cost targets now have documented
  p95/p99/throughput envelopes, provider-neutral AI cost budgets,
  `PerformanceCostPolicies`, and a deterministic harness. The harness is
  capacity-model evidence, not live API/browser/provider timing or production
  performance certification.
- current Task 59 baseline: onboarding is now packaged into repeatable customer,
  consultant, client, candidate, admin, data-import, risk-review, and go-live
  playbooks. It supports controlled-pilot onboarding without ad hoc engineering
  except approved integrations and governed data import; it is not public SaaS
  launch readiness.
- latest documented validation snapshot for the local Task 42 main baseline: `rtk git diff --check HEAD~1..HEAD`, `rtk npm run typecheck:web`, `rtk npm run build:web`, `rtk docker info`, and `PATH=/opt/homebrew/bin:$PATH rtk mvn -f services/core-api/pom.xml test` all passed on `main`. Maven reported 1029 tests, 0 failures, 0 errors, and 3 skipped. Task 52 now records the latest dependency-scan evidence for the security compliance baseline; it is not reclassified as Task 42 pilot evidence.
- latest Task 43 validation snapshot: `rtk npm --workspace @rto/web run test -- portalRouteContract.test.ts`, `rtk npm --workspace @rto/web run test`, `rtk npm run typecheck:web`, `rtk npm run build:web`, `rtk git diff --check`, `rtk mvn -f services/core-api/pom.xml -Dtest=AdminGovernanceControllerMappingTest,GovernanceReadServicePostgresIntegrationTest#onlyRuntimeWiredAdminSectionsAreEditable test`, and `rtk mvn -f services/core-api/pom.xml test` passed. Full Maven reported 1029 tests, 0 failures, 0 errors, and 3 skipped. Browser smoke on `http://127.0.0.1:5173` covered the strict Client/Candidate/Admin Task 43 deep links and rendered guarded sign-in/admin shell states without blank pages.
- latest Task 46 validation snapshot: `rtk mvn -f services/core-api/pom.xml -Dtest=DataLifecycleServiceTest,WorkflowActionPolicyTest,GovernanceReadServiceTest test`, `rtk git diff --check`, `rtk docker info`, and `PATH=/opt/homebrew/bin:$PATH rtk mvn -f services/core-api/pom.xml test` passed. Full Maven reported 1056 tests, 0 failures, 0 errors, and 3 skipped.
- latest Task 47 validation snapshot: `rtk mvn -f services/core-api/pom.xml -Dtest=JdbcIndustryPackReadPortIntegrationTest test`, `rtk mvn -f services/core-api/pom.xml -Dtest=GovernanceReadServicePostgresIntegrationTest#industryPackGovernanceShowsTask47CalibrationQueue test`, `rtk mvn -f services/core-api/pom.xml -Dtest=JdbcIndustryPackReadPortIntegrationTest,GovernanceReadServicePostgresIntegrationTest,GovernanceReadServiceTest,ConsultantMatchingSurfaceServiceTest,ScoreCapPolicyTest test`, `rtk git diff --check`, `rtk docker info`, and `PATH=/opt/homebrew/bin:$PATH rtk mvn -f services/core-api/pom.xml test` passed after the review-fix loop. Full Maven reported 1062 tests, 0 failures, 0 errors, and 3 skipped.
- latest integration batch validation snapshot on `main` before this docs drift
  commit: `rtk git diff --check`, `rtk docker info`,
  `rtk mvn -f services/core-api/pom.xml test`,
  `rtk npm --workspace @rto/web run test`, `rtk npm run typecheck:web`, and
  `rtk npm run build:web` all passed. Full Maven reported 1083 tests, 0
  failures, 0 errors, and 3 skipped. Web Vitest reported 9 test files and 38
  tests passed.
- latest Task 51 validation snapshot: `rtk git diff --check`, `rtk docker info`,
  `rtk mvn -f services/core-api/pom.xml -Dtest=FivePortalBoundaryRegressionTest,PermissionEnforcerTest,AccessControlContractTest,JdbcAccessAuditRecorderPostgresIntegrationTest test`,
  `rtk mvn -f services/core-api/pom.xml -Dtest=IdentityAuthPostgresIntegrationTest,SupportImpersonationPolicyTest,AdminObservabilityControllerPolicyTest,OwnerRevenueQueryServiceTest,PilotDataPostgresIntegrationTest,ConsultantApiCommandServiceTest,ConsultantApiQueryServiceTest,ConsultantWriteOrgIsolationIntegrationTest test`,
  and `rtk mvn -f services/core-api/pom.xml test` passed in the Task 51
  worktree. Full Maven reported 1093 tests, 0 failures, 0 errors, and 3
  skipped.
- latest Task 49/55/56/57 integration validation snapshot on `main` before this
  docs drift commit: `rtk git diff --check`, `rtk docker info`,
  `rtk mvn -f services/core-api/pom.xml test`,
  `rtk npm --workspace @rto/web run test`, `rtk npm run typecheck:web`, and
  `rtk npm run build:web` all passed. Full Maven reported 1144 tests, 0
  failures, 0 errors, and 3 skipped. Web Vitest reported 9 test files and 38
  tests passed.
- latest Task 50 validation snapshot on `main`: `rtk git diff --check`,
  `rtk docker info`,
  `rtk mvn -f services/core-api/pom.xml -Dtest=GovernanceConsoleReadServicePostgresIntegrationTest,AdminGovernanceControllerMappingTest,OwnerGovernanceControllerPolicyTest test`,
  `rtk mvn -f services/core-api/pom.xml -Dtest=AdminGovernanceControllerMappingTest,OwnerGovernanceControllerPolicyTest,GovernanceReadServiceTest,GovernanceReadServicePostgresIntegrationTest,ObservabilityReadServiceTest,AITaskRunnerServiceTest,JdbcIndustryPackReadPortIntegrationTest,RedactionAuditPostgresIntegrationTest test`,
  `rtk mvn -f services/core-api/pom.xml test`,
  `rtk npm --workspace @rto/web run test`, `rtk npm run typecheck:web`, and
  `rtk npm run build:web` all passed. Full Maven reported 1169 tests, 0
  failures, 0 errors, and 3 skipped. Web Vitest reported 9 test files and 38
  tests passed.
- merge status: Task 49, Task 55, Task 56, and Task 57 were rebased onto
  current local `main`, validated in their worktrees, fast-forward merged in
  order, and smoke-validated on `main` after each merge. No push was performed.
- next recommended task: move from Task 60 closure into post-100 roadmap
  selection. Task 42 readiness remains scoped to the controlled-pilot Usable v1
  gate, and Task 60 readiness remains scoped to the current v2.1/v2.0
  specification rather than public SaaS launch certification.

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
- Task 15: Product Readiness Bridge ✅ docs-only planning baseline delivering v2.1 capability split and usable-v1 acceptance scenarios
- Task 16: Real Product Data Model Completion ✅ V10 migration (15 new tables), domain/port/adapter/service/tests for Company, Job, Shortlist, Placement, Commission, CandidateDocument, Interaction, InterviewFeedback, ProfileFieldLineage
- Task 16-Hardening: DB Org-Scope Composite FK Hardening ✅ V12 migration (7 UNIQUE constraints + 19 composite FKs + 19 dropped simple FKs), 8 cross-org negative integration tests
- Task 17: Canonical Write Audit and Blocked Attempt Ledger ✅ V11 migration (governance.canonical_write_attempt), CanonicalWriteAttemptPort, CanonicalWriteService persistence for all decision types (allow/block/require_review), CanonicalWriteResult carries canonicalWriteAttemptId
- Task 18A: Product API Layer v1 — Infrastructure + Consultant Read Endpoints ✅ generic pagination (PagedQuery/PagedResult), 6 consultant response DTOs (company/job/shortlist summary+detail), 3 consultant read-only controllers, ConsultantApiQueryService facade, ConsultantCompany/Job/Shortlist response mappers, ResourceType.SHORTLIST, FieldAccessPolicy consultant allow rules, ApiSafeResponseBody extension, ApiBoundaryContractRules allowlist expansion, findAllByOrganizationId on Company/Job/Shortlist ports+JDBC+services, leakage and denial tests for all 6 endpoints
- Task 20: Document Storage and SourceItem v1 ✅ V13 migration (mime_type, file_size_bytes, original_filename, scan_status + unique constraint on intake.source_item), DocumentStore interface + DocumentStoreKey + InMemoryDocumentStore, VirusScanPort + NoOpVirusScanPort, DocumentUploadCommand + DocumentUploadResult, DocumentUploadService (MIME validation, size limits, SHA-256 dedup, idempotent), ConsultantDocumentController (POST upload + GET download), DocumentRetrievalResult, SourceItem record enhancement (4 new fields), JdbcSourceItemPersistencePort/JdbcInformationPacketPersistencePort column updates, API boundary leakage regression updated. No real virus scan (NoOp placeholder), no AI extraction, no client/candidate upload, no presigned URLs, CanonicalWriteGate bypass prevented.
- Task 21: Real AI Task Runner v1 ✅ V18 migration (`input_payload`, `output_payload`, replay lineage on `governance.ai_task_run`), `AITaskRun` append/update/readback audit model, audited in-process runner, prompt registry, JSON schema validation, task routing, DeepSeek provider adapter, Candidate Profile Parser v1, Authenticity Risk Assessor v1, replay support, authenticity-to-matching request adapter, and focused runner tests. Scope remains audit-only: no ClaimLedger/ReviewEvent/WorkflowEvent/canonical write-back from AI outputs.
- Task 22: Document Intelligence and Evidence Retrieval v1 ✅ V19 migration (`intake.parsed_document`, `intake.parsed_document_chunk`, `intake.parsed_document_span` plus `intake.extraction_run` mode expansion), `documentintelligence` package with TXT/PDF/DOCX parsing, chunk/span persistence, OCR/STT fail-closed boundary via `PENDING_EXTERNAL_PROCESSING`, `DocumentIntelligenceExtractionService`, and consultant document parse/evidence endpoints. Scope remains evidence-only: no OCR execution, no ClaimLedger append, no ReviewEvent append, no WorkflowEvent promotion beyond existing intake flows, and no canonical write-back from parsed output.
- Task 23: Governed AI Intake End-to-End backend/API slice ✅ V21 migration enabling `GOVERNED_AI_V1`, governed AI extraction orchestration across candidate/company/job packets, clean-fact candidate generation, stable source-span lineage for repeated fields, review query/decision services, consultant intake endpoints (`extract`, `review`, `decide`, `publish`), API-safe intake DTOs, contract-test allowlisting, evidence-quote-backed source highlights with fail-closed extraction when evidence cannot be matched, and fail-closed publish behavior. Candidate canonical publish now requires an existing candidate target, maps supported AI stable keys onto canonical CandidateProfile field paths (`profile.headline`, `profile.summary`, `skills.primary_skills`, `experience.projects`, `experience.timeline_highlights`), and no longer auto-creates records. The original Task 23 company/job publish block was later closed for the current scope by Task 25. Scope still excludes generic consultant candidate CRUD.
- Task 24: Consultant Portal v1 ✅ unified `/consultant` workspace in `apps/web` with dashboard, blocked actions, upload/intake/review/publish flow, talent/company/job/shortlist views, job intake, job outreach, matching review, workflow timeline, audit drawer, follow-up queue, and consultant candidate detail sections for overview/evidence/conflicts/stale info/follow-ups/history; plus new consultant backend surfaces for dashboard, candidates, workflow/audit, and follow-up data. Validation includes frontend `typecheck` / `build` and the targeted consultant/backend API suites used during final closure.
- Task 25: Company and Job Intake v1 ✅ backend-owned `CompanyIntakeApplicationService` / `JobIntakeApplicationService` / `JobActivationGateService`, governed company/job review-to-publish write-back, consultant job activation gate + activate API/UI, minimal structured commercial terms placeholder, client company profile + job intake + clarification API boundary, minimal real Client portal routes, and ownership hardening so client access is exact `clientActorId` scoped instead of metadata-text based. Validation includes frontend `typecheck` / `test` / `build` and backend `mvn -f services/core-api/pom.xml test -DskipITs`.
- Task 26: Workflow Engine v1 ✅ workflow legality vocabulary aligned to the current spec, transition decision/blocker model, `WorkflowTransitionAuditService.preview(...)`, fail-closed current-state validation through `WorkflowEntityStatePort`, placement/commission workflow state read-model baseline, consultant workflow `entity-state` API plus timeline `beforeStatus`/`afterStatus` + `entityStates`, portal display of current status/legal next actions/blockers, disclosure preview gate integration with real prerequisite checks, and focused controller/service/policy regressions. Scope still excludes SLA automation.
- Task 27: Matching and Evidence v1 ✅ consultant-internal MatchReport persistence via V22, backend-owned assembly from job/profile/document/authenticity evidence, consultant matching GET/POST API, persisted score-cap and risk semantics, consultant portal matching workspace, service-layer permission enforcement for `MATCH_REPORT`, and focused controller/service/JDBC round-trip regressions. Scope still excludes client delivery, shortlist send flow, and real industry ontology calibration.
- Task 28: Semiconductor Industry Pack v1 ✅ V23/V24 migrations for persisted industry-pack/ontology seed data and match-report metadata, new backend-owned `industrypack` read/resolution services, seeded semiconductor role families and anti-pattern templates, job-level consultant pack selection, real ontology/pack metadata flowing into consultant match reports, portal visibility for active pack and anti-pattern warnings, plus post-review hardening for legacy job updates, boundary-aware required-skill matching, historical match-report metadata truthfulness, and consultant-portal rendering of unknown legacy metadata. Scope still excludes admin pack management UI, multi-pack calibration, and client-facing match delivery; Task 35 now adds the first interaction-scoped outcome-loop baseline on top.
- Task 29: Shortlist Builder v1 ✅ backend-owned `ShortlistBuilderService`, shortlist card add/update/send consultant commands, client-safe shortlist card metadata and comparison payloads, pre-send checks, delivery preview placeholders, consultant approval before `sent_to_client`, shortlist/job/candidate workflow transition audit wiring (including draft creation, ready-for-review promotion, return-to-draft rollback, candidate shortlisted, shortlist card remove/restore, and shortlist send), consultant workflow-surface card-status rendering for shortlist composition events, consultant shortlist builder UI expansion, and focused shortlist regression updates. Scope still excludes Task 32 client-facing shortlist review/unlock flows.
- Task 30: Privacy Redaction and Re-identification v1 ✅ V25 migration adding `privacy.reidentification_risk_assessment` (org-scoped composite PK + risk-score check constraint) and extending the `workflow.workflow_event` namespace allowlist to include `privacy`. New `clientsafeprojection` policies — `CompanyNameGeneralizationPolicy` (curated semiconductor / chip / cloud-hyperscaler top-tier list mapping exact employer names to generalized labels), `ProjectChipNameRedactionPolicy` (declared exact names + known chip-family pattern set + generic chip-code-name shape detector with chip-context gating), `RareTitleYearCombinationRiskRule` (chief / head-of / founding / CTO-CEO-CFO etc. patterns with year detection), and `ClientSafeSummaryPipeline` (orchestrator that runs all three across every projected text field and returns a redacted snapshot + observed unsafe features + redaction explanations). `ReidentificationRiskAssessmentService` now produces a deterministic risk score in `[0.0, 1.0]` and a real `assessWithPipeline(...)` overload combining redaction + assessment. New `privacyredaction` package with `ReidentificationRiskAssessmentPort` + `JdbcReidentificationRiskAssessmentPort`, `PersistedReidentificationRiskAssessment`, deterministic workflow entity ids, `RedactionAuditService` orchestrator that writes the assessment row and emits either `REIDENTIFICATION_RISK_ASSESSED` or `CLIENT_SAFE_REDACTION_BLOCKED` workflow events depending on the decision, plus `PrivacyRedactionConfiguration`. Spec acceptance scenario "top chip company + unique title + exact year + chip code name" is now covered end-to-end by `RedactionAuditPostgresIntegrationTest` (Testcontainers Postgres, V1-V25 applied) which proves BLOCK + HIGH risk + employer/chip generalization + per-organization scope + idempotent newest-first listing. Follow-up hardening now wires `RedactionAuditService` into shortlist send re-evaluation and the audited client-safe query path, reuses the Spring-managed audit service, stores `workflow_event_id` on persisted assessments, and regression-covers those call sites. Remaining scope gaps are expanded curated company/chip vocabulary beyond the v1 acceptance set, ML-based scoring, and admin UI for the persisted assessments.
- Task 31: Candidate Portal v1 ✅ dynamic `/candidate/follow-up/:formId` form flow, candidate-scoped opportunity detail and interest capture, requestId-based consent pages, anonymous/semi-anonymous company rendering, consultant-gated follow-up answer capture that does not directly overwrite canonical truth, unlock clock hardening for deterministic consent expiry behavior, and focused candidate portal/controller/service regressions plus full backend/frontend validation.
- Task 32: Client Portal v1 ✅ V26 `privacy.client_unlock_request` persistence, new client dashboard/shortlist controllers and DTOs, client shortlist read/write mutations, unlock-request capture with disclosure workflow audit, client preference read/write on top of existing `CompanyPreference`, feedback submission via `InterviewFeedbackService`, and client-portal dashboard/shortlist/preferences/anonymous-candidate routes. The current client-safe chain now supports create job -> answer clarification -> review shortlist -> request unlock -> submit feedback without exposing raw candidate identity. Final disclosure release and real JD-upload intake remain deferred to later tasks.
- Task 33: Consent, Disclosure, and Unlock End-to-End ✅ V27/V28 migrations (client_unlock_workflow_entity_id + legacy_disclosure_approved_state), new `CandidateConsentController` (`GET /api/candidate/consent-status`, `POST /api/candidate/consent/confirm`), `ClientDisclosedCandidateController` (`GET /api/client/disclosed-candidates/{candidateId}`), `ConsultantUnlockController` (`GET /api/consultant/unlock-queue`, `POST /api/consultant/unlocks/{unlockRequestId}/approve`, `POST /api/consultant/unlocks/{unlockRequestId}/reject`), `CandidateConsentWorkflowService`, `UnlockWorkflowService`, consent/disclosure/unlock DTOs and response types, candidate-portal consent confirmation page, client-portal disclosed-candidate detail page, consultant-portal unlock approval queue page, full workflow event audit chain for consent-confirmation and unlock-approve/reject transitions, and fail-closed boundary behavior when prerequisites are missing. Scope still excludes real external notification delivery (email/WeChat/SMS) and candidate self-registration flow.
- Task 34: Notification and Follow-up System v1 plus candidate/client portal session closure ✅ V29 migration (`operations.notification`, `operations.notification_delivery_attempt`, `operations.notification_preference`, `operations.notification_schedule`, `recruiting.follow_up_submission`), `NotificationService` / scheduler processing, role-scoped notification preferences, reminder opt-out enforcement, notification audit trail, candidate notification/preference APIs and portal pages, candidate/client scoped session refresh/logout handling, consultant/client reminder delivery, consultant follow-up push notifications, and workflow-audited follow-up submissions that create explicit review tasks instead of mutating canonical profile facts.
- Task 35: Interview Feedback and Outcome Loop v1 ✅ V30 interview-feedback/outcome-loop migration, interaction-scoped `InterviewFeedback` and suggestion/calibration persistence, shortlist-embedded plus standalone `/client/feedback/:interviewId` client submission flow, audited AI feedback structurer wiring, consultant follow-up queue reuse for interview-feedback review, controlled consultant review endpoints, interaction-scoped outcome labels, and review-surface hardening so explicit workspace `interviewId`, eligibility gating, single-review semantics, and API error contracts all fail closed.
- Task 36: Placement and Commission v1 ✅ main commit `66d59e9` closes the first placement/commission productization stream with owner placement, commission, and revenue supervision surfaces on top of the recruiting placement/commission model, plus the required backend/API/UI closure to move post-interview transactions into a real owner-visible operating slice.
- Task 37: Owner and Admin Governance v1 ✅ baseline commit `639cbdf` adds `OwnerGovernanceController` and `AdminGovernanceController`, unified governance DTOs (`GovernanceSectionResponse`, `GovernanceMetricResponse`, `GovernanceItemResponse`, `GovernanceConfigUpdateResponse`), `GovernanceReadService`, governance config persistence through `V31__add_governance_config_entries.sql`, real Owner governance sections (`dashboard`, `pipeline`, `consultants`, `clients`, `risk`, `data-quality`, `ai-quality`, `audit`), a real Admin portal/workbench for governance query and config writes, `FieldAccessPolicy` / `ApiSafeResponseBody` expansion for governance resources, and frontend wiring through `apps/web/src/api/governance.ts`, `OwnerPortal.tsx`, and `AdminPortal.tsx`. Residual hardening remains around deeper runtime overlay coverage and dedicated Admin portal frontend tests.
- Task 38: Pilot Seed Data and Import Tools ✅ baseline commit `9e148db` adds deterministic `semiconductor-pilot-v1` data tooling, pilot accounts, 75 synthetic candidates, 8 semiconductor jobs, source-document seed material, import/export/rebuild/reset/validate commands, privacy validation, reset guards, and review-blocker fixes around import preflight, reset ordering, governance cleanup, and candidate-profile consistency.
- Task 39: Deployment v1 ✅ deployment baseline commit `984b329` adds a provider-neutral local-production deployment baseline, deployment/runbook docs, staging/production environment validation, PostgreSQL migration runbook, backup/restore/rollback/smoke-test runbooks, object-storage deployment wiring, and local MinIO bucket initialization. Scope remains not production-ready: no managed cloud, HTTPS/domain, tested restore, or production incident process.
- Task 40: Observability, Audit, and Replay v1 ✅ `main` baseline commit `68647b5` implements the backend/API/runbook subset: request correlation middleware for `/api/**`, staging/production key-value structured log patterns, admin-only observability APIs for WorkflowEvents, ReviewEvents, AITaskRuns, and disclosure audit export, safe API response DTOs, PostgreSQL-backed read services, and the Task 40 incident runbook. Scope remains no external observability vendor, no frontend dashboard, no error dashboard UI, no AI cost/latency dashboard UI, and no product-wide PII log audit claim.
- Task 41: Security and Privacy Hardening v1 is merged on `main` through `58529e4`, adding login input policy, configurable in-memory auth/document endpoint rate limiting, unsafe upload filename rejection before storage/persistence, UUID/email URL-path masking in request logs, explicit Admin same-organization disclosure-audit export permission, persistent access audit for Task 41 sensitive document/export surfaces, a data-retention/vulnerability-scan baseline doc, pinned Maven dependency-check configuration, and focused privacy/security regressions. Scope remains no MFA/lockout persistence/password reset/SSO, no distributed rate limiter, no product-wide field-level access audit, no full product-wide PII log audit, no destructive retention executor, and no production vulnerability remediation report.
- Task 42: Pilot E2E Acceptance Gate closure is now present in the local `main` baseline. It updates the deterministic pilot acceptance gate/report model from the earlier `NOT_READY` baseline to `CONTROLLED_PILOT_READY` for the Task 42 Usable v1 gate. It adds Playwright coverage for five portal seed logins and S01-S08 business flows, current Task 38 pilot CLI evidence, and Task 39 backup/restore evidence including restored API/document checks plus clean-seed restore validation. Scope remains not public production-ready: managed cloud, HTTPS/domain, production incident process, MFA/SSO, support ops, and broader Tasks 43-60 depth remain future work.
- Task 43: Full Portal Depth and UX Completion ✅ route-depth closure for five-portal v2.0/v2.1 route contracts, strict Client/Candidate spec route parameter alignment, Client/Candidate route continuity, and Admin `/admin/integrations` governance read wiring.
- Task 44: Full AI Task Registry Production Coverage ✅ registry baseline for all 28 v2.1 production AI task definitions, prompt/schema/eval artifacts, per-task review/write-back policy, default governed model route inspection, and definition-first Admin AI task registry visibility.
- Task 45: Full Workflow Automation and SLA Engine ✅ automation baseline for deterministic SLA/reminder/escalation/owner/blocker/next-best-action rules, consultant automation queue, CSV timeline export, Admin workflow rule visibility, and manual override reason enforcement.
- Task 46: Full Data Lifecycle, Deduplication, Conflict, Stale, and Merge ✅ decision/audit baseline for candidate/company/job duplicate detection, merge proposals, confirmed-fact conflict blocking, conflict-resolution recording, stale refresh requests, and retention/deletion decisions with tombstone protection. Scope remains no physical row deletion, direct merge mutation, fuzzy-search infrastructure, external data-quality queue, or canonical fact overwrite.
- Task 47: Industry Pack Expansion and Calibration ✅ backend-owned calibration metadata baseline for all 8 v2.1 packs, with only `semiconductor` marked production and the other packs honestly seeded for review.
- Task 48: Commercial and Finance Operations Hardening ✅ fee agreement snapshots, invoice readiness gates, invoice sent/paid/guarantee workflow enforcement, commission calculation inputs, Owner revenue reporting, and read-only accounting export handoff.
- Task 51: Multi-organization Boundary Hardening ✅ organization-scoped identity constraints, same-organization composite FKs for existing identity/auth/audit/governance/workflow/notification/commission actor links, tenant-aware access-audit search, tenant-aware Owner exports, tenant-aware pilot seed/import preflight, consultant same-organization enforcement, and audited same-organization support/admin impersonation policy.
- Task 52: Production Security Compliance Baseline ✅ threat model, access review, privacy/retention and secret-rotation runbooks, dependency/pen-test remediation workflow, issue register, and compliance documentation regression coverage.
- Task 53: Disaster Recovery and Business Continuity ✅ local backup/restore drill, document/object recovery, migration rollback invariants, AI/notification outage playbooks, and incident severity levels.
- Task 54: Performance, Load, and Cost Targets ✅ documented latency/throughput/cost targets, backend budget policy, and deterministic local performance/load/cost harness.
- Task 55: Data Import and Migration from Existing Systems ✅ governed CSV/document import, validation/reporting, duplicate safeguards, rollback/reset planning, and governed-intake import gateway boundaries.
- Task 56: Support and Operations Tooling ✅ ticketed same-organization support lookup, failed-notification retry, AI task replay boundary, data-correction request workflow, and support action audit.
- Task 57: Reporting, Exports, and Legal Audit Packages ✅ role/scope-safe Owner, Consultant, Client, Candidate, disclosure, placement/commission, and retention evidence export package boundaries.
- Task 58: Release Management and Regression Suite ✅ CI and local release gates, migration validation, backend/frontend regression chain, deterministic pilot E2E wrapper, privacy/security negatives, AI eval artifact/schema regression, and release checklist/gate docs.
- Task 59: Pilot-to-Production Onboarding Playbooks ✅ customer onboarding, consultant/client training, candidate consent FAQ, admin setup, data import, risk review, and go-live playbooks for repeatable controlled-pilot onboarding.
- Task 60: Full Product Acceptance Gate ✅ current v2.1/v2.0 specification accepted as `FULL_PRODUCT_100_READY` with final evidence matrix, `/consultant/placements` route-contract coverage, and passing release-gate browser E2E evidence.
- Task 18C: Consultant Shortlist CRUD + Sub-entity CREATE Endpoints ✅ ShortlistPersistencePort.update() + JdbcShortlistPersistencePort.update() with optimistic locking (WHERE organization_id = ? AND version = ?, SET version = version + 1), ShortlistService.updateShortlist(), FieldAccessPolicy.decideConsultantAccess() extended for SHORTLIST CREATE/UPDATE, 5 new request DTOs (ShortlistCreateRequest, ShortlistUpdateRequest, CompanyContactCreateRequest, JobRequirementCreateRequest, JobScorecardCreateRequest), ConsultantApiCommandService extended with createShortlist/updateShortlist/createCompanyContact/createJobRequirement/createJobScorecard, ConsultantShortlistController @PostMapping + @PutMapping("/{shortlistId}"), ConsultantCompanyController @PostMapping("/{companyId}/contacts"), ConsultantJobController @PostMapping("/{jobId}/requirements") + @PostMapping("/{jobId}/scorecard"), ApiBoundaryRegressionClosureTest updated for ShortlistController POST/PUT whitelisting, ConsultantControllerLeakageTest extended with 15 new write-operation tests, ConsultantWriteOrgIsolationIntegrationTest extended with 4 shortlist org-isolation + optimistic-locking tests. All sub-entity CREATE endpoints return parent detail response.
- Task 19A: Identity/Auth Infrastructure Baseline ✅ V15 migration adds `identity.user_account.password_hash` and new `identity.session` table. Backend now has Spring Security stateless filter chain, JWT issuance/validation, `RtoAuthenticatedPrincipal`, refresh-token-backed session persistence, `AuthenticationService`, `AuthenticationController` with `POST /api/auth/login`, `POST /api/auth/refresh`, and `POST /api/auth/logout`, auth-safe response DTOs, invalid-token fail-closed handling, focused auth controller coverage, and PostgreSQL/Testcontainers login-refresh-logout regression coverage.
- Task 19B: Product Controller Migration to JWT-backed Security Context ✅ consultant/client-safe/document product endpoints now read identity from Spring Security principal instead of temporary role/org headers, `SecurityConfig` now requires authentication for `/api/**` except `/api/auth/**` and `/health`, client-safe access context adapts from authenticated principal plus explicit field/disclosure headers, consultant/client-safe/document WebMvc regression tests now use `SecurityMockMvcRequestPostProcessors.authentication(...)`, and the backend Maven suite passes after the migration.
- Task 19C: Auth/Session Hardening and Regression Closure ✅ access tokens now fail closed against active `identity.session` state on every authenticated request, refresh now revokes the old session and creates a new session id so old access tokens die immediately, filter-time checks now fail closed on session/account/role mismatch, auth/controller regression tests now cover revoked-session bearer tokens and stale principal paths, and the backend Maven suite passes with the stronger revocation model.

## Current Truth/Kernel Capabilities

- ClaimLedger append persistence exists.
- ReviewEvent append persistence exists.
- WorkflowEvent append/audit foundation exists.
- CanonicalWriteGate exists and must be used before canonical writes.
- CanonicalWriteService boundary exists.
- `CanonicalWriteService` now persists a `governance.canonical_write_attempt` record for every decision type (allow/block/require_review) through `CanonicalWriteAttemptPort`. `CanonicalWriteResult` carries `canonicalWriteAttemptId` for downstream audit linkage.
- `CanonicalWriteService` idempotency returns the existing attempt on matching idempotency key without re-executing the gate decision.
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
- Task 9B adds the first minimal client-safe controller endpoint: `GET /api/client-safe/candidate-cards/{anonymousCardRef}`. The path uses the `card_` anonymous card reference, now authenticates through the JWT-backed `SecurityContext`, still accepts explicit field/disclosure request inputs for safe access shaping, delegates to a safe query facade/port returning `ClientSafeCandidateCard`, maps only through `ClientSafeCandidateCardResponseMapper`, and returns the existing API-safe envelope.
- Task 9B controller tests prove successful responses expose only the client-safe DTO/envelope, omit raw ids/PII/raw source/consultant notes/exact employer/project/product/chip/L4 identity fields, fail closed on missing/denied/identity-disclosed access context, sanitize denials, reject raw UUID path refs, expose no raw Candidate/Profile/governance types, and add no raw Candidate/Profile endpoints.
- Task 9C closes the current backend API boundary scope with regression tests proving anonymous-card-only request paths, raw id rejection, fail-closed access context handling, missing/unsupported context denial, sanitized denied/not-found/internal-error envelopes, successful DTO/envelope-only responses, no raw domain/governance type exposure in controller/facade/port/mapper surfaces, no broad/raw/disclosure/unlock/consent endpoint surface, and stricter API-visible text sanitization for DTO/error text.
- Task 10A adds minimal AITaskRun metadata auditability: explicit `CREATED/RUNNING/SUCCEEDED/FAILED/CANCELLED` status vocabulary, task/model/prompt/schema version validation, safe failure reason validation, requested-by/correlation/causation metadata, and append/readback PostgreSQL persistence through `AITaskRunService` / `JdbcAITaskRunPort`.
- Task 10B adds explicit AI write-back target and human-review status vocabulary plus deterministic `AITaskGovernancePolicy` decisions for metadata validation. It accepts no-write-back and claim-ledger proposal metadata, requires approved human review plus CanonicalWriteGate for canonical targets, requires client-safe boundary semantics for client-visible projection targets, and blocks consent/disclosure/unlock, workflow-action, and commercial/placement targets in this kernel.
- Task 10C closes the current AI governance backend kernel scope with regression coverage proving AITaskRun persistence stores model/prompt/schema/task version metadata, safe status metadata, write-back target metadata, and human-review metadata only; it does not call AI/model services, execute prompts, route models, queue workers, retry/async work, execute write-back, invoke `CanonicalWriteService`, write canonical facts, mutate CandidateProfile, or append ClaimLedgerItem/ReviewEvent/WorkflowEvent rows.
- Task 10 is complete only for the current backend kernel scope: AITaskRun metadata contract and persistence exist, model/prompt/schema/task version fields exist, write-back target vocabulary exists, human-review status vocabulary exists, deterministic fail-closed governance policy exists, and regression tests prove no AI execution, no write-back execution, and no canonical mutation.
- Task 21 adds the first real AI execution baseline: `AITaskRunnerService` now validates task input/output schemas, resolves prompt resources, routes task definitions to a configured model provider, executes against DeepSeek, records input/output/tool-call/cost/trace metadata in `AITaskRun`, and supports audited replay without writing facts.
- Task 21 ships two first-class audited AI tasks only: `candidate-profile-parser.v1` and `authenticity-risk-assessor.v1`. Their outputs remain non-canonical audited artifacts; they do not append ClaimLedger, ReviewEvent, WorkflowEvent, or canonical CandidateProfile writes.
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
- Task 18A adds the first v1 product API layer for Consultant read access: generic offset-based pagination infrastructure (`PagedQuery` with DEFAULT_LIMIT=20, MAX_LIMIT=100; `PagedResult<T>` sealed as `ApiSafeResponseBody`), six consultant-specific response DTOs (`ConsultantCompanySummaryResponse`, `ConsultantCompanyDetailResponse`, `ConsultantJobSummaryResponse`, `ConsultantJobDetailResponse`, `ConsultantShortlistSummaryResponse`, `ConsultantShortlistDetailResponse`), three read-only REST controllers at `/api/consultant/companies`, `/api/consultant/jobs`, and `/api/consultant/shortlists` (list + detail endpoints), `ConsultantApiQueryService` facade using `PermissionEnforcer.requireAllowed()` plus domain services and mappers, `ResourceType.SHORTLIST` added to the access-control vocabulary, `FieldAccessPolicy` extended with Consultant allow rules for READ on COMPANY/JOB/SHORTLIST, `ApiSafeResponseBody` sealed interface extended to permit all new response types, `ApiBoundaryContractRules` updated with allowlist field definitions for all six consultant responses, and `findAllByOrganizationId` methods added to Company/Job/Shortlist persistence ports, JDBC adapters, and domain services.
- Task 18A leakage and denial tests prove: (1) missing/wrong-role/missing-org headers fail closed for all endpoints, (2) successful responses contain only allowlisted fields, (3) not-found and invalid-id paths return sanitized API errors, (4) no internal entity type/CandidateProfile/candidate identity details leak through consultant response bodies or denial messages, (5) allowlist field definitions match the actual response DTO record components.
- Task 18B (partial — Consultant write endpoints) adds CREATE and UPDATE operations for Company and Job through the Consultant API boundary: `CompanyPersistencePort.update()` and `JobPersistencePort.update()` with optimistic-locking JDBC implementations (`WHERE version = ?`, `version = version + 1`), `CompanyService.updateCompany()` and `JobService.updateJob()` domain service methods, `FieldAccessPolicy.decideConsultantAccess()` extended to allow CREATE and UPDATE on COMPANY and JOB resources, four request DTOs (`CompanyCreateRequest`, `CompanyUpdateRequest`, `JobCreateRequest`, `JobUpdateRequest`) with compact canonical-constructor validation through `ApiBoundaryContractRules.requireNonBlank()`, `ConsultantApiCommandService` facade with `PermissionEnforcer`-based access enforcement and domain-object assembly, `@PostMapping` and `@PutMapping` endpoints on `ConsultantCompanyController` and `ConsultantJobController`, `HttpMessageNotReadableException` handler for invalid request body deserialization, and `ApiBoundaryRegressionClosureTest` updated to allow POST/PUT only on consultant company/job controllers.
- Task 18B leakage and write tests prove: (1) missing/wrong-role POST/PUT returns 403, (2) missing organization POST/PUT returns 400, (3) successful POST returns 201 with response body, (4) successful PUT returns 200 with response body, (5) invalid payload (blank required fields) returns 400 via HttpMessageNotReadableException handler, (6) PostgreSQL Testcontainers integration tests prove organization-scoped isolation (company created with Org A cannot be read or updated with Org B), (7) optimistic locking (wrong version fails with IllegalStateException, correct version succeeds and increments version), (8) all 622 tests pass with 0 failures.

## Current Known Gaps

- Task 7 is complete only for the current backend kernel scope.
- Task 8A is complete only for backend contract/evaluator-skeleton scope.
- Task 8B is complete only for minimal backend service-level enforcement on client-safe projection and raw Candidate/Profile guard surfaces.
- Task 8 is complete only for the current backend kernel scope: role/resource/action/field policy contracts exist, deterministic `PermissionEvaluator` exists, fail-closed `PermissionEnforcer` exists, sensitive backend guard slice exists, and five-portal boundary negative tests exist.
- Task 9A is complete only for internal-safe API DTO/contract skeleton and contract-test scope.
- Task 9B is complete only for the first client-safe controller boundary and no-internal-entity-leakage test scope.
- Task 9 is complete only for the current backend kernel scope: API-safe DTO/envelope contracts, one client-safe candidate-card controller boundary, fail-closed authenticated access context plus explicit safe request metadata, sanitized API error/denial responses, and API leakage regression tests.
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
- Task 14 is complete only for the current backend kernel scope: the consent/disclosure service and persistence layer are hardened for chain binding, L3/L4 separation, retry-safe final disclosure persistence, organization-scoped linkage, and legacy cross-organization approver denial. It still does not add Consent/Disclosure/Unlock API/controller/UI, consent/disclosure-specific workflow execution, prior-contact/prior-application review flow, fee-agreement validation, job-activation lookup, or identity-disclosed Client read behavior.
- Real model routing, prompt execution, audited AI task execution, governed publish paths, client-facing transaction flows, evidence-backed matching, shortlist builder/send, and industry-pack calibration now exist for the current Task 60 scope. Remaining AI/matching gaps are post-100 depth: no OCR/STT execution worker, no workerized AI task queue, no live multi-provider failover, no broad autonomous canonical write-back, and no learned multi-pack calibration loop.
- Eval-feedback governance now exists through the later governance/eval console work. Remaining governance depth is automatic calibration execution and live provider/runtime control-plane behavior.
- No real re-identification risk scorer exists beyond the deterministic Task 7C placeholder.
- Client-facing shortlist review, unlock request capture, interview feedback, profile/preferences, and approved identity-disclosed client read behavior now exist through Task 32/33. Real external shortlist delivery execution remains post-100 work.
- No broad raw Candidate/Profile REST surface exists, but product-controller/API/UI slices do now exist for auth, consultant portal, client portal, document workflows, governed intake, workflow/audit, follow-ups, and the narrow client-safe candidate-card route.
- Task 19A closes the "no auth/login/session" gap only for the baseline auth infrastructure slice: login/refresh/logout exist, JWT issuance/validation exists, Spring Security exists, and refresh-token-backed persisted sessions exist.
- Existing product controllers now evaluate identity through JWT-backed `SecurityContext`, and Task 19C closes the session revocation, stale-session, and broader auth regression gap for the current baseline.
- Task 51 hardens the current directly organization-scoped identity model with
  organization-scoped constraints, composite same-organization FKs, tenant-aware
  audit/export/import boundaries, and audited same-organization support
  impersonation. No multi-organization membership/session switching exists yet;
  `identity.user_account` remains directly organization-scoped in this baseline.
- No SSO/OIDC/external identity provider integration exists yet.
- No password reset, MFA, email verification, or auth hardening flow exists yet.
- Task 33 added the first Consent/Disclosure/Unlock API/controller/UI end-to-end: candidate consent confirmation, client disclosed-candidate read, and consultant unlock approval queue. Real external notification delivery (email/WeChat/SMS), candidate self-registration flow, prior-contact/prior-application review, and fee-agreement validation remain future work.
- Identity-disclosed client read behavior now exists for approved unlocks through `ClientDisclosedCandidateController`.
- No complete product-wide RBAC/ABAC enforcement yet.
- No broad redaction pipeline or automatic text rewriting exists beyond the current deterministic client-safe summary pipeline and its shortlist/query integrations.
- Task 22 closes the first document intelligence slice for consultant-facing evidence retrieval: TXT/PDF/DOCX parsing, parsed-document/chunk/span persistence, consultant parse/summary/evidence APIs, and OCR/STT fail-closed boundary status now exist. Real OCR/STT execution, async worker orchestration, client-safe evidence exposure, and AI claim generation remain future work.
- Current-scope AI governance, executable task baselines, governed extraction/publish paths, owner/admin governance APIs, AI eval/reporting, and release gates now exist through Task 60. Remaining AI gaps are post-100 runtime-control depth: no workerized AI task queue, retry scheduler, live multi-provider failover, automatic human-review workflow engine, broad autonomous ClaimLedger/review-queue write-back, or broad canonical write execution from AI governance.
- Workflow legality validation and audited transition preview now exist through Task 26, and Task 45 now adds deterministic SLA/reminder/escalation rules plus consultant automation queue and timeline export. Broader cross-portal workflow execution, external dispatch, persisted task queues, and full BPMN automation remain future work.
- Task 46 adds the first backend-owned stale detection and conflict-resolution
  decision workflow. It records auditable refresh/conflict decisions through
  `WorkflowEvent`; it does not directly mutate canonical fields.
- A controlled Candidate/Profile product surface exists through read models, consultant/candidate UI, governed publish, refresh/conflict workflows, and client-safe disclosure paths. What remains absent by design is a generic raw CandidateProfile CRUD/search engine that bypasses the governed gates.
- Task 16-Hardening V12 resolves the previously known V10 org-scope FK gap: 7 parent UNIQUE constraints, 19 composite FKs, 8 cross-org negative tests. Nullable FK columns (interaction.job_id, document.source_item_id) and cross-cutting user FK (commission.consultant_id) intentionally excluded as documented design decisions.
- Task 17 `persistAttempt()` idempotency returns existing attempt on key match without verifying payload equivalence. Future hardening should add an idempotency equivalence hash or command fingerprint.
- Task 17 V11 `governance.canonical_write_attempt` columns `claim_ledger_item_id`, `review_event_id`, and `workflow_event_id` are ref-only uuid columns without FK constraints (intentional loose ledger design for now). Future hardening should document the FK-free design decision or add optional composite FKs.
- recruiting.* source/packet cleanup/deprecation remains deferred.
- Task 18A was only the original Consultant read endpoint slice for companies, jobs, and shortlists. Later tasks add shortlist detail/send behavior, client-safe candidate projection paths, client/candidate portal product APIs, org-scope FK hardening, and JWT-backed active controller paths. Remaining API gaps are richer filtering/full-text search, batch operations, DELETE semantics where appropriate, public ecosystem APIs, and deeper customer integration surfaces.
- Task 18B (partial) adds Consultant write endpoints (CREATE/UPDATE) for Company and Job only. Later work in Task 18C and Task 29 adds shortlist write/send behavior, but no DELETE endpoints exist, no Client-portal write endpoints exist, no Candidate/CandidateProfile write endpoints exist, no batch operations exist, and no filtering/search on write responses exists.

## Next Recommended Task

Task 42 Pilot E2E Acceptance Gate closure now returns
`CONTROLLED_PILOT_READY` for the Task 42 Usable v1 gate, and Task 60 now
returns `FULL_PRODUCT_100_READY` for the current v2.1/v2.0 specification. The
next task should be chosen from the post-100 roadmap items, such as public
launch operations, managed infrastructure, formal security/compliance
certification, live provider activation, external systems integration, customer
migration execution, support UI, or additional production-calibrated industry
packs. Use:

- `docs/roadmap/productization-roadmap.md`
- `docs/roadmap/v2.1-capability-split.md`
- `docs/roadmap/current-engineering-snapshot.md`
- `docs/roadmap/implementation-status.md`
- `docs/roadmap/known-gaps.md`

Task 19A, Task 19B, and Task 19C close the baseline auth infrastructure, controller migration, and auth/session hardening slice.
Task 34 closes the combined notification/reminder system plus candidate/client portal session and follow-up participation hardening stream.
Task 35 closes the first interview-feedback and interaction-scoped outcome-loop stream.
Task 23 backend/API scope, Task 24 Consultant Portal v1, Task 25 Company and Job Intake v1, Task 26 Workflow Engine v1, Task 27 Matching and Evidence v1, Task 28 Semiconductor Industry Pack v1, Task 29 Shortlist Builder v1, Task 30 Privacy Redaction and Re-identification v1, Task 31 Candidate Portal v1, Task 32 Client Portal v1, Task 33 Consent/Disclosure/Unlock end-to-end, Task 34 Notification/Follow-up/session closure, Task 35 Interview Feedback and Outcome Loop v1, Task 36 Placement and Commission v1, Task 37 Owner/Admin Governance v1, Task 38 Pilot Seed Data and Import Tools, Task 39 Deployment v1, Task 40 Observability, Audit, and Replay v1 backend/API/runbook subset, Task 41 Security and Privacy Hardening v1, Task 42 Pilot E2E Acceptance Gate, Task 43 route-depth closure, Task 44 AI task registry coverage, Task 45 workflow automation/SLA, Task 46 data lifecycle, Task 47 industry-pack calibration, Task 48 commercial finance hardening, Task 49 integration boundaries, Task 51 multi-organization boundary hardening, Task 52 security compliance baseline, Task 53 DR/BCP, Task 54 performance/load/cost targets, Task 55 import/migration, Task 56 support operations, Task 57 reporting/export/legal audit packages, Task 58 release management/regression suite, Task 59 onboarding playbooks, and Task 60 full-product acceptance are now complete on `main` for their documented scopes. Remaining items are post-100 roadmap or deployment/customer-go-live work tracked in `known-gaps.md` and `productization-roadmap.md`.

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
