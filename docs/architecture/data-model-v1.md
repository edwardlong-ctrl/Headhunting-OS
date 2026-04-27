# Truth Layer Data Model v1

## Scope

This document defines the first production-grade data contracts and database design for Task 2A. It is documentation only. It does not implement entities, migrations, repositories, APIs, UI, or demo data.

## Global Field Conventions

Unless explicitly stated otherwise, production tables should include:

- `id`: UUID or ULID primary key.
- `organization_id`: required tenant boundary.
- `status`: enum scoped to the object.
- `created_at`, `updated_at`: server-generated timestamps.
- `created_by`, `updated_by`: nullable only for trusted system bootstrap rows.
- `version`: optimistic concurrency integer for canonical records.
- `metadata`: JSONB for non-authoritative diagnostics only. It must not hide canonical fields.

Soft deletion should use status and retention policy fields. Production truth records should not be physically deleted by normal application actions.

## Shared Enum Catalog

| Enum | Values |
| --- | --- |
| `verification_status` | `ai_extracted`, `human_acknowledged`, `consultant_attested`, `candidate_confirmed`, `external_verified`, `system_inference`, `conflicting`, `needs_confirmation`, `rejected`, `retracted` |
| `claim_type` | `fact`, `preference`, `intent`, `risk`, `inference`, `prediction` |
| `assertion_strength` | `explicit`, `implied`, `weak_signal`, `contradiction`, `unknown` |
| `client_shareability` | `internal_only`, `client_safe`, `consent_required`, `forbidden` |
| `risk_tier` | `T0_AUTO_CLEANUP`, `T1_LOW`, `T2_MEDIUM`, `T3_HIGH`, `T4_TRANSACTION_LEGAL` |
| `actor_role` | `owner`, `consultant`, `client`, `candidate`, `admin`, `system`, `ai` |
| `redaction_level` | `L0_TEASER`, `L1_GENERALIZED`, `L2_CLIENT_SAFE`, `L3_CONSENTED_DETAIL`, `L4_IDENTITY_DISCLOSED` |
| `industry_pack_maturity` | `cold`, `seeded`, `calibrated`, `production` |

## Object Contracts

### Organization

1. Purpose: Tenant and business boundary for all truth-layer records.
2. Required fields: `organization_id`, `legal_name`, `display_name`, `status`, `default_timezone`, `created_at`, `version`.
3. Important optional fields: `billing_profile_ref`, `data_region`, `retention_policy_id`, `risk_policy_id`.
4. Status enums: `active`, `suspended`, `archived`.
5. Relationships: Parent of UserAccount, RoleAssignment, all recruiting, governance, workflow, privacy, and audit records.
6. Indexes: Unique `legal_name` where active if required by business; index `status`; index `created_at`.
7. Permission/privacy concerns: Cross-organization access must be impossible through object ids, search, exports, events, or AI runs.
8. Audit requirements: Create, suspend, archive, retention policy changes, and security policy changes require AuditLog.
9. Negative test cases: Create user without organization; read another organization's Candidate by id; assign role across organizations; archive organization with active disclosures without retention decision.
10. First production slice: Yes.

### UserAccount

1. Purpose: Authenticated human account or controlled system account.
2. Required fields: `user_account_id`, `organization_id`, `email`, `display_name`, `status`, `created_at`, `version`.
3. Important optional fields: `phone`, `identity_provider_subject`, `last_login_at`, `mfa_enabled`, `locale`.
4. Status enums: `invited`, `active`, `suspended`, `deactivated`.
5. Relationships: Belongs to Organization; has many RoleAssignments; acts in WorkflowEvent, ReviewEvent, AuditLog.
6. Indexes: Unique `(organization_id, email)`; index `(organization_id, status)`; index `identity_provider_subject`.
7. Permission/privacy concerns: Client and candidate accounts must not be escalated through client-submitted role fields.
8. Audit requirements: Invite, activation, suspension, deactivation, identity provider link, MFA changes.
9. Negative test cases: Duplicate email in same organization; spoof actor role in API command; suspended account writes canonical record; candidate account reads other candidate profile.
10. First production slice: Yes.

### RoleAssignment

1. Purpose: Role and scope binding for a user inside an organization.
2. Required fields: `role_assignment_id`, `organization_id`, `user_account_id`, `role`, `scope_type`, `scope_id`, `status`, `created_at`, `version`.
3. Important optional fields: `expires_at`, `granted_by`, `revoked_by`, `reason`.
4. Status enums: `active`, `expired`, `revoked`.
5. Relationships: Belongs to UserAccount and Organization; referenced by permission checks and AuditLog.
6. Indexes: `(organization_id, user_account_id, status)`; `(organization_id, role, status)`; unique active assignment on `(user_account_id, role, scope_type, scope_id)`.
7. Permission/privacy concerns: Scope must be evaluated server-side; client cannot self-assert role or scope.
8. Audit requirements: Grant, revoke, expiry change, scope change.
9. Negative test cases: Active duplicate role assignment; client role granted consultant scope; expired role accepted by service; cross-org scoped role.
10. First production slice: Yes.

