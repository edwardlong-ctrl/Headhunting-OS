# Post-Task 14 Productization Roadmap

## From Production Kernel to Full v2.1/v2.0 Product

This roadmap starts after Task 14.

Chinese companion document: `docs/roadmap/productization-roadmap.zh-CN.md`.

Task 0-14 produced the Production Kernel: backend-owned truth, audit,
governance, access, privacy, and narrow portal/API foundations. It is not a
complete user-facing product.

The next objective is to turn the kernel into a usable recruiting transaction
system:

```text
Real job/candidate/company data
-> governed AI intake
-> Claim Ledger
-> human review
-> canonical profile/job/company
-> evidence-backed matching
-> client-safe shortlist
-> candidate consent
-> unlock/disclosure
-> interview feedback
-> offer/placement/commission
-> audit, replay, governance, and outcome feedback
```

## Source of Truth

- v2.1 is the current product source of truth.
- v2.0 remains the historical UI and portal baseline.
- This roadmap does not replace `docs/specs/CURRENT_SPEC.md`, `docs/specs/v2.1/product-spec-v2.1.md`, or `docs/specs/v2.0/product-spec-v2.0.md`.
- v2.0 UI/portal definitions must not be deleted, compressed, or replaced.
- Consultant remains one unified portal.

## Honest Current State

| Area | Current state after Task 14 | Product interpretation |
| --- | --- | --- |
| Truth layer | ClaimLedger, ReviewEvent, WorkflowEvent, CanonicalWriteGate, transaction boundary, and minimal CandidateProfile write path exist | Strong kernel foundation |
| Governed intake | SourceItem/InformationPacket/extraction/claim/review/write bridge exists with deterministic placeholder | Not real AI intake yet |
| CandidateProfile | Minimal persisted field-write path exists with lineage/stale/conflict metadata | Not full profile engine |
| Client-safe privacy | ClientSafeCandidateCard, redaction vocabulary, re-identification placeholder, and narrow endpoint exist | Not real privacy pipeline |
| RBAC/ABAC | Backend policy/evaluator/enforcer kernel exists | Not production auth/session |
| API | One narrow client-safe card endpoint exists | Not broad product API |
| AI governance | AITaskRun metadata and governance policy exist | No model calls, prompt execution, queue, replay UI, or write-back execution |
| Matching | MatchReport contracts, evidence metadata, score-cap policy, and deterministic generation placeholder exist | Not real matching engine |
| Consent/disclosure | Backend policy, persistence, service, and Task 14 hardening exist | Not full unlock/disclosure workflow or UI |
| UI | Five-portal route shell plus narrow client-safe card flow exists | Not complete portals |
| Deployment/security | Development validation exists | Not production deployment |

Current completion against the full v2.1/v2.0 product is approximately
30% +/- 5%. The foundation is deeper than a demo, but daily usable product
workflows are mostly still ahead.

## Roadmap Milestones

| Milestone | Tasks | Completion label |
| --- | --- | --- |
| Production Kernel | 0-14 | Completed for current kernel scope |
| Productization Bridge | 15-17 | Completed: planning baseline, product data model, canonical write audit |
| Operational Core | 18-20 | Real product API, auth, storage |
| Real AI Intake | 21-24 | Real AI, review, Consultant portal |
| Recruiting Transaction Core | 25-34 | Job, workflow, matching, shortlist, redaction, candidate/client loops |
| Pilot Operations | 35-42 | Outcome, placement, governance, seed, deployment, observability, security, E2E gate |
| Full Product Completion | 43-60 | Full v2.1/v2.0 product completeness and production operating model |

## Priority Classes

- P0: Required before controlled pilot.
- P1: Strongly required for a credible pilot; can be staged only with explicit risk acceptance.
- P2: Can follow Usable v1, but required before the full v2.1/v2.0 product is called 100%.

## Common Validation

Every implementation task should normally run:

