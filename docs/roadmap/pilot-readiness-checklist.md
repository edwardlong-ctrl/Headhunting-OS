# Pilot Readiness Checklist

## Purpose

This checklist defines what must be true before the product can be called
Usable v1 / Controlled Pilot Ready.

The target is not public SaaS launch. The target is a controlled commercial
pilot where 1-3 consultants, 1-3 client companies, and invited candidates can
use the system for real recruiting transactions without bypassing truth,
privacy, audit, or workflow gates.

## Pilot Readiness Summary

Task 42 gate result: **CONTROLLED PILOT READY for the Task 42 Usable v1 gate**.
The gate explicitly tracks the eight pilot flows, negative gates, and required
validation evidence in `docs/roadmap/task-42-pilot-e2e-acceptance-gate.md`;
partial seam coverage is not treated as controlled-pilot readiness.

| Gate | Required for pilot | Current status after Task 42 baseline |
| --- | --- | --- |
| Product scope boundary | Must define kernel vs product vs pilot | Roadmap/status docs exist; still not pilot-acceptance complete |
| Real business data model | Candidate, Company, Job, Document, Interaction, Shortlist, Consent, Disclosure, Placement basics | Product model baseline exists; downstream disclosure/placement/commercial depth still partial |
| Real auth and access | Login/session, org membership, role/field enforcement | JWT/session baseline exists; broader membership and auth hardening still partial |
| Real intake | File/text upload, source lineage, AI claim extraction, review, canonical write | Governed intake/document parsing baseline exists; JD upload and broader async orchestration still partial |
| Real AI execution | Provider routing, prompt registry, schema validation, AITaskRun replay | Audited runner baseline exists; no queue/worker or governed write-back execution |
| Consultant portal | Daily workflow for intake, review, talent, jobs, matching, shortlist, timeline | Real unified workflow surface exists for the current product slice |
| Client portal | Job creation, clarification, shortlist review, unlock, feedback | Unified client workspace now exists; identity-disclosed read exists after approved unlock; JD upload and external delivery remain missing |
| Candidate portal | Upload, profile review, follow-up, opportunity, consent | Candidate portal v1 exists for follow-up, opportunity, and consent confirmation; broader upload/profile review remains partial |
| Workflow engine | Legal transitions, blockers, timeline, SLA placeholders | Legal transitions/timeline exist; SLA automation still missing |
| Privacy and disclosure | Redaction, re-identification scoring, consent/disclosure unlock workflow | Redaction plus consent/unlock/disclosure workflow baseline exists; Task 42 E2E evidence covers the pilot path; prior-contact/fee-protection depth remains future hardening |
| Deployment and operations | Staging, production-like env, backup/restore, observability, incident runbook | Partial: Task 39 local-production deployment baseline plus Task 40 provider-neutral observability/API/runbook baseline exist; not production-ready |
| Security | Auth hardening, file security, PII logs, export controls, access audit | Partial: Task 41 backend baseline covers login input policy, rate limiting, unsafe filename rejection, URL-path masking, explicit disclosure-audit export permission, and persistent access audit for Task 41 sensitive document/export surfaces; production security still deferred |

## Gate 1: Product Boundary and Planning

- [ ] `docs/roadmap/productization-roadmap.md` exists and states Task 14 is Production Kernel, not full product.
- [ ] `docs/roadmap/product-scope-after-kernel.md` exists and separates completed kernel work from product gaps.
- [ ] v2.1 remains the current source of truth.
- [ ] v2.0 UI and portal definitions remain preserved.
- [ ] Consultant remains one unified portal.
- [ ] The roadmap has no claim that Task 14 equals full usable product.
- [ ] The roadmap has explicit "not fake complete" rules.

## Gate 2: Data and Truth Layer

- [ ] Candidate aggregate supports real product fields and lifecycle.
- [ ] CandidateProfile supports complete field families, versioning, source lineage, status, stale, and conflict metadata.
- [ ] CandidateDocument metadata exists and links to SourceItem/InformationPacket.
- [ ] Company, CompanyContact, CompanyPreference, Job, JobRequirement, JobScorecard, CandidateCompanyInteraction exist.
- [ ] Shortlist and ShortlistCandidateCard persistence exists.
- [ ] InterviewFeedback, Placement, and Commission basics exist for pilot scope.
- [ ] Canonical writes go through domain services and CanonicalWriteGate.
- [ ] Gate-blocked canonical attempts are auditably recorded without mutating facts.
- [ ] All key writes are organization-scoped.

## Gate 3: API and Access