### Candidate

1. Purpose: Canonical person-level recruiting asset, not a resume.
2. Required fields: `candidate_id`, `organization_id`, `status`, `current_profile_id`, `privacy_status`, `created_at`, `version`.
3. Important optional fields: `owner_consultant_id`, `do_not_contact_reason`, `merged_into_candidate_id`, `last_activity_at`, `default_industry_pack_id`.
4. Status enums: `new`, `profile_parsed`, `consultant_review`, `available`, `matched_to_job`, `outreach`, `interested`, `consent_pending`, `consent_confirmed`, `shortlisted`, `client_review`, `identity_disclosed`, `interviewing`, `offer_pending`, `placed`, `rejected`, `archived`, `do_not_contact`, `merged`.
5. Relationships: Has CandidateProfile versions, CandidateDocument, CandidateEvidenceItem, ClaimLedgerItem, ConsentRecord, DisclosureRecord, MatchReport, ShortlistCandidateCard, PriorApplicationClaim.
6. Indexes: `(organization_id, status)`; `(organization_id, owner_consultant_id)`; `(organization_id, current_profile_id)`; partial index `do_not_contact`; duplicate-detection support indexes on normalized identity hashes.
7. Permission/privacy concerns: Client cannot read raw Candidate before L4 disclosure; candidate user can read only self-scoped profile views.
8. Audit requirements: Create, merge, archive, do-not-contact, privacy status change, status transitions, owner changes.
9. Negative test cases: Client receives Candidate id before disclosure; AI sets status to rejected; merge loses source lineage; do-not-contact candidate enters outreach.
10. First production slice: Yes.

### CandidateProfile

1. Purpose: Versioned canonical candidate profile snapshot produced through review gates.
2. Required fields: `candidate_profile_id`, `organization_id`, `candidate_id`, `profile_version`, `status`, `field_status_map`, `created_at`, `version`.
3. Important optional fields: `headline`, `location`, `skills`, `experience_summary`, `education`, `compensation_expectation`, `availability`, `motivation`, `source_claim_ids`, `superseded_by_profile_id`.
4. Status enums: `draft`, `in_review`, `canonical`, `superseded`, `locked`.
5. Relationships: Belongs to Candidate; references ClaimLedgerItem ids and CandidateEvidenceItem ids; referenced by ConsentRecord and MatchReport.
6. Indexes: Unique `(candidate_id, profile_version)`; `(organization_id, candidate_id, status)`; GIN index on structured skills if implemented as JSONB.
7. Permission/privacy concerns: Field-level statuses drive client-safe statements; sensitive fields require redaction and consent.
8. Audit requirements: Canonical publication, supersession, field-level changes, status map changes, profile lock.
9. Negative test cases: CandidateProfile published without claim ids for material fields; bulk approve sets candidate-confirmed; stale profile used for consent; client sees compensation bottom line pre-disclosure.
10. First production slice: Yes.

### CandidateDocument

1. Purpose: Metadata and processing state for candidate-related source files.
2. Required fields: `candidate_document_id`, `organization_id`, `candidate_id`, `source_item_id`, `document_type`, `storage_ref`, `status`, `created_at`, `version`.
3. Important optional fields: `file_hash`, `language`, `document_date`, `is_latest_version`, `quarantine_reason`, `parsed_at`.
4. Status enums: `uploaded`, `classified`, `parsed`, `failed`, `quarantined`, `archived`.
5. Relationships: Belongs to Candidate and SourceItem; can produce ClaimLedgerItem and CandidateEvidenceItem records.
6. Indexes: `(organization_id, candidate_id, status)`; unique `(organization_id, file_hash)` when hash exists; `(source_item_id)`.
7. Permission/privacy concerns: Client never receives raw document storage refs before L4 disclosure and explicit policy.
8. Audit requirements: Upload, classification, quarantine, parse failure, archive, access to raw document.
9. Negative test cases: Same file ingested twice as separate latest documents; quarantined document used for evidence; client downloads raw CV pre-disclosure; missing storage_ref accepted.
10. First production slice: Yes, metadata only.

### CandidateEvidenceItem

