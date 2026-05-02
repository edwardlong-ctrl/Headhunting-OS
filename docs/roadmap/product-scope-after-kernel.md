# Product Scope After Production Kernel

## Purpose

This document fixes the post-Task 14 scope boundary.

Task 0-14 created a Production Kernel. It did not create the complete v2.1/v2.0
user-facing product. Future roadmap work must keep that distinction clear.

## Source of Truth

- `docs/specs/CURRENT_SPEC.md` remains the source-of-truth pointer.
- `docs/specs/v2.1/product-spec-v2.1.md` remains the current product source of truth.
- `docs/specs/v2.0/product-spec-v2.0.md` remains the historical UI and portal baseline.
- v2.0 UI images, five-portal definitions, page tables, AI Task Registry, data objects, workflow state machines, Industry Pack structure, delivery plan, and acceptance structure must be preserved.
- Consultant is one unified portal. The two Consultant UI boards in v2.0/v2.1 are two design boards for one portal, not two portals.

## Current Completion Estimate

The honest product completion estimate is:

| Target | Current estimate | Meaning |
| --- | ---: | --- |
| Production Kernel | 85-90% | Core truth/audit/governance/access foundations are mostly complete for the current kernel scope. Some kernel-adjacent gaps remain, especially blocked-attempt audit ledger and schema cleanup. |
| Controlled Pilot / Usable v1 | 25-30% | The safety foundations exist, but real daily user workflows, auth, real AI execution, document intake, full portals, deployment, and operations are still missing. |
| Full v2.1/v2.0 Product | 30% +/- 5% | The backend foundation is valuable and deep, but the complete product still requires broad API, real workflows, real AI, five complete portals, integrations, deployment, governance consoles, and full acceptance gates. |

This estimate intentionally does not count a backend contract as a completed
user-facing feature. A feature is product-complete only when it has durable data,
service-layer enforcement, safe API contracts, appropriate UI workflow, audit,
negative tests, and acceptance evidence.

## What Task 0-14 Has Actually Built

The completed work is a production-first backend kernel:

- Source-of-truth docs and repo skeleton.
- Java 21 / Spring Boot 3 modular-monolith backend foundation.
- PostgreSQL/Flyway migrations through V9.
- ClaimLedger append persistence.
- ReviewEvent append persistence.
- WorkflowEvent append/audit foundation with idempotency, correlation, causation, and a read model skeleton.
- CanonicalWriteGate and CanonicalWriteService boundary.
- Real Spring/JDBC transaction boundary for canonical-write audit/profile-write slices.
- Governed intake source/packet/extraction/claim/review/canonical-write bridge skeleton.
- CandidateProfile contract, minimal persistence, lineage/stale/conflict metadata, and one explicit gated field-write path.
- ClientSafeCandidateCard contract, forbidden-field policy, L0-L4 vocabulary, minimal projection boundary, and deterministic re-identification placeholder.
- Identity/RBAC/ABAC contract kernel, deny-by-default evaluator, fail-closed enforcer, and five-portal negative tests.
- API-safe DTO/envelope/error contract and one narrow client-safe card endpoint.
- AITaskRun metadata contract and persistence, write-back target vocabulary, human-review status vocabulary, and deterministic governance policy.
- MatchReport/evidence/scoring contracts, score-cap policy, deterministic generation placeholder, and regression closure.
- Consent/Disclosure/Unlock backend policy, persistence, audited service boundary, and Task 14 chain hardening.
- Route-aware five-portal web shell with one narrow client-safe candidate-card flow.

## What Is Not Yet Built

The following are not complete product features yet:

- Real auth/login/session and Spring Security beyond the current baseline hardening.
- Production user/organization membership and full product-wide RBAC/ABAC.
- Broad Consultant, Client, Candidate, Owner, and Admin API surfaces.
- Real file upload, object storage, malware scan integration, and document lifecycle.
- Real AI model execution, prompt registry execution, model routing, retry, async queue, and replay UI.
- Real OCR/STT/file conversion/document intelligence/RAG/source highlight retrieval.
- Governed AI intake UI for Clean Facts and Source Highlight review.
- Full Candidate, Company, Job, JobScorecard, CandidateDocument, CandidateCompanyInteraction, Shortlist, InterviewFeedback, Placement, and Commission product behavior.
- Full workflow automation beyond the current Task 26 legality validation, blockers, consultant timeline/entity-state slice, and read-model baseline.
- Full matching engine, persisted MatchReports, client-facing safe explanations, industry ontology calibration, and outcome feedback loop.
- Real redaction pipeline and real re-identification risk scoring.
- Shortlist builder, client preview, send gate, PDF/email/WeChat-safe summary behavior.
- Candidate portal participation flows.
- Client portal job intake, shortlist, unlock, and feedback flows.
- Consent/Disclosure/Unlock API/UI and full workflow with prior-contact/prior-application/fee/job checks.
- Notifications and follow-up delivery.
- Owner/Admin governance dashboards.
- Deployment, backup/restore, observability, incident runbooks, security hardening, and privacy operations.

## Correction to the Previous Post-Task 14 Suggestion

The prior suggested roadmap has the right direction: after Task 14 the project
must move from Production Kernel to usable product. It must not keep polishing
abstract architecture forever.

However, several statements in that suggestion are now stale:

- It says the transaction boundary is only a skeleton. The current repo has a real Spring/JDBC transaction boundary with rollback tests.
- It says API DTO/client-safe response contracts are absent. The current repo has a narrow API-safe DTO/envelope contract and one client-safe card endpoint.
- It says CandidateProfile persistence is absent. The current repo has a minimal CandidateProfile persistence and one explicit gated field-write path.
- It says RBAC/ABAC is absent. The current repo has a backend kernel, not production auth.
- It says AITaskRun is only a skeleton. The current repo has metadata persistence and governance policy, but still no real execution.
- It says matching/privacy/consent are mostly absent. The current repo has backend kernels and placeholders, not full product workflows.

Therefore the next roadmap should reuse the product direction, but not repeat
already-finished kernel work as if it does not exist.

## Correct Milestones

| Milestone | Meaning | Completion label |
| --- | --- | --- |
| Task 0-14 | Backend-owned truth, audit, governance, access, and privacy foundations | Production Kernel |
| Task 15-42 | End-to-end controlled commercial pilot with real users and real data under strict controls | Usable v1 / Controlled Pilot Ready |
| Task 43-60 | Full v2.1/v2.0 product scope with all five portals, full AI task registry, governance, integrations, operations, and acceptance gates | Full Product 100% for current spec |

Task 42 is not full SaaS completion. It is the first honest controlled pilot
gate. Task 60 is the current roadmap's 100% completion target for the v2.1/v2.0
specification.

## Non-negotiable Product Invariants

- Backend owns truth.
- PostgreSQL is the target source of truth.
- AI outputs claims, not facts.
- Raw input is not fact.
- Extraction output is not fact.
- ClaimLedgerItem is claim, not fact.
- ReviewEvent is review evidence, not fact promotion.
- Risk-tiered human review comes before canonical write.
- CanonicalWriteGate must not be bypassed.
- Every key state transition must create WorkflowEvent.
- Every AI-assisted state transition must create AITaskRun and WorkflowEvent records.
- Client must never read raw Candidate or raw CandidateProfile before unlock/disclosure.
- L3 consented detail is not L4 identity disclosure.
- Bulk approve cannot produce candidate_confirmed or external_verified.
- AI cannot disclose identity, unlock contact information, make commercial promises, reject candidates, or overwrite human-confirmed facts.

## Definition of Product-Complete

A v2.1 capability is complete only when all of the following are true:

- It has durable PostgreSQL-backed data or an explicit documented reason why it is derived-only.
- It is reachable through backend domain services rather than direct table or frontend-only mutation.
- It has API DTOs that do not leak internal entities.
- It has service-level permissions and field-level visibility enforcement.
- It has positive and negative tests for the core safety boundaries.
- It writes required WorkflowEvent, AITaskRun, ReviewEvent, ConsentRecord, or DisclosureRecord records where applicable.
- It has user-facing workflow in the appropriate portal when the spec requires user participation.
- It has acceptance evidence against the v2.1/v2.0 source-of-truth behavior.
