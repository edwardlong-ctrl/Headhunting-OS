# Task 2A Contracts-First Truth Layer Design Plan

## Scope

This document defines the first production-grade truth-layer design for the AI Headhunting Transaction OS. It is a planning artifact only. It does not create entities, migrations, application logic, UI, or demo data.

Source documents read before writing:

- `AGENTS.md`
- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/architecture/production-kernel-initialization-plan.md`
- `docs/architecture/module-boundaries.md`
- `docs/architecture/security-and-privacy-principles.md`

`docs/specs/v2.0/product-spec-v2.0.md` was not needed for this task because Task 2A does not check UI preservation or v2.0 baseline content.

## Product Rules Preserved

- v2.1 is the current product source of truth.
- The product is production-first, not demo-first and not MVP-first.
- Backend services and PostgreSQL own truth.
- AI outputs claims, not facts.
- Client users must never receive raw Candidate objects before unlock/disclosure.
- Every key state transition must create a WorkflowEvent.
- Consent and disclosure records must be versioned, auditable, and append-safe.
- Consultant remains one unified portal; this task does not alter portal/UI definitions.

## Non-Goals

- No Flyway migration files.
- No Java entities, repositories, services, or controllers.
- No TypeScript schemas or generated clients.
- No frontend screens or view implementations.
- No fixture payloads, seed files, or fake demo data.
- No AI prompt or provider integration.

## Design Goal

Task 2A should leave the repository with enough contract-level specificity that the next implementation task can create database migrations, shared schemas, repository tests, and service gate tests without re-deciding the truth model.

The design is split into:

- `docs/architecture/data-model-v1.md`: object-by-object contracts and database design.
- `docs/security/client-safe-data-boundary.md`: DTO and privacy boundary rules.
- `docs/testing/truth-layer-negative-tests.md`: negative test plan for contract, gate, privacy, workflow, and AI governance failures.

## Schema Namespace Strategy

The Task 1 migration already reserved these PostgreSQL schemas. Task 2A keeps that namespace plan and assigns ownership without adding new namespaces.

| Namespace | Ownership | Planned objects |
| --- | --- | --- |
| `identity` | Organization, users, roles, tenant boundary | `organization`, `user_account`, `role_assignment` |
| `recruiting` | Canonical recruiting records and intake sources | `candidate`, `candidate_profile`, `candidate_document`, `candidate_evidence_item`, `company`, `company_contact`, `job`, `job_requirement`, `job_scorecard`, `source_item`, `information_packet`, `match_report`, `shortlist`, `shortlist_candidate_card`, `prior_contact_claim`, `prior_application_claim` |
| `governance` | AI, claim, review, ontology, and policy catalogs | `claim_ledger_item`, `review_event`, `ai_task_definition`, `ai_task_run`, `industry_pack`, `ontology_version`, `skill_concept` |
| `workflow` | Append-only state transitions and timelines | `workflow_event` |
| `privacy` | Consent, disclosure, and unlock protection | `consent_record`, `disclosure_record` |
| `audit` | Immutable security and governance audit | `audit_log` |

Cross-namespace references should use stable UUID or ULID identifiers. Avoid cross-schema cascading deletes for production records; use status changes and explicit retention policies instead.

## Migration Order

No migrations are created in Task 2A. The first implementation task should use this order so foreign keys and service gates can be introduced without circular dependencies:

1. `identity`: `organization`, `user_account`, `role_assignment`.
2. `governance` catalogs: `industry_pack`, `ontology_version`, `skill_concept`, `ai_task_definition`.
3. `recruiting` canonical anchors: `company`, `company_contact`, `job`, `job_requirement`, `job_scorecard`, `candidate`, `candidate_profile`.
4. `recruiting` intake and evidence: `source_item`, `information_packet`, `candidate_document`, `candidate_evidence_item`.
5. `governance` claim and review layer: `claim_ledger_item`, `review_event`, `ai_task_run`.
6. `workflow`: `workflow_event`.
7. `privacy`: `consent_record`, `disclosure_record`.
8. `recruiting` protection and transaction outputs: `prior_contact_claim`, `prior_application_claim`, `match_report`, `shortlist`, `shortlist_candidate_card`.
9. `audit`: `audit_log`, or earlier if the implementation needs audit triggers during migration testing.

## Service Boundary Ownership

The Java modular monolith should own the domain services. Shared contracts describe commands, events, DTOs, and enum values; they do not own writes.

| Service boundary | Owns | Must not do |
| --- | --- | --- |
| `IdentityService` | Organizations, users, role assignments, tenant checks | Expose role state as client-trustable input |
| `RecruitingIntakeService` | SourceItem, InformationPacket, document intake state | Promote raw intake to fact |
| `CandidateService` | Candidate and CandidateProfile canonical state | Send raw Candidate data to client role DTOs |
| `CompanyService` | Company and CompanyContact canonical state | Allow clients to edit another organization's relationship state |
| `JobService` | Job, JobRequirement, JobScorecard activation rules | Activate jobs without required commercial and scorecard gates |
| `EvidenceService` | CandidateEvidenceItem and source lineage | Treat evidence extraction as verified fact |
| `ClaimLedgerService` | ClaimLedgerItem lifecycle and claim-to-field mapping | Write canonical records directly |
| `ReviewGateService` | ReviewEvent, risk tier decisions, bulk approval limits | Convert bulk approve to `candidate_confirmed` or `external_verified` |
| `WorkflowService` | WorkflowEvent creation and timeline reads | Delete or overwrite workflow events |
| `PrivacyProtectionService` | ConsentRecord, DisclosureRecord, unlock and disclosure gates | Disclose identity without candidate consent, client request, consultant approval, and DisclosureRecord |
| `MatchingService` | MatchReport generation inputs, score caps, evidence coverage | Present match score as a fact or bypass ontology/evidence caps |
| `ShortlistService` | Shortlist and ShortlistCandidateCard client-safe output | Send cards without redaction and re-identification risk checks |
| `AIGovernanceService` | AITaskDefinition, AITaskRun, schema/prompt/model metadata | Let AI mutate canonical facts or protected states directly |
| `IndustryOntologyService` | IndustryPack, OntologyVersion, SkillConcept lifecycle | Score against stale ontology without confidence warning |
| `AuditService` | AuditLog and sensitive access trail | Act as the only source of domain truth |

## API and View-Model Boundary

The API boundary should separate write commands, internal read models, and client-safe DTOs.

| Boundary type | Rules |
| --- | --- |
| Write command | Validated server-side. Must include `organization_id`, actor context, idempotency key for external actions, target entity version where applicable, and reason for gate-sensitive operations. |
| Internal read model | Available only to Owner, Consultant, Admin, or Candidate self according to role policy. May include canonical fields, claims, evidence, and audit summaries. |
| Client-safe DTO | Generated server-side from approved MatchReport and ShortlistCandidateCard data. Must never include raw Candidate, direct candidate identifiers, contact information, exact identity clues, internal notes, source documents, or forbidden claim text. |
| Event DTO | WorkflowEvent and AuditLog reads must be role-filtered. Client role must not infer protected candidate identity from event payloads. |
| AI task DTO | AITaskRun reads must redact raw inputs, prompts, source spans, and provider metadata if they contain candidate, client, commercial, or sensitive internal information. |

Frontend code should consume DTOs only. It must not construct privacy decisions from raw entity objects.

## Canonical Write-Back Gate Rules

Canonical write-back gates must be service-layer rules, not UI conventions.

1. All AI outputs start as ClaimLedgerItem records or AITaskRun outputs, never as canonical fact.
2. Each target field must have a risk tier: T0, T1, T2, T3, or T4.
3. T0 cleanup may write machine-normalized metadata with logs.
4. T1 low-risk fields may be bulk accepted but only to `human_acknowledged`.
5. T2 fields require source span and field-level ReviewEvent.
6. T3 fields require explicit review and cannot be one-click bulk approved.
7. T4 transaction/legal gates require second approval, reason, WorkflowEvent, and AuditLog.
8. Bulk approve cannot produce `candidate_confirmed`, `external_verified`, consent, disclosure, offer, placement, or commission state.
9. Conflicting claims block canonical overwrite until the conflict is resolved or explicitly retained as unresolved.
10. A canonical write must record claim ids, reviewer id, previous value hash or structured before state, after state, and entity version.

## WorkflowEvent Requirements

Every key transition must create a WorkflowEvent. Minimum fields:

- `workflow_event_id`
- `organization_id`
- `entity_namespace`
- `entity_type`
- `entity_id`
- `entity_version`
- `action`
- `before_state`
- `after_state`
- `actor_user_id`
- `actor_role`
- `source_type`
- `source_ref_id`
- `ai_task_run_id` when AI assisted
- `reason`
- `idempotency_key`
- `occurred_at`
- `created_at`

WorkflowEvent records are append-only. Corrections must be represented by a new event that references the previous event.

## AI Task Run Recording Requirements

Every AI-assisted action must have an AITaskRun record that can explain what happened without making provider secrets visible.

Required AITaskRun fields:

- `ai_task_run_id`
- `organization_id`
- `task_definition_id`
- `task_version`
- `input_schema_version`
- `output_schema_version`
- `prompt_version`
- `model_provider`
- `model_name`
- `model_version`
- `tool_calls`
- `source_ref_ids`
- `target_entity_type`
- `target_entity_id`
- `write_back_target`
- `human_review_status`
- `status`
- `started_at`
- `completed_at`
- `error_code`
- `cost_units`

AI task output can propose claims, summaries, match reports, follow-ups, and workflow actions. It cannot mutate confirmed facts, consent records, disclosure records, commercial terms, placement, or commission states directly.

## Consent and Disclosure Gate Requirements

Disclosure is allowed only when all required gates pass:

- Candidate consent is confirmed.
- ConsentRecord references the exact CandidateProfile version and consent text version.
- Job is activated.
- Fee agreement or commercial protection state is active.
- Client requested unlock.
- Consultant approved disclosure.
- No accepted PriorContactClaim or blocking PriorApplicationClaim prevents disclosure.
- DisclosureRecord is created before identity/contact data leaves the server.
- WorkflowEvent and AuditLog are created for the unlock and disclosure transition.

Consent revocation must block future disclosures and mark affected client-safe outputs stale. It does not delete historical WorkflowEvent, DisclosureRecord, or AuditLog records.

## Raw Candidate Leakage Prevention

Client role APIs must be tested as hostile consumers:

- A client cannot request `/candidate`, raw candidate ids, source documents, internal notes, claim ledger internals, or exact identity clues before disclosure.
- Client-safe DTOs must be generated by server-side redaction logic.
- Pre-unlock candidate references should use shortlist card ids or scoped aliases, not canonical Candidate ids.
- Response bodies, nested objects, event timelines, errors, exports, and AI explanations must be scanned for raw Candidate fields.

Detailed rules are in `docs/security/client-safe-data-boundary.md` and test cases are in `docs/testing/truth-layer-negative-tests.md`.

## First Production Slice Recommendation

The first implementation slice should create schema and contract foundations for all objects listed in `data-model-v1.md`, but it should implement behavior only for identity, intake, claim ledger, review gates, workflow events, AI task run recording, privacy gates, and client-safe DTO validation.

Recommended next implementation boundary:

1. Add shared contract definitions and enum schemas.
2. Add Flyway migrations for the first namespace group.
3. Add repository and schema tests.
4. Add service gate tests before service implementations.
5. Add client-safe serialization tests before any client-facing candidate endpoints.