- [ ] Real login/session/auth context exists.
- [ ] Temporary header-based access context is removed from production paths.
- [ ] User, Organization, Role, Membership, and assignment data are persisted.
- [ ] Consultant can only access assigned/organization-scoped data.
- [ ] Client can only access its own company/job/shortlist data.
- [ ] Candidate can only access self-scoped candidate data and opportunity/consent requests.
- [ ] Admin/System cannot bypass canonical-write or disclosure gates by role alone.
- [ ] Client cannot read raw Candidate or raw CandidateProfile by ID enumeration, URL manipulation, API call, export, or error message.
- [ ] API DTOs do not expose internal entities.
- [ ] API error responses do not leak raw ids, stack traces, package names, source text, or PII.

## Gate 4: Document Intake and AI

- [ ] CV upload creates SourceItem/InformationPacket, not confirmed facts.
- [ ] JD upload creates SourceItem/InformationPacket, not activated jobs.
- [ ] Consultant notes and client feedback create source records and claims, not facts.
- [ ] Object storage or a local-production equivalent is configured outside PostgreSQL large-object stuffing.
- [ ] File hash, duplicate detection, MIME validation, and size validation exist.
- [ ] Malware scan placeholder or integration boundary exists and fails closed for unsafe files.
- [ ] Document text extraction and source span addressing exist.
- [ ] Real AI provider abstraction exists.
- [ ] Prompt registry and model routing config exist.
- [ ] Input/output schema validation exists.
- [ ] AITaskRun records input schema, output schema, prompt version, model version, cost, latency, status, failure, source refs, and write-back target.
- [ ] AI output enters ClaimLedger, not canonical facts.
- [ ] Replay support exists for AI tasks.
- [ ] Deterministic test provider exists for CI.

## Gate 5: Governed Intake Review

- [ ] Consultant can create candidate/company/job intake packets from UI.
- [ ] Consultant can see Clean Facts mode.
- [ ] Consultant can see Source Highlight mode.
- [ ] Claim-level review exists with approve/reject/needs-follow-up.
- [ ] Risk-tiered review is enforced.
- [ ] Bulk approve is capped at human_acknowledged.
- [ ] T3/T4 actions cannot be bulk-approved.
- [ ] Canonical write requires claim, source span, review evidence, risk tier, and gate allow.
- [ ] Candidate answers require consultant confirmation before canonical write.
- [ ] Conflicting and stale fields block transaction-ready status until resolved or intentionally downgraded.

## Gate 6: Workflow Engine

- [x] Job state machine enforces legal transitions for the current workflow slice.
- [x] Candidate state machine enforces legal transitions for the current workflow slice.
- [x] Shortlist state machine enforces legal transitions for the current workflow slice.
- [x] Consent state machine enforces legal transitions for the current workflow slice.
- [x] Disclosure state machine enforces legal transitions for the current workflow slice.
- [x] Placement/Commission state machine baseline exists for pilot-scope read-model lookup.
- [ ] Every key transition writes WorkflowEvent with actor, entity, action, before_state, after_state, reason, timestamp, AI involvement, correlation, causation.
- [x] Illegal transition attempts are blocked and auditably visible.
- [x] Timeline API exists.
- [ ] SLA due-date placeholder exists for follow-ups and blocked tasks.

## Gate 7: Matching, Industry Pack, and Shortlist

- [ ] MatchReport persistence exists.
- [ ] MatchReport includes 1-5 score, dimension scores, score confidence, evidence coverage, provenance weight, authenticity risk, ontology version, industry-pack version.
- [ ] Score cap rules are enforced before display or delivery.
- [ ] Semiconductor pack is at least seeded.
- [ ] Other packs are marked cold unless actually calibrated.
- [ ] Cold pack cannot give 5.
- [ ] Keyword-only skill match without project evidence cannot give 5.
- [ ] Semiconductor anti-patterns are tested: software QA is not IC DV, PCB layout is not PD, manufacturing quality testing is not DFT.
- [ ] Shortlist draft exists.
- [ ] Client-safe anonymous candidate cards are generated through redaction and privacy gates.
- [ ] Consultant must manually approve before shortlist is sent.
- [ ] AI cannot auto-send shortlist.

## Gate 8: Privacy, Consent, Disclosure, and Unlock

- [ ] Re-identification risk scorer exists beyond deterministic placeholder.
- [ ] L0/L1/L2/L3/L4 redaction pipeline exists.
- [ ] Exact company + rare title + exact year is generalized or blocked.
- [ ] Chip/project/product code names are removed or generalized before consent.
- [ ] Patents, papers, public talks, public profiles, and unique achievements are hidden before authorization.
- [ ] Consent binds opportunity, profile version, consent text version, and shared fields.
- [ ] Client unlock request requires active job, fee/commercial precondition, candidate consent, consultant approval, prior-contact review, prior-application review, and DisclosureRecord generation.
- [ ] L3 consented detail does not become L4 identity disclosure.
- [ ] DisclosureRecord chain can prove who requested, who approved, what consent version applied, what client-safe card version existed, and which WorkflowEvents/AITaskRuns participated.