1. Purpose: Evidence atom supporting a claim, match explanation, or profile field.
2. Required fields: `candidate_evidence_item_id`, `organization_id`, `candidate_id`, `source_item_id`, `evidence_type`, `source_span_ref`, `trust_level`, `verification_status`, `client_shareability`, `created_at`.
3. Important optional fields: `claim_ledger_item_id`, `normalized_text`, `redacted_text`, `provenance_weight`, `expires_at`, `review_event_id`.
4. Status enums: Use `verification_status`; additional lifecycle values: `active`, `stale`, `superseded`, `retracted`.
5. Relationships: Belongs to Candidate and SourceItem; supports ClaimLedgerItem, CandidateProfile, MatchReport, ShortlistCandidateCard.
6. Indexes: `(organization_id, candidate_id)`; `(source_item_id)`; `(claim_ledger_item_id)`; `(client_shareability, verification_status)`.
7. Permission/privacy concerns: Source spans can leak identity; client-safe views must use redacted_text and shareability gates.
8. Audit requirements: Evidence creation, status change, redaction change, client-shareability change, retraction.
9. Negative test cases: Evidence without source span; forbidden evidence included in client summary; system inference marked external verified; retracted evidence used in match score.
10. First production slice: Yes.

### Company

1. Purpose: Canonical client/company account and relationship record.
2. Required fields: `company_id`, `organization_id`, `name`, `status`, `relationship_stage`, `created_at`, `version`.
3. Important optional fields: `industry_pack_id`, `website`, `locations`, `payment_risk`, `bypass_risk`, `commercial_terms_ref`, `owner_consultant_id`.
4. Status enums: `prospect`, `active_client`, `paused`, `risk_review`, `archived`.
5. Relationships: Has CompanyContact, Job, PriorContactClaim, PriorApplicationClaim, DisclosureRecord.
6. Indexes: `(organization_id, status)`; normalized unique name where appropriate; `(organization_id, owner_consultant_id)`; `(industry_pack_id)`.
7. Permission/privacy concerns: Company users see only their company-scoped jobs, shortlists, and disclosure surfaces.
8. Audit requirements: Create, relationship changes, payment risk changes, commercial terms refs, archive.
9. Negative test cases: Client user reads another company's shortlist; duplicate active company created without merge review; payment risk overwritten by AI; archived company activates job.
10. First production slice: Yes.

### CompanyContact

1. Purpose: Person contact at a company with scoped portal access and communication preferences.
2. Required fields: `company_contact_id`, `organization_id`, `company_id`, `name`, `email`, `status`, `created_at`, `version`.
3. Important optional fields: `title`, `phone`, `department`, `decision_role`, `communication_preferences`, `user_account_id`.
4. Status enums: `invited`, `active`, `inactive`, `bounced`, `do_not_contact`.
5. Relationships: Belongs to Company; may link to UserAccount; actor in WorkflowEvent and client feedback.
6. Indexes: Unique `(organization_id, company_id, email)`; `(organization_id, company_id, status)`; `(user_account_id)`.
7. Permission/privacy concerns: Contact cannot access candidate identity or raw documents before disclosure gates.
8. Audit requirements: Invite, portal link, status changes, communication preference changes.
9. Negative test cases: Contact linked to user from another organization; inactive contact requests unlock; duplicate email silently overwrites contact; client contact sees internal notes.
10. First production slice: Yes.

### Job

1. Purpose: Canonical hiring mandate and transaction anchor.
2. Required fields: `job_id`, `organization_id`, `company_id`, `status`, `title`, `created_at`, `version`.
3. Important optional fields: `owner_consultant_id`, `industry_pack_id`, `commercial_status`, `location`, `seniority`, `compensation_range`, `activation_checked_at`, `closed_reason`.
4. Status enums: `draft`, `submitted`, `intake_review`, `needs_more_info`, `commercial_pending`, `contract_pending`, `activated`, `shortlist_in_progress`, `shortlist_sent`, `interviewing`, `offer_pending`, `closed`, `paused`, `cancelled`.
5. Relationships: Belongs to Company; has JobRequirement, JobScorecard, MatchReport, Shortlist, PriorApplicationClaim, DisclosureRecord.
6. Indexes: `(organization_id, company_id, status)`; `(organization_id, owner_consultant_id, status)`; `(industry_pack_id)`; full-text search on title if needed.
7. Permission/privacy concerns: Client sees only company-scoped job DTOs; commercial/internal fields are role-filtered.
8. Audit requirements: Activation, pause, close, commercial status changes, scorecard changes, status transitions.
9. Negative test cases: Job activated without commercial gate; client changes internal scorecard weight; AI closes job; cross-company shortlist attached to job.
10. First production slice: Yes.

### JobRequirement

1. Purpose: Evidence-backed requirement or preference for a job.
2. Required fields: `job_requirement_id`, `organization_id`, `job_id`, `requirement_type`, `label`, `priority`, `status`, `created_at`, `version`.
3. Important optional fields: `source_item_id`, `claim_ledger_item_id`, `must_have`, `weight`, `clarification_needed`, `evidence_examples`.
4. Status enums: `draft`, `active`, `needs_clarification`, `superseded`, `removed`.
5. Relationships: Belongs to Job; feeds JobScorecard and MatchReport; may reference SourceItem and ClaimLedgerItem.
6. Indexes: `(organization_id, job_id, status)`; `(job_id, priority)`; `(claim_ledger_item_id)`.
7. Permission/privacy concerns: Client-visible requirements must not expose internal consultant strategy or confidential commercial notes.
8. Audit requirements: Create, priority/weight changes, removal, clarification resolution.
9. Negative test cases: Requirement without job; removed requirement used in scorecard; AI creates must-have from weak signal without review; client sees internal-only requirement.
10. First production slice: Yes.