```sh
git diff --check
npm run typecheck:web
npm run build:web
docker info
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

Backend-only tasks may omit frontend build only when the task explicitly states
that no web files or web contracts changed. Frontend/API tasks must include both
frontend and backend validation.

## Task 15: Product Readiness Bridge

Priority: P0

Status: **Completed** at `328dcf9`.

Goal: Convert the Task 0-14 kernel completion into a productization baseline
without pretending that Task 14 is the full product.

Must deliver:

- This productization roadmap.
- Pilot readiness checklist.
- Product scope after kernel boundary document.
- v2.1 capability split into complete / partial / missing / forbidden-to-fake.
- Usable v1 end-to-end acceptance scenarios.

Forbidden scope:

- No business code.
- No product spec rewrite.
- No claim that Task 14 is full product completion.

Acceptance:

- Roadmap states that v2.1 remains source of truth.
- Roadmap states that v2.0 UI/portal baseline is preserved.
- Roadmap states that Consultant remains one unified portal.
- Roadmap states that AI outputs claims, not facts.
- Roadmap states that Client cannot read raw Candidate before unlock/disclosure.
- Roadmap gives a task path from Production Kernel to Usable v1 and then to full product 100%.

## Task 16: Real Product Data Model Completion

Priority: P0

Status: **Completed** at `d5045ce`. V10 migration added 15 new tables; domain/port/adapter/service/tests delivered for Company, Job, Shortlist, Placement, Commission, CandidateDocument, Interaction, InterviewFeedback, ProfileFieldLineage.

Goal: Expand from kernel tables and minimal CandidateProfile persistence into
real product aggregates.

Current baseline:

- `recruiting.candidate` and `recruiting.candidate_profile` exist.
- CandidateProfile minimal field writes exist through CanonicalWriteService.
- Company/Job/Shortlist/Placement objects are not yet product-complete.

Must deliver:

- Candidate aggregate with lifecycle state and organization scope.
- Complete CandidateProfile field family support and profile versioning.
- CandidateDocument metadata linked to SourceItem/InformationPacket.
- Company and CompanyContact persistence.
- CompanyPreference baseline.
- Job, JobRequirement, JobScorecard persistence.
- CandidateCompanyInteraction persistence.
- Shortlist and ShortlistCandidateCard persistence.
- InterviewFeedback, Placement, and Commission baseline tables for downstream tasks.
- Source lineage links from canonical fields to ClaimLedgerItem, ReviewEvent, SourceItem, InformationPacket, AITaskRun, WorkflowEvent, and source spans.

Forbidden scope:

- No real AI model calls.
- No UI.
- No direct canonical write bypass.
- No Client raw Candidate visibility.

Acceptance:

- Core product entities persist and read back organization-scoped data.
- Each candidate profile field has status, source lineage, and version metadata.
- Canonical profile writes still pass through domain services and CanonicalWriteGate.
- Tests prove Client cannot read raw Candidate/Profile tables through product services.

## Task 17: Canonical Write Audit and Blocked Attempt Ledger

Priority: P0

Status: **Completed** at `66e416c`. V11 migration added `governance.canonical_write_attempt`; CanonicalWriteService now persists attempt records for all decision types (allow/block/require_review); CanonicalWriteResult carries canonicalWriteAttemptId.

Goal: Close the current kernel gap where allowed writes audit correctly, but
blocked canonical attempts do not yet have a separate persisted audit ledger.

Current baseline:

- Real Spring transaction boundary exists.
- Allowed canonical CandidateProfile field writes can commit WorkflowEvent and profile field together.
- Gate-blocked attempts do not mutate facts, but blocked attempt observability is still incomplete.

Must deliver:

- Persisted CanonicalWriteAttempt or equivalent audit record for allow/block/require_review.
- Transactional coupling for canonical write, WorkflowEvent, and write-attempt audit.
- Idempotency for repeated blocked and allowed attempts.
- Reason-code vocabulary for blocked/require-review decisions.
- Query/read model for Admin/Owner review of blocked attempts.

Forbidden scope:

- No weakening of CanonicalWriteGate.
- No AI direct write path.
- No controller-level hand-written transaction workaround.

Acceptance:

- If canonical write succeeds and WorkflowEvent fails, the transaction rolls back.
- If WorkflowEvent succeeds but canonical write fails, the transaction rolls back.
- If CanonicalWriteGate blocks, no canonical fact mutates, and the blocked attempt is auditably visible.

## Task 18: Product API Layer v1

Priority: P0

Goal: Build a broad but safe API boundary for real portal workflows.

Current baseline:

- One narrow `GET /api/client-safe/candidate-cards/{anonymousCardRef}` endpoint exists.
- API-safe DTO/envelope/error contracts exist.

Must deliver:

- `/api/consultant/*` for intake, review, talent, companies, jobs, matching, shortlist, workflow.
- `/api/client/*` for job intake, clarification, shortlist review, anonymous candidate detail, unlock request, feedback.
- `/api/candidate/*` for upload, profile review, follow-up, opportunity, consent, timeline.
- `/api/owner/*` for business/risk/quality dashboards.
- `/api/admin/*` for governance, audit, AI task, workflow, permissions, ontology, redaction.
- Pagination, filtering, sorting, and search baseline.
- Safe DTO mapping for every response.
- Error contract and validation contract.
- API contract tests and leakage tests.

Forbidden scope:

- No raw entity response DTOs.
- No raw Candidate/Profile endpoint for Client.
- No frontend-only permission control.

Acceptance:

- Client shortlist and candidate detail responses contain only client-safe DTOs before disclosure.
- Internal entity classes cannot be returned from controllers.
- Unsafe path refs, raw UUID candidate refs, and missing auth context fail closed.

## Task 19: Identity, Auth, RBAC, and ABAC Production v1

Priority: P0

Goal: Turn backend access-policy kernel into production identity and access
control.

Current baseline:

- Portal role vocabulary, field classifications, PermissionEvaluator, and PermissionEnforcer exist.
- Temporary header-based API context exists for the narrow client-safe endpoint.

Must deliver:

- User, Organization, Membership, RoleAssignment, and session model.
- Login/session baseline with Spring Security or an equivalent backend-owned security layer.
- Consultant agency organization membership.
- Client company organization membership.
- Candidate self-owned identity and profile relationship.
- Admin/System role separation.
- Field-level ABAC policies wired to API/service boundaries.
- Organization boundary tests.
- ID enumeration and cross-org negative tests.
- Replacement of temporary header-based production access context.

Forbidden scope:

- No complex SSO requirement for v1.
- No billing/multi-tenant commercial scope yet.
- No frontend-hidden-only permissions.

Acceptance:

- Client cannot access raw Candidate even with known candidate/profile ids.
- Candidate cannot read other candidates or client/commercial/internal notes.
- Admin/System cannot bypass canonical-write/disclosure gates by role alone.

## Task 20: Document Storage and SourceItem v1

Priority: P0

Goal: Let the system receive real CV, JD, notes, screenshots, and feedback
files while preserving raw-input-is-not-fact.

Current baseline:

- SourceItem and InformationPacket backend records exist.
- No real upload/object-storage/document lifecycle exists.

Must deliver:

- File upload API for Candidate CV, Job JD, consultant notes, client feedback, and supporting documents.
- Object storage abstraction.
- SourceItem metadata persistence linked to InformationPacket.
- File hash and duplicate detection.
- MIME/type/size validation.
- Malware scan placeholder or integration boundary.
- Document text extraction boundary.
- Source span addressing format.
- Access-controlled download/read APIs.

Forbidden scope:

- No large-file stuffing into canonical fact tables.
- No upload-to-fact shortcut.
- No AI read of unauthorized files.

Acceptance:

- Uploading a CV creates SourceItem/InformationPacket only.
- Confirmed CandidateProfile fields are created only after extraction, claim, review, and canonical gate.
- Unauthorized users cannot download source documents.

## Task 21: Real AI Task Runner v1

Priority: P0

Goal: Convert AITaskRun metadata governance into executable, replayable,
auditable AI tasks.

Current baseline:

- AITaskRun metadata persistence and governance policy exist.
- No real model calls, prompt execution, routing, queue, retry, or replay execution exists.

Must deliver:

- AI provider abstraction.
- Model routing config.
- Prompt registry with versioning.
- Input/output schema validation.
- Task execution lifecycle: queued/running/succeeded/failed/cancelled.
- Retry and failure state policy.
- Cost and latency logging.
- Tool-call metadata logging.
- Replay support.
- Deterministic test provider.
- Strict write-back target enforcement.

Forbidden scope:

- No AI direct canonical writes.
- No prompt hardcoding in controllers.
- No model vendor as fact source.
- No AI self-approval for human-review-required targets.

Acceptance:

- Candidate Profile Parser records input, output, model, prompt version, schema versions, failure reason, cost, latency, source refs, write-back target, and human review status.
- AI task output can create claims or review tasks, not confirmed facts.

## Task 22: Document Intelligence and Evidence Retrieval v1

Priority: P1 for pilot credibility, P0 for evidence-heavy deployments

Goal: Add real document parsing, chunks, source highlights, and evidence
retrieval.

Current baseline:

- Source spans exist as metadata references.
- No real parsing/OCR/RAG/source-highlight service exists.

Must deliver:

- Document parsing service interface.
- Parsed document chunk model.
- Source span mapping.
- OCR/STT/file-conversion worker boundary.
- Evidence retrieval API.
- RAGFlow adapter or internal adapter.
- Parsed document audit metadata.
- Evidence retrieval permission checks.

Forbidden scope:

- RAG cannot become fact source.
- Parsed chunks cannot become canonical facts directly.
- No general chatbot as primary product entry.

Acceptance:

- A claim such as "candidate has UVM coverage closure experience" can point back to CV/note source span.
- Client cannot see raw source spans that are internal-only or pre-consent identity-revealing.

## Task 23: Governed AI Intake End-to-End

Priority: P0

Goal: Complete the first real AI intake loop:
upload/source -> AI extraction -> Claim Ledger -> review -> canonical write.

Current baseline:

- Safe backend chain exists with deterministic placeholder and minimal single-field write.

Must deliver:

- Candidate intake flow.
- Company intake flow.
- Job intake flow.
- Claim Ledger Builder task integration.
- Conflict Detector integration.
- Entity Resolver integration.
- Canonical Record Builder integration.
- Review API support.
- Field-level approve/reject/needs-follow-up.
- Clean Facts and Source Highlight API models.

Forbidden scope:

- No automatic creation of formal candidate/company/job records without review.
- No overwrite of human-confirmed facts without explicit higher-risk review.
- No weak-signal intent as confirmed candidate intent.

Acceptance:

- Consultant uploads CV + WeChat note + call note.
- System creates claims with source spans.
- Only risk-tier-reviewed fields enter CandidateProfile.
- Conflicts and stale fields block transaction-ready status until resolved or downgraded.

## Task 24: Consultant Portal v1

Priority: P0

Goal: Make Consultant the daily operating surface.

Current baseline:

- Five-portal route shell exists.
- Consultant portal is not yet a workflow application.

Must deliver:

- Consultant dashboard.
- AI Intake center.
- Intake review page with Clean Facts and Source Highlight modes.
- Talent Pool list/detail.
- Company list/detail.
- Job list/detail.
- Matching review page.
- Shortlist builder entry.
- Follow-up queue.
- Workflow timeline.
- Risk/blocked actions panel.
- Audit drawer.

Forbidden scope:

- No second Consultant portal.
- No static mock-only workflows.
- No UI mutation that bypasses backend-approved APIs.

Acceptance:

- A consultant can complete candidate intake review and publish to Talent Pool without Postman.
- The UI surfaces evidence, field status, risk tier, and blocked reasons rather than hiding governance.

## Task 25: Company and Job Intake v1

Priority: P0

Goal: Let real company and job demand enter the system and become matchable.

Must deliver:

- Consultant-created company.
- Client-created company profile.
- Manual job intake.
- AI job intake from JD.
- Clarification questions.
- JobScorecard generation.
- Consultant activation gate.
- Commercial terms placeholder.

Forbidden scope:

- No automatic job activation.
- No commercial commitment automation.
- No raw JD as final scorecard.

Acceptance:

- Client uploads JD.
- AI creates job draft and missing questions.
- Consultant review is required before activation.

## Task 26: Workflow Engine v1

Priority: P0

Goal: Move from append-only audit events to legal workflow state machines.

Current baseline:

- WorkflowEvent append/audit/read-model skeleton exists.
- Transition legality validation, blocker modeling, consultant workflow timeline enrichment, and entity-state preview are now implemented on main in `c63d79a`.
- SLA due-date placeholders and automation were deferred from Task 26 and are
  now partially covered by Task 45's deterministic backend-owned SLA baseline.

Must deliver:

- Job state machine.
- Candidate state machine.
- Shortlist state machine.
- Consent state machine.
- Disclosure state machine.
- Placement/Commission state machine baseline.
- Transition validator.
- Blocker reasons.
- SLA due-date placeholder.
- Workflow timeline API.

Forbidden scope:

- No complex BPMN engine.
- No direct state update without WorkflowEvent.
- No frontend-decided transition legality.

Current implementation snapshot:

- Job/Candidate/Shortlist/Consent/Disclosure legality preview now returns stable target statuses plus blocker reasons.
- Consultant workflow timeline now exposes real `beforeStatus` / `afterStatus`.
- Consultant workflow `entity-state` now exposes `currentStatus` plus legal next actions and blockers.
- Disclosure preview now reuses real prerequisite gates instead of static-only state-machine output.
- Placement/Commission state lookup baseline exists for workflow read-model usage.

Acceptance:

- Candidate cannot jump from `new` to `identity_disclosed`.
- Disclosure cannot bypass consent and consultant approval.
- Illegal transition attempts are blocked and auditably visible.

## Task 27: Matching and Evidence v1

Priority: P0

Goal: Turn matching contracts into persisted, explainable, evidence-backed
matching.

Current baseline:

- MatchReport contract, score-cap policy, and deterministic placeholder generation exist.

Must deliver:

- MatchReport persistence.
- Dimension score model backed by JobScorecard and CandidateProfile evidence.
- Evidence coverage calculation.
- Provenance weighting v1.
- Score cap enforcement.
- Authenticity risk v1.
- Match explanation generator.
- Interview question generator.
- Negative case tests.

Forbidden scope:

- No high score from resume keyword stuffing.
- No cold-pack 5 score.
- No evidence-free explanation shown to client.

Acceptance:

- Candidate with only CV keywords and no project evidence cannot receive a 5 for Technical Fit.
- MatchReport records confidence, evidence coverage, provenance, and ontology/industry-pack versions.

## Task 28: Semiconductor Industry Pack v1

Priority: P1, but needed before a semiconductor pilot

Goal: Make one industry pack real enough to use before pretending all packs are
production-ready.

Must deliver:

- Semiconductor role families: DV/Verification, PD, DFT, Analog/Mixed Signal, Firmware/Embedded.
- SkillConcept seed data.
- Anti-pattern definitions.
- Scorecard templates.
- Interview question templates.
- Evidence examples.
- Cold/seeded maturity rules.
- OntologyVersion persistence.

Forbidden scope:

- No fake production readiness for all eight industry packs.
- No software QA as IC verification.
- No PCB layout as physical design.

Acceptance:

- A software testing resume matched to a DV role is downgraded with an anti-pattern explanation.
- Other packs remain cold unless calibrated.

## Task 29: Shortlist Builder v1

Priority: P0

Goal: Let consultants create client-safe shortlists from match reports.

Must deliver:

- Shortlist draft entity.
- Candidate selection flow.
- Anonymous candidate card generation.
- Comparison table.
- Pre-send checks.
- Client-safe summary generation.
- PDF/email/WeChat-safe summary placeholder.
- Consultant approval before send.
- Audit events.

Forbidden scope:

- No AI auto-send.
- No real name/contact/full LinkedIn before disclosure.
- No high re-identification detail in anonymous cards.

Acceptance:

- Shortlist moves from draft to sent_to_client only after consultant confirmation and privacy gates pass.

## Task 30: Privacy Redaction and Re-identification v1

Priority: P0

Goal: Replace placeholder privacy checks with real anonymous-summary risk
control.

Current baseline:

- Re-identification placeholder and L0-L4 vocabulary exist.

Must deliver:

- ReidentificationRiskAssessment persistence.
- Unsafe feature detector.
- Company name generalization.
- Project/product/chip name removal/generalization.
- Rare title/year combination risk rules.
- L0/L1/L2/L3/L4 summary generation pipeline.
- Client-safe summary gate.
- Redaction audit event.

Forbidden scope:

- No manual bypass of high-risk redaction.
- No patents/papers/public talks/unique achievements before candidate authorization.
- No internal-only evidence in client output.

Acceptance:

- "Top chip company + unique title + exact year + chip code name" is generalized or blocked before client display.

## Task 31: Candidate Portal v1

Priority: P0

Goal: Let candidates participate in profile confirmation, follow-up, opportunity
review, and consent.

Must deliver:

- Candidate home.
- Resume/profile document upload.
- AI-extracted profile review.
- Follow-up form.
- Opportunity view.
- Consent request.
- Consent text versioning.
- Shared-fields preview.
- Status timeline.

Forbidden scope:

- No open job marketplace.
- No access to other candidates or client data.
- No unlimited reuse of consent across unrelated jobs.

Acceptance:

- Candidate consent binds opportunity, profile version, consent text version, and shared fields.

## Task 32: Client Portal v1

Priority: P0

Goal: Let client companies create jobs, answer clarification, review anonymous
shortlists, request unlock, and submit feedback.

Current baseline:

- Client dashboard, manual job creation, clarification, shortlist review, anonymous candidate detail, unlock request capture, feedback submission, and client profile/preferences are implemented in the current main baseline.
- Unlock remains request-only in this task; final identity disclosure release still belongs to Task 33.
- JD upload remains deferred; the current intake path is manual job submission plus clarification.

Must deliver:

- Client dashboard.
- Manual job creation.
- JD upload job creation.
- Clarification answers.
- Shortlist review.
- Anonymous candidate detail.
- Unlock request.
- Interview feedback.
- Client profile/preferences.

Forbidden scope:

- No raw Candidate.
- No contact info.
- No internal consultant notes.
- No direct identity disclosure.

Acceptance:

- Client completes: create job -> answer clarification -> review shortlist -> request unlock -> submit feedback.

## Task 33: Consent, Disclosure, and Unlock End-to-End

Priority: P0

Goal: Turn backend consent/disclosure kernel into full transaction protection
workflow.

Current baseline:

- Backend persistence, policy, service, and chain hardening exist.
- No API/UI/workflow product flow exists.

Must deliver:

- Consent request behavior.
- Candidate consent confirmation.
- Consent versioning and shared-fields preview.
- Client unlock request.
- Consultant approval.
- PriorContactClaim.
- PriorApplicationClaim.
- Fee protection placeholder.
- DisclosureRecord generation.
- Unlock/disclosure WorkflowEvent chain.
- Identity-disclosed client read behavior only after L4 gate passes.

Forbidden scope:

- No AI unlock.
- No AI identity disclosure.
- No client bypass of consultant approval.
- No L4 without consent_confirmed.

Acceptance:

- Client unlock request is blocked if candidate consent is missing, expired, revoked, mismatched, or not bound to the requested opportunity/profile version.

## Task 34: Notification and Follow-up System v1

Priority: P1 for pilot comfort, P0 for low-touch pilot operations

Goal: Make the system push workflow tasks instead of relying on manual refresh.

Current execution note:

- Task 34 has been completed on main together with the remaining candidate-facing portal auth/session hardening and profile-participation closure work from the Task 31/19 follow-up stream. That combined slice was intentional: candidate/client reminders became useful only once candidate/client portal sessions could refresh and logout safely, candidate follow-up submissions created explicit review tasks instead of mutating facts, and the resulting reminders became visible inside the candidate/client/consultant portals with workflow audit coverage.

Must deliver:

- In-app notification.
- Email provider abstraction.
- SMS provider abstraction or placeholder.
- Candidate follow-up form delivery.
- Client clarification delivery.
- Reminder schedule.
- Notification audit.
- Unsubscribe/preference baseline.

Forbidden scope:

- No sensitive information in notifications.
- No AI auto-send of shortlist or disclosure.
- No direct canonical write from candidate/client answers.

Acceptance:

- Candidate answer about salary/location/start date creates a review task, not direct profile overwrite.

## Task 35: Interview Feedback and Outcome Loop v1

Priority: P1

Goal: Feed client interview feedback into interactions, outcomes, and suggested
updates without polluting global facts.

Must deliver:

- InterviewFeedback entity.
- Structured feedback form.
- AI feedback structurer.
- Profile update suggestions.
- Company preference update suggestions.
- Reject reason taxonomy.
- Outcome label.
- Match calibration dataset baseline.

Forbidden scope:

- No AI automatic rejection.
- No automatic overwrite of ability facts.
- No one-client feedback permanently polluting global ontology.

Acceptance:

- "Good technical fit but salary mismatch" updates that interaction and job outcome, not the candidate's global suitability for all similar jobs.

## Task 36: Placement and Commission v1

Priority: P1 for pilot, P0 for transaction-OS credibility

Goal: Move beyond recommendation into recruiting transaction operations.

Must deliver:

- Offer tracking.
- Placement record.
- Start date.
- Fee rate.
- Invoice status.
- Payment status.
- Guarantee period.
- Replacement required.
- Commission record.
- Revenue dashboard source data.

Forbidden scope:

- No full accounting system.
- No automatic offer or commission confirmation.
- No replacement for legal contract/invoice systems.

Acceptance:

- Owner can see expected fee, invoice status, payment status, and guarantee status for an offer_accepted placement.
- Owner revenue treats expected fee and paid fee as backend-owned supervision metrics: totals aggregate all related commissions, unknown amounts are disclosed as known subtotals with excluded counts, and a commission without `amount` cannot be marked paid.

## Task 37: Owner and Admin Governance v1

Priority: P1

Goal: Make business quality and system governance visible.

Must deliver:

- Owner dashboard.
- Pipeline/revenue dashboard.
- Risk dashboard.
- AI quality dashboard.
- Review quality dashboard.
- Audit search.
- Model routing config UI.
- Ontology governance view.
- Redaction policy view.
- Sample audit queue.

Forbidden scope:

- No vanity dashboard with fake frontend data.
- No Admin fact mutation outside domain services.
- No hidden override of candidate privacy gates.

Acceptance:

- Owner can see a consultant's high bulk-approve ratio and open sample audit records.

## Task 38: Pilot Seed Data and Import Tools

Priority: P1

Goal: Build repeatable pilot data without using real sensitive personal data in
public demos.

Must deliver:

- Synthetic but realistic semiconductor candidates.
- 75 talent-pool seed records.
- 5 active jobs.
- 3 under-review jobs.
- Company account seed.
- Candidate account seed.
- Source document seed.
- Reset/import/export CLI.
- Data quality validation.
- Demo/pilot scenario script.

Forbidden scope:

- No public demo with real personal sensitive information.
- No seed data bypassing normal business workflow.
- No privacy-invasive generated records.

Acceptance:

- One command rebuilds a pilot data environment and data can travel through normal UI/API workflows.

## Task 39: Deployment v1

Priority: P0

Goal: Move from local development to real accessible staging and production-like
environments.

Must deliver:

- Production config profiles.
- Environment variable validation.
- Managed PostgreSQL deployment.
- Object storage config.
- Backend deployment.
- Frontend deployment.
- HTTPS/domain.
- Migration runbook.
- Rollback docs.
- Backup/restore validation.

Forbidden scope:

- No committed secrets.
- No hand-edited production database schema.
- No skipped migration validation.

Acceptance:

- From an empty database, migrations + seed + deployment allow a consultant to log in and complete intake.

## Task 40: Observability, Audit, and Replay v1

Priority: P1, with P0 subset for pilot

Goal: Make production failures traceable and replayable.

Status after `68647b5`: the provider-neutral backend/API/runbook subset is
complete on `main` through safe request correlation, structured staging/
production log patterns, admin audit search APIs, AITaskRun trace/replay
visibility, disclosure audit export, and the incident runbook. Error dashboard,
AI cost/latency dashboard UI, external observability vendors, and product-wide
PII log audit remain deferred.

Must deliver:

- Structured logs.
- Request correlation ID.
- AITaskRun trace.
- WorkflowEvent search.
- ReviewEvent search.
- Disclosure audit export.
- Error dashboard.
- AI cost/latency dashboard.
- Production incident runbook.

Forbidden scope:

- No sensitive raw data in normal logs.
- No untracked background fixes.
- No deletion of workflow/consent/disclosure audit records.

Acceptance:

- Given a DisclosureRecord, the system can show requester, approver, consent version, client-safe card version, AI tasks, and workflow events.

## Task 41: Security and Privacy Hardening v1

Priority: P0

Goal: Make real candidate/client data safe enough for controlled pilot.

Status on `main` through `58529e4`: a controlled-pilot backend hardening
slice exists for login input policy, auth/document rate limiting, upload
filename rejection, URL-path PII masking in request logs, explicit Admin
disclosure-audit export permission, persistent access audit for Task 41
sensitive document/export surfaces, data-retention policy baseline, pinned
dependency-check configuration, and focused privacy/security regressions. This
is not a production security certification, distributed rate limiter, full PII
log audit, MFA/lockout suite, product-wide field-level access audit, or
vulnerability remediation report.

Must deliver:

- Auth/session hardening.
- Password/login policy.
- Rate limiting.
- File upload security.
- PII masking in logs.
- Access audit.
- Export permission.
- Data retention policy baseline.
- Vulnerability scan baseline.
- Privacy regression tests.

Forbidden scope:

- No fake security statement.
- No raw Candidate export to Client.
- No source document download without authorization.

Acceptance:

- Client cannot obtain unauthorized identity information by ID enumeration, URL manipulation, API call, export, logs, or error message.

## Task 42: Pilot E2E Acceptance Gate

Priority: P0

Goal: Decide whether the product is honestly Usable v1 / Controlled Pilot Ready.

Current gate status: `CONTROLLED_PILOT_READY` for the Task 42 Usable v1 gate.
The Task 42 acceptance-gate model and report now have current evidence for the
Task 38 pilot CLI chain, five-portal Playwright login coverage, S01-S08
business-flow Playwright coverage, and Task 39 backup/restore validation. See
`docs/roadmap/task-42-pilot-e2e-acceptance-gate.md`. This does not certify
public production readiness; Tasks 43-60 remain required for broader
operations, security, support, managed deployment, and product depth.

Must pass:

- Consultant uploads CV + note -> AI claims -> review -> canonical profile.
- Client/company uploads JD -> AI job draft -> clarification -> consultant activation.
- MatchReport -> evidence-backed explanation -> score cap.
- Consultant creates anonymous shortlist -> client-safe preview.
- Candidate receives opportunity/consent -> confirms authorization.
- Client reviews shortlist -> requests unlock.
- Consultant approves unlock -> DisclosureRecord -> identity disclosed.
- Client submits interview feedback -> outcome label -> suggested updates enter review.

Forbidden scope:

- No seed shortcut around workflow.
- No direct database edits to manufacture pass state.
- No hidden failed cases.

Acceptance:

- All eight pilot flows pass.
- Negative privacy, permission, canonical-write, and AI-boundary tests pass.
- System can be called Usable v1 / Controlled Commercial Pilot Ready.
- System is still not full public SaaS or full v2.1/v2.0 completion.

## Task 43: Full Portal Depth and UX Completion

Priority: P2 after pilot, required for 100%

Goal: Complete all five v2.0/v2.1 portal page groups beyond pilot paths.

Must deliver:

- Full Owner page set.
- Full Consultant page set.
- Full Client page set.
- Full Candidate page set.
- Full Admin page set.
- Responsive layouts and accessibility baseline.
- Empty/loading/error/permission states.
- Cross-page workflow continuity.

Acceptance:

- Every route named in v2.1/v2.0 has a real backend-connected page or an explicitly deferred non-100% exception. For 100%, no core v2.1 route remains a shell.

Closeout status: completed for the route-depth gate. Owner, Consultant,
Client, Candidate, and Admin v2.0/v2.1 route sets are covered by
`portalRouteContract.test.ts`; strict spec parameter names are aligned for
client anonymous candidate review and candidate opportunity/consent detail
routes; `/admin/integrations` is connected to the Admin governance read
boundary.

## Task 44: Full AI Task Registry Production Coverage

Priority: P2 after pilot, required for 100%

Goal: Implement and govern the AI Task Registry from v2.1.

Must deliver:

- Tasks 0.1-13 from the base registry.
- Tasks 14-23 from v2.1 governance registry.
- Input/output schemas.
- Prompt versions.
- Eval cases.
- Human-review policy per task.
- Write-back target policy per task.
- Replay and regression reports.

Forbidden scope:

- No scattered prompts outside registry.
- No task without schema and review policy.
- No model output as fact.

Acceptance:

- Admin can inspect task version, schema, model route, eval result, cost/latency, and replay history for every production AI task.

Closeout status: completed for the registry-coverage and Admin inspection gate.
All 28 v2.1 production AI task definitions now have registry task ids,
versions, prompt versions, classpath prompt/schema/eval artifacts,
human-review policy, write-back target policy, and governed model route
inspection. `/admin/ai-task-registry` now reads definition-first coverage and
shows version, schema, model route, eval result registration, aggregate
cost/latency, failure count, and replay history count for every production task.
This does not add worker
queues, broad write-back execution, or full business executors for every
registry-only task.

## Task 45: Full Workflow Automation and SLA Engine

Priority: P2 after pilot, required for 100%

Goal: Expand workflow from legal transitions to operational automation.

Must deliver:

- SLA rules.
- Reminder rules.
- Blocker escalation.
- AI next-best-action suggestions.
- Manual override with reason.
- Workflow rule admin view.
- Timeline exports.

Acceptance:

- Stalled consent, clarification, feedback, interview, offer, invoice, and guarantee workflows surface to the right owner with due dates and audit trail.

Closeout status: completed for the first backend-owned workflow automation and
SLA baseline. `WorkflowAutomationPolicy` now defines SLA, reminder, escalation,
owner, blocker, and next-best-action rules for consent, clarification,
feedback, interview, offer, invoice, and guarantee workflows. Consultant
workflow now exposes an automation queue and CSV timeline export derived from
the existing `WorkflowEvent` read model; manual override requests require a
non-blank reason. Admin `/admin/workflow-rules` now reads the built-in Task 45
automation coverage instead of a deferred placeholder. This does not add
external email/SMS dispatch beyond the existing notification baseline, a
persisted AIActionRecommendation table, or a full workflow/BPMN runtime.

## Task 46: Full Data Lifecycle, Deduplication, Conflict, Stale, and Merge

Priority: P2 after pilot, required for 100%

Goal: Make data quality operational rather than manual cleanup.

Must deliver:

- Candidate/company/job duplicate detection.
- High-confidence duplicate block.
- Low-confidence duplicate warning with justification.
- Merge proposal and merge audit.
- Conflict resolution workflow.
- Stale detection engine.
- Refresh workflow.
- Data retention and deletion policy execution.

Acceptance:

- Duplicate/merge/conflict/stale decisions are auditable and cannot silently overwrite confirmed facts.

## Task 47: Industry Pack Expansion and Calibration

Priority: P2 after semiconductor pilot, required for 100%

Goal: Bring all v2.1 industry packs to honest maturity states.

Must deliver:

- General pack.
- Semiconductor pack production calibration.
- Finance pack.
- Healthcare pack.
- Internet/AI pack.
- Sales pack.
- Executive search pack.
- Manufacturing pack.
- Gold cases and negative cases per pack.
- Drift detection and review queues.

Forbidden scope:

- No fake production label for uncalibrated packs.
- No cold pack 5 scores.

Acceptance:

- Every pack has maturity, ontology version, review_by, gold cases, negative cases, anti-patterns, and score caps.

## Task 48: Commercial and Finance Operations Hardening

Priority: P2 after pilot, required for 100%

Goal: Complete placement, fee protection, invoice, guarantee, and commission
operations to v2.1 scope.

Must deliver:

- Fee agreement tracking.
- Invoice readiness workflow.
- Invoice sent/paid states.
- Guarantee active/completed/replacement states.
- Commission calculation inputs.
- Owner revenue reporting.
- Export to accounting process.

Acceptance:

- Placement-to-paid lifecycle is auditable without pretending to replace the official accounting system.

## Task 49: Integrations v1

Priority: P2 after pilot, required for 100%

Goal: Connect the product to real operating channels.

Must deliver:

- Email provider.
- SMS provider or production placeholder.
- Calendar integration.
- OCR/STT service.
- ATS/HRIS import/export baseline.
- WeChat/email-safe summary export.
- Webhook/event integration boundary.

Forbidden scope:

- No integration writes confirmed facts directly.
- No un-audited outbound sensitive data.

Acceptance:

- External inputs become SourceItem/InformationPacket/claims first, and outbound messages are audited.

## Task 50: Governance, Eval, and Ontology Production Console

Priority: P2 after pilot, required for 100%

Goal: Make quality governance a first-class Admin/Owner product.

Must deliver:

- Eval dashboard.
- Negative case generator.
- Review quality signals.
- Model routing console.
- Cost/latency dashboard.
- Ontology drift dashboard.
- Redaction incident dashboard.
- AI resume authenticity risk dashboard.

Acceptance:

- Admin/Owner can find AI task failures, hallucination risks, stale ontology warnings, privacy incidents, and low-quality review patterns without database access.

## Task 51: Multi-organization Boundary Hardening

Priority: P2 after pilot, required for 100%

Goal: Make organization boundaries production-grade across all product surfaces.

Must deliver:

- Organization-scoped unique constraints.
- Cross-org negative tests for every major API.
- Tenant-aware audit search.
- Tenant-aware exports.
- Tenant-aware seed/import tools.
- Support/admin impersonation policy with audit.

Acceptance:

- A user from one organization cannot infer, search, export, or access another organization's raw or derived private data.

## Task 52: Production Security Compliance Baseline

Priority: P2 after pilot, required for 100%

Goal: Complete security and privacy controls beyond pilot hardening.

Must deliver:

- Threat model.
- Access review process.
- Privacy/data-retention runbook.
- Key/secret rotation.
- Dependency vulnerability remediation.
- Pen-test issue remediation.
- Security regression suite.

Acceptance:

- Security issues found in baseline scan and review are tracked to closure or explicitly risk-accepted before 100% claim.

## Task 53: Disaster Recovery and Business Continuity

Priority: P2 after pilot, required for 100%

Goal: Prove the system can recover from operational failure.

Must deliver:

- Backup schedule.
- Restore drill.
- Migration rollback drill.
- Object storage recovery.
- AI provider outage playbook.
- Notification provider outage playbook.
- Incident severity levels.

Acceptance:

- Restore drill proves a recent backup can recover database and documents into a working environment.

## Task 54: Performance, Load, and Cost Targets

Priority: P2 after pilot, required for 100%

Goal: Define and meet realistic performance and AI cost envelopes.

Must deliver:

- API latency targets.
- Portal interaction targets.
- AI task latency/cost budgets.
- Batch parsing throughput.
- Matching throughput.
- Load tests.
- Cost alerts.

Acceptance:

- Pilot-sized and expected production-sized workloads meet documented latency and cost targets.

## Task 55: Data Import and Migration from Existing Systems

Priority: P2 after pilot, required for 100%

Goal: Let real teams bring historical recruiting data into the governed system.

Must deliver:

- CSV import.
- Legacy ATS/CRM import mapping.
- Resume/document batch import.
- Import validation report.
- Duplicate detection during import.
- Rollback/reset behavior.

Acceptance:

- Historical data imports into SourceItem/InformationPacket/claims/canonical records through governed paths, with validation and rollback.

## Task 56: Support and Operations Tooling

Priority: P2 after pilot, required for 100%

Goal: Give operators safe tools to support users without direct database fixes.

Must deliver:

- User support lookup.
- Audit-safe resend/retry actions.
- AI task retry/replay tools.
- Failed notification retry.
- Data correction request workflow.
- Support action audit.

Forbidden scope:

- No support backdoor around domain services.

Acceptance:

- Common support issues can be handled through audited tools rather than manual database edits.

## Task 57: Reporting, Exports, and Legal Audit Packages

Priority: P2 after pilot, required for 100%

Goal: Make business and compliance exports productized.

Must deliver:

- Owner reports.
- Consultant activity reports.
- Client-facing shortlist and feedback exports.
- Candidate personal-data export.
- Disclosure audit export.
- Placement/commission export.
- Data retention export/delete evidence.

Acceptance:

- Exports respect role, organization, consent, disclosure, and field-level visibility policies.

## Task 58: Release Management and Regression Suite

Priority: P2 after pilot, required for 100%

Goal: Make releases repeatable and safe.

Must deliver:

- CI pipeline.
- Migration validation.
- Backend regression suite.
- Frontend regression suite.
- Browser E2E suite.
- Privacy/security negative suite.
- AI eval regression suite.
- Release checklist.

Acceptance:

- A release cannot be called ready unless tests, migrations, E2E, privacy, and eval gates pass.

## Task 59: Pilot-to-Production Onboarding Playbooks

Priority: P2 after pilot, required for 100%

Goal: Turn controlled pilot learnings into repeatable customer onboarding.

Must deliver:

- Customer onboarding checklist.
- Consultant training flow.
- Client training flow.
- Candidate consent FAQ.
- Admin setup guide.
- Data import guide.
- Risk review guide.
- Go-live checklist.

Acceptance:

- A new pilot customer can be onboarded without engineering intervention except configured integrations and approved data import.

## Task 60: Full Product Acceptance Gate

Priority: P2, final 100% gate for current v2.1/v2.0 spec

Goal: Decide whether the current v2.1/v2.0 product plan is fully implemented.

Must pass:

- Every v2.0/v2.1 five-portal page group has real route, API, state, permission behavior, and acceptance evidence.
- Every v2.1 AI task registry entry has schema, prompt version, execution policy, AITaskRun record, human-review/write-back policy, and eval evidence.
- Every core data object in v2.1 has contract, persistence or derived model, service boundary, access policy, audit behavior, and tests.
- Every workflow state machine has transition legality validation and WorkflowEvent audit.
- Matching, industry pack, evidence coverage, score cap, provenance, authenticity risk, ontology version, and outcome feedback are implemented.
- Consent, disclosure, unlock, prior contact, prior application, fee protection, placement, commission, and audit export are implemented.
- Security, privacy, observability, backup/restore, deployment, support, import/export, and release gates pass.
- v2.0 UI/portal definitions are preserved.
- No "fake completed" shell remains for a core v2.1 requirement.

Acceptance:

- The system can be called Full v2.1/v2.0 Product 100% for the current specification.
- This still does not mean every future commercial SaaS feature exists. Billing, marketplace scale, advanced analytics, deep ATS ecosystems, and additional industry production packs may continue as post-100% roadmap items if they were not required by v2.1/v2.0.

## Recommended Execution Order

The shortest honest route to usable product is:

```text
15 -> 16 -> 17 -> 18 -> 19 -> 20
-> 21 -> 22 -> 23 -> 24
-> 25 -> 26 -> 27 -> 28 -> 29 -> 30 -> 31 -> 32 -> 33 -> 34
-> 38 -> 39 -> 41 -> 42
```

Tasks 35, 36, 37, and 40 can run before Task 42 when pilot scope requires them,
but they should not delay the first controlled pilot unless the pilot depends on
placements, commissions, governance dashboard depth, or production incident
readiness.

After Task 42, complete Tasks 43-60 to reach full v2.1/v2.0 implementation.

## Do Not Do Now

- Do not build eight fake production-ready industry packs.
- Do not split into microservices before the modular monolith is product-complete.
- Do not make chat the main product entry.
- Do not let RAG, MCP, LLM vendors, or low-code tools become source of truth.
- Do not prioritize decorative UI over real workflow actions, privacy gates, consent, disclosure, and audit.
- Do not let seed data bypass the same workflow that real data must use.
- Do not claim pilot readiness until Task 42 passes.
- Do not claim full product completion until Task 60 passes.