## Gate 9: Portals

- [ ] Owner portal supports dashboard, pipeline, consultants, clients, revenue, placements, commission, risk, data quality, AI quality, audit.
- [ ] Consultant portal supports dashboard, AI intake, intake review, talent pool, candidate detail, companies, jobs, matching, outreach, shortlist, follow-ups, workflow, placements, commission, reports, settings.
- [ ] Client portal supports dashboard, job creation, JD intake, clarification, shortlist review, anonymous candidate detail, unlock request, interview feedback, profile/preferences.
- [ ] Candidate portal supports home, upload, AI-extracted profile review, follow-up form, opportunity view, consent request, shared-fields preview, status timeline.
- [ ] Admin portal supports AI policy, AI task registry, industry packs, schema, workflow rules, permissions, audit log, integrations, security, claim ledger, review quality, ontology governance, privacy redaction, model routing, eval feedback.
- [ ] UI surfaces use backend-approved API responses and do not fake confirmed facts.

## Gate 10: Deployment, Operations, Security

- [ ] Staging environment exists.
- [x] Production-like deployment baseline exists (Task 39 provider-neutral local-production compose, image build path, and runbooks; not production-ready).
- [x] Environment variable validation fails fast for staging / production profiles.
- [x] PostgreSQL migration runbook exists for empty-database Flyway startup plus Task 38 pilot import.
- [x] Backup/restore test passes for Task 42 local controlled-pilot evidence.
- [ ] Object storage is configured and tested.
- [ ] HTTPS/domain is configured.
- [x] Structured logs and correlation IDs exist for `/api/**` request boundaries and staging/production log patterns.
- [x] AITaskRun trace and WorkflowEvent search exist through Admin observability APIs.
- [ ] PII is masked in ordinary logs product-wide. Task 41 masks UUID/email path segments in normal request logs only.
- [x] Access audit exists for Task 41 sensitive document/export surfaces. Product-wide field-level access audit remains future hardening.
- [x] Export permissions exist for Admin disclosure-audit export.
- [x] File upload security tests exist for unsafe original filename rejection.
- [x] Rate limiting exists on auth login/refresh and consultant document endpoints.
- [x] Incident runbook exists for request id lookup, audit search, disclosure export, AI replay investigation, and forbidden log handling.

## Pilot E2E Acceptance Flows

All eight flows must pass without direct database mutation, seed shortcuts, or hidden manual fixes.

1. [x] Consultant uploads candidate CV + notes -> AI claims -> review -> canonical profile.
2. [x] Client or consultant uploads JD -> AI job draft -> clarification -> consultant activation.
3. [x] System generates MatchReport -> evidence-backed explanation -> score cap applies.
4. [x] Consultant selects candidates -> anonymous shortlist -> client-safe preview.
5. [x] Candidate receives opportunity/consent -> confirms authorization.
6. [x] Client reviews shortlist -> requests unlock.
7. [x] Consultant approves unlock -> DisclosureRecord -> identity disclosed through allowed path only.
8. [x] Client submits interview feedback -> outcome label -> profile/job/company suggested update enters human review.

## Negative Acceptance Flows

- [x] Client cannot fetch raw candidate by candidate id.
- [x] Client cannot infer raw candidate id from anonymous card response.
- [x] Client cannot request L4 identity disclosure without consent and consultant approval.
- [x] AI cannot write canonical facts directly.
- [x] AI cannot approve its own write-back.
- [x] Bulk approve cannot produce candidate_confirmed or external_verified.
- [x] Shortlist cannot be sent when re-identification risk is high and unresolved.
- [x] Disclosure cannot bypass prior-contact/prior-application review when claims exist.
- [x] Candidate cannot see other candidates, client-internal notes, or commercial terms.
- [x] Admin cannot mutate facts outside domain services.

## Required Validation Commands

Run these before claiming pilot readiness:

```sh
git diff --check
npm run typecheck:web
npm run build:web
docker info
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
npm run pilot:data:rebuild
npm run pilot:data:validate
npm run pilot:data:export
RTO_PILOT_DATA_ALLOW_RESET=true npm run pilot:data:reset
RTO_E2E_API_PORT=<api-port> RTO_E2E_WEB_PORT=<web-port> npm run test:e2e:pilot
```

Additional pilot-gate validation must include:

- API contract tests for all portal endpoints.
- Browser E2E tests for the eight pilot flows.
- Negative privacy and permission tests.
- Migration-from-empty-database test.
- Backup/restore test.
- Seed/import/reset/export test.
- Deployment smoke test against staging.
- Security test report covering auth, ID enumeration, file upload, export, and PII logging.