### JobScorecard

1. Purpose: Versioned scoring rubric for matching candidates to a job.
2. Required fields: `job_scorecard_id`, `organization_id`, `job_id`, `scorecard_version`, `status`, `criteria`, `created_at`, `version`.
3. Important optional fields: `industry_pack_id`, `ontology_version_id`, `weighting_policy`, `activation_gate_result`, `superseded_by_scorecard_id`.
4. Status enums: `draft`, `active`, `superseded`, `stale`.
5. Relationships: Belongs to Job; references JobRequirement, IndustryPack, OntologyVersion; used by MatchReport.
6. Indexes: Unique `(job_id, scorecard_version)`; `(organization_id, job_id, status)`; `(ontology_version_id)`.
7. Permission/privacy concerns: Client may see a simplified scorecard, not internal weights or risk policies unless allowed.
8. Audit requirements: Activation, supersession, ontology version changes, criteria and weight changes.
9. Negative test cases: Active scorecard without requirements; stale ontology used without warning; scorecard changed after MatchReport without superseding report; client writes weights.
10. First production slice: Yes.

### SourceItem

1. Purpose: Raw source metadata for files, text, notes, emails, forms, feedback, or imported records.
2. Required fields: `source_item_id`, `organization_id`, `source_type`, `origin_actor_type`, `status`, `received_at`, `created_at`.
3. Important optional fields: `storage_ref`, `content_hash`, `language`, `external_ref`, `source_timestamp`, `sensitivity_level`, `parsed_text_ref`.
4. Status enums: `uploaded`, `classifying`, `classified`, `parsing`, `parsed`, `failed`, `quarantined`, `archived`.
5. Relationships: Belongs to InformationPacket; can link to CandidateDocument, CandidateEvidenceItem, ClaimLedgerItem, AITaskRun.
6. Indexes: `(organization_id, status)`; unique `(organization_id, content_hash)` where present; `(source_type, received_at)`.
7. Permission/privacy concerns: Raw source text is internal by default; source spans can identify candidates or clients.
8. Audit requirements: Upload, parse, quarantine, archive, raw access, external reference changes.
9. Negative test cases: Quarantined source processed by AI; missing source_type; duplicate hash creates duplicate facts; source from one organization attached to another.
10. First production slice: Yes.

### InformationPacket

1. Purpose: Intake batch that groups raw sources and tracks AI/review progress toward canonical records.
2. Required fields: `information_packet_id`, `organization_id`, `packet_type`, `processing_status`, `created_at`, `version`.
3. Important optional fields: `source_item_ids`, `detected_conflicts`, `stale_fields`, `missing_fields`, `suggested_followups`, `published_entity_type`, `published_entity_id`.
4. Status enums: `uploaded`, `classifying`, `extracting`, `reviewing`, `approved`, `published`, `rejected`, `superseded`.
5. Relationships: Has SourceItem; produces Candidate, Company, Job drafts, ClaimLedgerItem, ReviewEvent, AITaskRun.
6. Indexes: `(organization_id, packet_type, processing_status)`; `(published_entity_type, published_entity_id)`; `(created_at)`.
7. Permission/privacy concerns: Packet data is internal until reviewed into role-safe DTOs.
8. Audit requirements: Status changes, publish, reject, source additions/removals, conflict resolution.
9. Negative test cases: Packet published without review event; packet source belongs to another org; rejected packet writes canonical data; client reads packet internals.
10. First production slice: Yes.

### ClaimLedgerItem

1. Purpose: Reviewable claim created from source material, AI extraction, human notes, or system inference before canonical write-back.
2. Required fields: `claim_ledger_item_id`, `organization_id`, `entity_type`, `entity_id`, `claim_type`, `assertion_strength`, `source_span_ref`, `speaker`, `verification_status`, `canonical_write_allowed`, `client_shareability`, `created_at`, `version`.
3. Important optional fields: `target_field_path`, `source_item_id`, `candidate_evidence_item_id`, `confidence`, `contradicts_claim_id`, `review_event_id`, `expires_at`.
4. Status enums: Use `verification_status`.
5. Relationships: References Candidate, CandidateProfile, Company, Job, SourceItem, CandidateEvidenceItem, ReviewEvent, AITaskRun.
6. Indexes: `(organization_id, entity_type, entity_id)`; `(target_field_path, verification_status)`; `(source_item_id)`; `(client_shareability)`.
7. Permission/privacy concerns: Client cannot read raw claim text unless transformed into client-safe DTO and allowed by shareability.
8. Audit requirements: Creation source, status changes, canonical_write_allowed changes, contradiction resolution, retraction.
9. Negative test cases: AI claim writes canonical directly; weak_signal intent becomes interested_confirmed; internal_only claim appears in shortlist; conflicting claim overwrites external_verified field.
10. First production slice: Yes.

### ReviewEvent

1. Purpose: Human or system review decision record for claims, fields, redaction, write-back, and gates.
2. Required fields: `review_event_id`, `organization_id`, `reviewer_user_id`, `target_entity_type`, `target_entity_id`, `field_path`, `risk_tier`, `decision`, `bulk_flag`, `duration_ms`, `created_at`.
3. Important optional fields: `claim_ledger_item_id`, `source_span_ref`, `reason`, `sample_audit_status`, `review_velocity_bucket`, `previous_value_hash`, `after_value_hash`.
4. Status enums: `completed`, `escalated`, `sampled_for_audit`, `failed_audit`, `superseded_by_review`.
5. Relationships: References UserAccount, ClaimLedgerItem, CandidateProfile, JobRequirement, ShortlistCandidateCard, WorkflowEvent.
6. Indexes: `(organization_id, reviewer_user_id, created_at)`; `(risk_tier, bulk_flag)`; `(target_entity_type, target_entity_id)`.
7. Permission/privacy concerns: Review payload may include sensitive source spans; role-filter read models must redact.
8. Audit requirements: All T2/T3/T4 decisions, bulk approvals, failed audit outcomes, override decisions.
9. Negative test cases: Bulk review marks external_verified; T4 review missing reason; review event created by client role; unrealistically fast high-risk approvals not flagged.
10. First production slice: Yes.

### WorkflowEvent

1. Purpose: Append-only timeline of key state transitions.
2. Required fields: `workflow_event_id`, `organization_id`, `entity_namespace`, `entity_type`, `entity_id`, `action`, `before_state`, `after_state`, `actor_user_id`, `actor_role`, `source_type`, `reason`, `occurred_at`, `created_at`.
3. Important optional fields: `entity_version`, `source_ref_id`, `ai_task_run_id`, `review_event_id`, `idempotency_key`, `correlation_id`, `previous_event_id`.
4. Status enums: `recorded`, `correction_recorded`; no destructive status.
5. Relationships: References any workflow-bearing entity; links to AITaskRun, ReviewEvent, AuditLog.
6. Indexes: `(organization_id, entity_type, entity_id, occurred_at)`; `(actor_user_id, occurred_at)`; unique `(organization_id, idempotency_key)` when present.
7. Permission/privacy concerns: Timeline DTOs must not leak raw candidate identity or internal notes to client/candidate roles.
8. Audit requirements: WorkflowEvent itself is audit evidence; corrections require a new WorkflowEvent and AuditLog.
9. Negative test cases: State changes without WorkflowEvent; event deleted; client sees identity in event reason; AI-assisted transition lacks AITaskRun id.
10. First production slice: Yes.

### ConsentRecord

1. Purpose: Versioned candidate authorization to share a profile/opportunity scope.
2. Required fields: `consent_record_id`, `organization_id`, `candidate_id`, `candidate_profile_id`, `profile_version`, `job_id`, `consent_text_version`, `status`, `requested_at`, `created_at`, `version`.
3. Important optional fields: `candidate_user_id`, `confirmed_at`, `declined_at`, `expires_at`, `revoked_at`, `revocation_reason`, `shared_field_preview_hash`.
4. Status enums: `not_requested`, `requested`, `candidate_viewed`, `consent_confirmed`, `consent_declined`, `expired`, `revoked`.
5. Relationships: Belongs to Candidate, CandidateProfile, Job; gates DisclosureRecord and ShortlistCandidateCard redaction level.
6. Indexes: `(organization_id, candidate_id, job_id, status)`; `(candidate_profile_id, profile_version)`; `(expires_at)`.
7. Permission/privacy concerns: Candidate can see own consent details; client cannot see consent internals beyond allowed unlock state.
8. Audit requirements: Request, view, confirm, decline, expire, revoke, text version changes.
9. Negative test cases: Consent confirmed without profile version; disclosure uses stale consent; revoked consent still allows unlock; consent text changed without new version.
10. First production slice: Yes.

### DisclosureRecord

1. Purpose: Auditable record of identity/contact disclosure and fee protection activation.
2. Required fields: `disclosure_record_id`, `organization_id`, `candidate_id`, `job_id`, `company_id`, `consent_record_id`, `status`, `requested_by_client_contact_id`, `approved_by_user_id`, `disclosed_at`, `created_at`, `version`.
3. Important optional fields: `disclosed_field_set`, `fee_protection_expires_at`, `denial_reason`, `prior_contact_claim_id`, `prior_application_claim_id`, `client_safe_card_id`.
4. Status enums: `not_disclosed`, `consent_confirmed`, `client_requested_unlock`, `consultant_approved`, `identity_disclosed`, `fee_protection_active`, `denied`, `revoked_for_future`.
5. Relationships: References Candidate, Job, Company, ConsentRecord, CompanyContact, ShortlistCandidateCard, PriorContactClaim, PriorApplicationClaim.
6. Indexes: `(organization_id, candidate_id, job_id)`; `(company_id, status)`; unique active disclosure per `(candidate_id, job_id, company_id)`.
7. Permission/privacy concerns: Must be created before L4 identity/contact DTO is returned.
8. Audit requirements: Unlock request, approval, denial, disclosure, fee protection activation, future revocation.
9. Negative test cases: Disclosure without consent; disclosure with blocking prior contact; identity returned before record exists; client requests unlock for another company job.
10. First production slice: Yes.

### MatchReport

1. Purpose: Evidence-backed candidate-to-job recommendation, including score, confidence, risks, and gates.
2. Required fields: `match_report_id`, `organization_id`, `candidate_id`, `job_id`, `candidate_profile_id`, `job_scorecard_id`, `status`, `match_score`, `score_confidence`, `evidence_coverage`, `ontology_version_id`, `created_at`, `version`.
3. Important optional fields: `dimension_scores`, `risk_flags`, `followup_questions`, `score_cap_reason`, `authenticity_risk`, `source_claim_ids`, `review_event_id`.
4. Status enums: `draft`, `generated`, `needs_review`, `approved_internal`, `blocked_by_gate`, `superseded`, `archived`.
5. Relationships: References Candidate, Job, CandidateProfile, JobScorecard, OntologyVersion, CandidateEvidenceItem, ClaimLedgerItem, AITaskRun.
6. Indexes: `(organization_id, job_id, status)`; `(candidate_id, job_id)`; `(score_confidence, evidence_coverage)`.
7. Permission/privacy concerns: Client receives only client-safe explanation, never raw evidence or identity clues pre-disclosure.
8. Audit requirements: Generation, score cap application, review approval, supersession, blocked gate reason.
9. Negative test cases: Score 5 with cold industry pack; explanation without evidence; weak_signal intent boosts score above cap; client sees internal risk notes.
10. First production slice: Yes, minimal contract; matching behavior later.

### Shortlist

1. Purpose: Client-review packet of one or more client-safe candidate cards for a job.
2. Required fields: `shortlist_id`, `organization_id`, `job_id`, `company_id`, `status`, `created_by`, `created_at`, `version`.
3. Important optional fields: `sent_at`, `client_viewed_at`, `review_event_id`, `client_feedback_summary`, `closed_reason`.
4. Status enums: `draft`, `ready_for_review`, `sent_to_client`, `client_viewed`, `client_feedback_pending`, `candidate_selected`, `contact_unlocked`, `interviewing`, `closed`, `withdrawn`.
5. Relationships: Belongs to Job and Company; has ShortlistCandidateCard; linked to WorkflowEvent and DisclosureRecord.
6. Indexes: `(organization_id, job_id, status)`; `(company_id, status)`; `(sent_at)`.
7. Permission/privacy concerns: Sent shortlist must contain only client-safe cards and role-filtered metadata.
8. Audit requirements: Ready for review, sent, viewed, candidate selected, unlock requested, closed/withdrawn.
9. Negative test cases: Send shortlist without card approval; include candidate from another job/company; resend stale card after consent revocation; client sees draft shortlist.
10. First production slice: Yes, minimal contract; full sending behavior later.

### ShortlistCandidateCard

1. Purpose: Redacted candidate presentation unit for a shortlist.
2. Required fields: `shortlist_candidate_card_id`, `organization_id`, `shortlist_id`, `candidate_id`, `match_report_id`, `redaction_level`, `status`, `client_alias`, `created_at`, `version`.
3. Important optional fields: `client_safe_summary`, `redacted_evidence_refs`, `reidentification_risk_score`, `unsafe_features`, `consent_record_id`, `disclosure_record_id`.
4. Status enums: `draft`, `client_safe_generated`, `risk_review_required`, `approved`, `sent`, `hidden`, `superseded`.
5. Relationships: Belongs to Shortlist; references Candidate, MatchReport, ConsentRecord, DisclosureRecord, CandidateEvidenceItem.
6. Indexes: `(organization_id, shortlist_id, status)`; `(candidate_id, match_report_id)`; unique `(shortlist_id, candidate_id)`.
7. Permission/privacy concerns: Pre-disclosure DTO must use `client_alias`, not Candidate id or exact identity details.
8. Audit requirements: Redaction generation, risk review, approval, send, hide, disclosure link.
9. Negative test cases: L2 card contains exact employer and project fingerprint; card exposes Candidate id; approved card lacks risk score; sent card uses forbidden claim.
10. First production slice: Yes.

### AITaskDefinition

1. Purpose: Versioned AI task catalog with schemas, review policy, and write-back target.
2. Required fields: `ai_task_definition_id`, `organization_id`, `task_key`, `task_version`, `status`, `input_schema_version`, `output_schema_version`, `human_review_policy`, `created_at`, `version`.
3. Important optional fields: `description`, `model_routing_policy`, `write_back_target`, `eval_suite_ref`, `owner_user_id`, `deprecated_at`.
4. Status enums: `draft`, `active`, `deprecated`, `retired`.
5. Relationships: Parent of AITaskRun; referenced by AIGovernanceService and evals.
6. Indexes: Unique `(organization_id, task_key, task_version)`; `(status)`; `(write_back_target)`.
7. Permission/privacy concerns: Only Admin/System can change active definitions; task definitions cannot authorize protected writes alone.
8. Audit requirements: Create, activate, deprecate, retire, schema version change, review policy change.
9. Negative test cases: Run deprecated task as active; task lacks output schema; task write_back_target points to consent/disclosure direct mutation; non-admin activates definition.
10. First production slice: Yes.

### AITaskRun

1. Purpose: Observability and governance record for each AI-assisted action.
2. Required fields: `ai_task_run_id`, `organization_id`, `ai_task_definition_id`, `task_version`, `status`, `input_schema_version`, `output_schema_version`, `prompt_version`, `model_provider`, `model_name`, `human_review_status`, `started_at`, `created_at`.
3. Important optional fields: `model_version`, `tool_calls`, `source_ref_ids`, `target_entity_type`, `target_entity_id`, `write_back_target`, `completed_at`, `error_code`, `cost_units`, `trace_ref`.
4. Status enums: `queued`, `running`, `succeeded`, `failed`, `blocked_by_gate`, `requires_review`, `write_back_completed`, `cancelled`.
5. Relationships: References AITaskDefinition, SourceItem, InformationPacket, ClaimLedgerItem, WorkflowEvent, ReviewEvent, MatchReport.
6. Indexes: `(organization_id, ai_task_definition_id, status)`; `(target_entity_type, target_entity_id)`; `(started_at)`; `(error_code)`.
7. Permission/privacy concerns: Raw prompts, source text, and model outputs may contain sensitive data; public/provider status must be secret-safe and role-filtered.
8. Audit requirements: Start, completion, failure, tool calls, write-back attempts, human review status changes.
9. Negative test cases: AI-assisted transition without AITaskRun; missing schema versions; successful run writes canonical field without ReviewEvent; provider secret appears in logs or DTO.
10. First production slice: Yes.

### AuditLog

1. Purpose: Immutable security, privacy, and governance audit trail.
2. Required fields: `audit_log_id`, `organization_id`, `actor_user_id`, `actor_role`, `action`, `target_entity_type`, `target_entity_id`, `result`, `occurred_at`, `created_at`.
3. Important optional fields: `ip_hash`, `user_agent_hash`, `before_state_hash`, `after_state_hash`, `reason`, `workflow_event_id`, `ai_task_run_id`, `sensitivity_level`.
4. Status enums: `recorded`, `correction_recorded`; immutable.
5. Relationships: References all sensitive domain operations; can link to WorkflowEvent and AITaskRun.
6. Indexes: `(organization_id, target_entity_type, target_entity_id, occurred_at)`; `(actor_user_id, occurred_at)`; `(action, result)`.
7. Permission/privacy concerns: Audit reads are Admin/Owner only and may need redaction for export.
8. Audit requirements: AuditLog is the audit record; corrections must be additive, not destructive.
9. Negative test cases: Protected read lacks audit log; audit row deleted; audit export leaks raw candidate data; failed authorization attempt not logged.
10. First production slice: Yes.

### PriorContactClaim

1. Purpose: Company claim that it already knows or has contacted the candidate, used to protect fee/disclosure decisions.
2. Required fields: `prior_contact_claim_id`, `organization_id`, `company_id`, `candidate_id`, `job_id`, `claim_status`, `claimed_by_contact_id`, `claimed_at`, `created_at`, `version`.
3. Important optional fields: `evidence_source_item_id`, `evidence_summary`, `review_event_id`, `decision_reason`, `blocks_disclosure`, `expires_at`.
4. Status enums: `claimed`, `evidence_requested`, `under_review`, `accepted_blocks_disclosure`, `rejected`, `expired`.
5. Relationships: References Company, CompanyContact, Candidate, Job, DisclosureRecord, WorkflowEvent.
6. Indexes: `(organization_id, company_id, candidate_id)`; `(job_id, claim_status)`; partial index where `blocks_disclosure = true`.
7. Permission/privacy concerns: Claim handling must not reveal candidate identity beyond what the client already lawfully knows.
8. Audit requirements: Claim creation, evidence request, decision, block/unblock disclosure, expiry.
9. Negative test cases: Accepted prior contact still allows disclosure; claim by wrong company blocks another company; no WorkflowEvent for decision; client uses claim endpoint to probe candidate identity.
10. First production slice: Yes.

### PriorApplicationClaim

1. Purpose: Candidate or company claim that the candidate already applied to a company/job.
2. Required fields: `prior_application_claim_id`, `organization_id`, `company_id`, `candidate_id`, `job_id`, `claim_status`, `source_actor_type`, `claimed_at`, `created_at`, `version`.
3. Important optional fields: `evidence_source_item_id`, `same_job_confidence`, `review_event_id`, `decision_reason`, `blocks_shortlist`, `expires_at`.
4. Status enums: `claimed`, `under_review`, `accepted_blocks_shortlist`, `accepted_warn_only`, `rejected`, `expired`.
5. Relationships: References Candidate, Company, Job, ShortlistCandidateCard, WorkflowEvent.
6. Indexes: `(organization_id, candidate_id, company_id)`; `(job_id, claim_status)`; partial index where `blocks_shortlist = true`.
7. Permission/privacy concerns: Candidate/application history is sensitive and must not be exposed to unrelated clients.
8. Audit requirements: Claim creation, review decision, block/warn result, expiry.
9. Negative test cases: Same-company different-job warning blocks all jobs; blocking claim ignored during shortlist send; client infers candidate identity through claim response; claim without actor source accepted.
10. First production slice: Yes.

### IndustryPack

1. Purpose: Industry-specific configuration boundary for scorecards, ontology, evidence rules, and score caps.
2. Required fields: `industry_pack_id`, `organization_id`, `key`, `display_name`, `maturity`, `status`, `created_at`, `version`.
3. Important optional fields: `description`, `default_ontology_version_id`, `score_cap_policy`, `owner_user_id`, `last_calibrated_at`.
4. Status enums: `draft`, `active`, `deprecated`, `retired`; maturity uses `cold`, `seeded`, `calibrated`, `production`.
5. Relationships: Has OntologyVersion and SkillConcept; referenced by Job, JobScorecard, MatchReport, Candidate.
6. Indexes: Unique `(organization_id, key)`; `(status, maturity)`; `(last_calibrated_at)`.
7. Permission/privacy concerns: Only Admin/System can change active pack policy; maturity affects client-visible confidence.
8. Audit requirements: Activation, maturity changes, score cap policy changes, calibration.
9. Negative test cases: Cold pack returns score 5; deprecated pack used for new job without warning; non-admin changes score cap; pack deleted while reports reference it.
10. First production slice: Yes, minimum catalog and maturity rules.

### OntologyVersion

1. Purpose: Versioned skill and concept ontology used for scoring and explanation.
2. Required fields: `ontology_version_id`, `organization_id`, `industry_pack_id`, `version_label`, `status`, `effective_from`, `review_by`, `created_at`, `version`.
3. Important optional fields: `source`, `owner_user_id`, `deprecated_at`, `stale_reason`, `replacement_ontology_version_id`.
4. Status enums: `draft`, `active`, `stale`, `deprecated`, `retired`.
5. Relationships: Belongs to IndustryPack; has SkillConcept; referenced by JobScorecard and MatchReport.
6. Indexes: Unique `(industry_pack_id, version_label)`; `(industry_pack_id, status)`; `(review_by)`.
7. Permission/privacy concerns: Stale ontology must lower score confidence and display warning in internal views.
8. Audit requirements: Activate, stale marking, deprecate, replacement mapping, review date changes.
9. Negative test cases: MatchReport lacks ontology version; stale ontology generates high-confidence score; active version has no review_by; old reports mutate when ontology changes.
10. First production slice: Yes.

### SkillConcept

1. Purpose: Living ontology concept with aliases, definitions, evidence examples, and anti-patterns.
2. Required fields: `skill_concept_id`, `organization_id`, `ontology_version_id`, `label`, `status`, `definition`, `created_at`, `version`.
3. Important optional fields: `aliases`, `role_family`, `evidence_examples`, `anti_patterns`, `effective_from`, `review_by`, `deprecated_at`, `replaced_by_skill_concept_id`.
4. Status enums: `proposed`, `active`, `deprecated`, `replaced`, `rejected`.
5. Relationships: Belongs to OntologyVersion; referenced by JobRequirement, JobScorecard, CandidateEvidenceItem, MatchReport.
6. Indexes: `(ontology_version_id, status)`; unique `(ontology_version_id, label)`; GIN index on aliases if JSONB/text array.
7. Permission/privacy concerns: Skill concept definitions influence scoring but do not expose candidate data.
8. Audit requirements: Create, activate, deprecate, replacement, definition changes, anti-pattern changes.
9. Negative test cases: Concept active without definition; alias maps unrelated skills; deprecated concept used in new scorecard without replacement warning; reports lose historical concept meaning after update.
10. First production slice: Yes, minimum ontology support; calibration depth later.

