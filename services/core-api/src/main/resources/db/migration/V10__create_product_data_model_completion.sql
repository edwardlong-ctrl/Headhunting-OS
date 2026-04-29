-- ============================================================================
-- V10: Product Data Model Completion (Task 16)
--
-- Adds all remaining core product entities required for end-to-end
-- recruiting transaction workflows:
--
--   16A: candidate_profile field expansion + profile_field_lineage
--   16B: company, company_contact, company_preference, job,
--        job_requirement, job_scorecard
--   16C: candidate_document, candidate_company_interaction,
--        shortlist, shortlist_candidate_card
--   16D: interview_feedback, placement, commission
--
-- All new tables follow recruiting.* conventions: uuid PK,
-- organization_id NOT NULL FK indexed, text status with CHECK,
-- standard audit columns.
-- ============================================================================

-- === Subtask 16A: Candidate Profile Hardening ================================

-- Add denormalized top-level columns for searchable v2.1 field families.
-- The JSONB field-document system in metadata remains authoritative for lineage.
ALTER TABLE recruiting.candidate_profile
  ADD COLUMN IF NOT EXISTS name text
    CHECK (name IS NULL OR btrim(name) <> ''),
  ADD COLUMN IF NOT EXISTS email text
    CHECK (email IS NULL OR btrim(email) <> ''),
  ADD COLUMN IF NOT EXISTS phone text
    CHECK (phone IS NULL OR btrim(phone) <> ''),
  ADD COLUMN IF NOT EXISTS citizenship text
    CHECK (citizenship IS NULL OR btrim(citizenship) <> ''),
  ADD COLUMN IF NOT EXISTS industry text
    CHECK (industry IS NULL OR btrim(industry) <> ''),
  ADD COLUMN IF NOT EXISTS projects jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS portfolio jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS work_history jsonb NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX IF NOT EXISTS candidate_profile_org_name_idx
  ON recruiting.candidate_profile (organization_id, name)
  WHERE name IS NOT NULL;
CREATE INDEX IF NOT EXISTS candidate_profile_org_email_idx
  ON recruiting.candidate_profile (organization_id, email)
  WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS candidate_profile_org_industry_idx
  ON recruiting.candidate_profile (organization_id, industry)
  WHERE industry IS NOT NULL;

-- Per-field-per-profile lineage rows, queryable across profiles.
-- Supplements the in-JSONB lineage with cross-cutting lookups
-- like "which profiles were affected by source_item X?"
CREATE TABLE recruiting.profile_field_lineage (
  profile_field_lineage_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  candidate_profile_id uuid NOT NULL
    REFERENCES recruiting.candidate_profile (candidate_profile_id),
  candidate_id uuid NOT NULL
    REFERENCES recruiting.candidate (candidate_id),
  field_path text NOT NULL CHECK (btrim(field_path) <> ''),
  source_type text NOT NULL CHECK (
    source_type IN (
      'claim_ledger_item',
      'review_event',
      'source_item',
      'information_packet',
      'intake_extraction_run',
      'workflow_event',
      'source_span',
      'external_evidence'
    )
  ),
  source_id text NOT NULL CHECK (btrim(source_id) <> ''),
  source_trust text CHECK (source_trust IS NULL OR btrim(source_trust) <> ''),
  provenance_label text CHECK (provenance_label IS NULL OR btrim(provenance_label) <> ''),
  recorded_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX profile_field_lineage_org_profile_path_idx
  ON recruiting.profile_field_lineage (organization_id, candidate_profile_id, field_path);
CREATE INDEX profile_field_lineage_org_candidate_path_idx
  ON recruiting.profile_field_lineage (organization_id, candidate_id, field_path);
CREATE INDEX profile_field_lineage_org_source_type_idx
  ON recruiting.profile_field_lineage (organization_id, source_type, source_id);

COMMENT ON TABLE recruiting.profile_field_lineage IS
  'Task 16A: queryable per-field lineage linking canonical profile fields to their provenance sources (ClaimLedgerItem, ReviewEvent, SourceItem, InformationPacket, AITaskRun, WorkflowEvent, source spans, external evidence). Supplements the in-JSONB lineage stored in candidate_profile.metadata.';

-- === Subtask 16B: Company + Job =============================================

CREATE TABLE recruiting.company (
  company_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  name text NOT NULL CHECK (btrim(name) <> ''),
  display_name text CHECK (display_name IS NULL OR btrim(display_name) <> ''),
  industry text CHECK (industry IS NULL OR btrim(industry) <> ''),
  website text CHECK (website IS NULL OR btrim(website) <> ''),
  headquarters_location text CHECK (headquarters_location IS NULL OR btrim(headquarters_location) <> ''),
  size_band text CHECK (size_band IS NULL OR btrim(size_band) <> ''),
  status text NOT NULL CHECK (status IN ('new', 'active', 'inactive', 'archived')),
  payment_reliability text CHECK (payment_reliability IS NULL OR btrim(payment_reliability) <> ''),
  owner_consultant_id uuid REFERENCES identity.user_account (user_account_id),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX company_org_status_idx
  ON recruiting.company (organization_id, status);
CREATE UNIQUE INDEX company_org_name_uidx
  ON recruiting.company (organization_id, lower(name))
  WHERE status <> 'archived';
CREATE INDEX company_org_industry_idx
  ON recruiting.company (organization_id, industry)
  WHERE industry IS NOT NULL;
CREATE INDEX company_org_owner_idx
  ON recruiting.company (organization_id, owner_consultant_id)
  WHERE owner_consultant_id IS NOT NULL;

COMMENT ON TABLE recruiting.company IS
  'Task 16B: client company entity. Organization-scoped. Not exposed raw to Client portal; client-safe projection is separate.';

CREATE TABLE recruiting.company_contact (
  company_contact_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  company_id uuid NOT NULL
    REFERENCES recruiting.company (company_id),
  name text NOT NULL CHECK (btrim(name) <> ''),
  title text CHECK (title IS NULL OR btrim(title) <> ''),
  email text CHECK (email IS NULL OR btrim(email) <> ''),
  phone text CHECK (phone IS NULL OR btrim(phone) <> ''),
  role_type text CHECK (role_type IS NULL OR role_type IN (
    'hr', 'ta', 'hiring_manager', 'executive', 'other'
  )),
  is_primary boolean NOT NULL DEFAULT false,
  status text NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX company_contact_org_company_idx
  ON recruiting.company_contact (organization_id, company_id);
CREATE INDEX company_contact_org_email_idx
  ON recruiting.company_contact (organization_id, email)
  WHERE email IS NOT NULL;

COMMENT ON TABLE recruiting.company_contact IS
  'Task 16B: contact person within a client company.';

CREATE TABLE recruiting.company_preference (
  company_preference_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  company_id uuid NOT NULL
    REFERENCES recruiting.company (company_id),
  preference_key text NOT NULL CHECK (btrim(preference_key) <> ''),
  preference_value jsonb NOT NULL,
  notes text CHECK (notes IS NULL OR btrim(notes) <> ''),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE UNIQUE INDEX company_preference_org_company_key_uidx
  ON recruiting.company_preference (organization_id, company_id, preference_key);

COMMENT ON TABLE recruiting.company_preference IS
  'Task 16B: key-value preference records per company (sourcing channels, feedback style, contractual defaults, etc.).';

CREATE TABLE recruiting.job (
  job_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  company_id uuid NOT NULL
    REFERENCES recruiting.company (company_id),
  title text NOT NULL CHECK (btrim(title) <> ''),
  description text,
  location jsonb,
  seniority_band text CHECK (seniority_band IS NULL OR btrim(seniority_band) <> ''),
  role_family text CHECK (role_family IS NULL OR btrim(role_family) <> ''),
  employment_type text CHECK (employment_type IS NULL OR btrim(employment_type) <> ''),
  compensation jsonb,
  status text NOT NULL CHECK (
    status IN (
      'draft',
      'submitted',
      'intake_review',
      'needs_more_info',
      'commercial_pending',
      'contract_pending',
      'activated',
      'shortlist_in_progress',
      'shortlist_sent',
      'interviewing',
      'offer_pending',
      'closed',
      'paused',
      'cancelled'
    )
  ),
  commercial_terms jsonb,
  owner_consultant_id uuid REFERENCES identity.user_account (user_account_id),
  activated_at timestamptz,
  closed_at timestamptz,
  close_reason text CHECK (close_reason IS NULL OR btrim(close_reason) <> ''),
  industry_pack_id uuid,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX job_org_status_idx
  ON recruiting.job (organization_id, status);
CREATE INDEX job_org_company_idx
  ON recruiting.job (organization_id, company_id);
CREATE INDEX job_org_owner_idx
  ON recruiting.job (organization_id, owner_consultant_id)
  WHERE owner_consultant_id IS NOT NULL;

COMMENT ON TABLE recruiting.job IS
  'Task 16B: job requisition entity. Organization-scoped, linked to company.';

CREATE TABLE recruiting.job_requirement (
  job_requirement_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  job_id uuid NOT NULL
    REFERENCES recruiting.job (job_id),
  requirement_type text NOT NULL CHECK (btrim(requirement_type) <> ''),
  label text NOT NULL CHECK (btrim(label) <> ''),
  importance text NOT NULL CHECK (importance IN ('must_have', 'nice_to_have', 'preferred')),
  detail jsonb,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX job_requirement_org_job_idx
  ON recruiting.job_requirement (organization_id, job_id);
CREATE INDEX job_requirement_org_type_label_idx
  ON recruiting.job_requirement (organization_id, requirement_type, label);

COMMENT ON TABLE recruiting.job_requirement IS
  'Task 16B: individual job requirement (skills, experience, education, etc.). Searchable across jobs.';

CREATE TABLE recruiting.job_scorecard (
  job_scorecard_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  job_id uuid NOT NULL
    REFERENCES recruiting.job (job_id),
  dimensions jsonb NOT NULL,
  scoring_guidance text,
  status text NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'archived')),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE UNIQUE INDEX job_scorecard_org_job_active_uidx
  ON recruiting.job_scorecard (organization_id, job_id)
  WHERE status = 'active';

COMMENT ON TABLE recruiting.job_scorecard IS
  'Task 16B: scoring dimensions per job. Dimensions stored as JSONB array. One active scorecard per job.';

-- === Subtask 16C: CandidateDocument + Interaction + Shortlist ================

CREATE TABLE recruiting.candidate_document (
  candidate_document_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  candidate_id uuid NOT NULL
    REFERENCES recruiting.candidate (candidate_id),
  document_type text NOT NULL CHECK (btrim(document_type) <> ''),
  title text CHECK (title IS NULL OR btrim(title) <> ''),
  storage_ref text CHECK (storage_ref IS NULL OR btrim(storage_ref) <> ''),
  content_hash text CHECK (content_hash IS NULL OR btrim(content_hash) <> ''),
  source_item_id uuid REFERENCES recruiting.source_item (source_item_id),
  language text CHECK (language IS NULL OR btrim(language) <> ''),
  status text NOT NULL CHECK (status IN ('active', 'superseded', 'archived')),
  superseded_by_document_id uuid
    REFERENCES recruiting.candidate_document (candidate_document_id),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX candidate_document_org_candidate_idx
  ON recruiting.candidate_document (organization_id, candidate_id, status);
CREATE UNIQUE INDEX candidate_document_org_hash_uidx
  ON recruiting.candidate_document (organization_id, content_hash)
  WHERE content_hash IS NOT NULL;

COMMENT ON TABLE recruiting.candidate_document IS
  'Task 16C: document metadata linking candidate to SourceItem/InformationPacket provenance. Does not store file content directly.';

CREATE TABLE recruiting.candidate_company_interaction (
  candidate_company_interaction_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  candidate_id uuid NOT NULL
    REFERENCES recruiting.candidate (candidate_id),
  company_id uuid NOT NULL
    REFERENCES recruiting.company (company_id),
  job_id uuid REFERENCES recruiting.job (job_id),
  interaction_type text NOT NULL CHECK (
    interaction_type IN (
      'submission',
      'prior_contact',
      'prior_application',
      'interview',
      'placement'
    )
  ),
  status text NOT NULL CHECK (status IN ('active', 'completed', 'cancelled')),
  started_at timestamptz NOT NULL,
  ended_at timestamptz,
  notes text CHECK (notes IS NULL OR btrim(notes) <> ''),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX cci_org_candidate_company_idx
  ON recruiting.candidate_company_interaction (organization_id, candidate_id, company_id);
CREATE INDEX cci_org_job_idx
  ON recruiting.candidate_company_interaction (organization_id, job_id)
  WHERE job_id IS NOT NULL;
CREATE INDEX cci_org_type_status_idx
  ON recruiting.candidate_company_interaction (organization_id, interaction_type, status);

COMMENT ON TABLE recruiting.candidate_company_interaction IS
  'Task 16C: records interactions between candidates and companies (submissions, prior contacts, interviews, placements).';

CREATE TABLE recruiting.shortlist (
  shortlist_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  job_id uuid NOT NULL
    REFERENCES recruiting.job (job_id),
  title text CHECK (title IS NULL OR btrim(title) <> ''),
  status text NOT NULL CHECK (
    status IN (
      'draft',
      'ready_for_review',
      'sent_to_client',
      'client_viewed',
      'client_feedback_pending',
      'candidate_selected',
      'contact_unlocked',
      'interviewing',
      'closed'
    )
  ),
  sent_at timestamptz,
  client_viewed_at timestamptz,
  owner_consultant_id uuid REFERENCES identity.user_account (user_account_id),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX shortlist_org_job_idx
  ON recruiting.shortlist (organization_id, job_id);
CREATE INDEX shortlist_org_status_idx
  ON recruiting.shortlist (organization_id, status);

COMMENT ON TABLE recruiting.shortlist IS
  'Task 16C: shortlist of candidates for a job. Client sees anonymous cards only; raw candidate identity not exposed through this entity.';

CREATE TABLE recruiting.shortlist_candidate_card (
  shortlist_candidate_card_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  shortlist_id uuid NOT NULL
    REFERENCES recruiting.shortlist (shortlist_id),
  anonymous_candidate_card_id uuid NOT NULL,
  candidate_id uuid NOT NULL
    REFERENCES recruiting.candidate (candidate_id),
  candidate_profile_id uuid NOT NULL
    REFERENCES recruiting.candidate_profile (candidate_profile_id),
  sort_order integer NOT NULL DEFAULT 0,
  status text NOT NULL CHECK (
    status IN (
      'draft',
      'included',
      'removed',
      'selected',
      'unlocked',
      'rejected'
    )
  ),
  match_report_id uuid,
  client_notes text CHECK (client_notes IS NULL OR btrim(client_notes) <> ''),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX shortlist_card_org_shortlist_idx
  ON recruiting.shortlist_candidate_card (organization_id, shortlist_id, sort_order);
CREATE UNIQUE INDEX shortlist_card_org_candidate_uidx
  ON recruiting.shortlist_candidate_card (organization_id, shortlist_id, candidate_id);
CREATE INDEX shortlist_card_org_status_idx
  ON recruiting.shortlist_candidate_card (organization_id, status);

COMMENT ON TABLE recruiting.shortlist_candidate_card IS
  'Task 16C: individual card on a shortlist. candidate_id and candidate_profile_id are internal-only columns — Client portal sees only anonymous_candidate_card_id. Service-level guards enforce this separation.';

-- === Subtask 16D: InterviewFeedback + Placement + Commission =================

CREATE TABLE recruiting.interview_feedback (
  interview_feedback_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  candidate_company_interaction_id uuid NOT NULL
    REFERENCES recruiting.candidate_company_interaction (candidate_company_interaction_id),
  job_id uuid NOT NULL
    REFERENCES recruiting.job (job_id),
  interviewer_name text CHECK (interviewer_name IS NULL OR btrim(interviewer_name) <> ''),
  interviewer_role text CHECK (interviewer_role IS NULL OR btrim(interviewer_role) <> ''),
  interview_round integer,
  interview_date timestamptz,
  outcome text NOT NULL CHECK (
    outcome IN ('strong_yes', 'yes', 'maybe', 'weak_no', 'strong_no')
  ),
  ratings jsonb NOT NULL DEFAULT '{}'::jsonb,
  strengths text,
  concerns text,
  notes text,
  submitted_by_role text NOT NULL CHECK (btrim(submitted_by_role) <> ''),
  submitted_by_user_id uuid REFERENCES identity.user_account (user_account_id),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX interview_feedback_org_interaction_idx
  ON recruiting.interview_feedback (organization_id, candidate_company_interaction_id);
CREATE INDEX interview_feedback_org_job_idx
  ON recruiting.interview_feedback (organization_id, job_id);
CREATE INDEX interview_feedback_org_outcome_idx
  ON recruiting.interview_feedback (organization_id, outcome);

COMMENT ON TABLE recruiting.interview_feedback IS
  'Task 16D: structured interview feedback with dimension ratings and outcome recommendation.';

CREATE TABLE recruiting.placement (
  placement_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  job_id uuid NOT NULL
    REFERENCES recruiting.job (job_id),
  candidate_id uuid NOT NULL
    REFERENCES recruiting.candidate (candidate_id),
  company_id uuid NOT NULL
    REFERENCES recruiting.company (company_id),
  status text NOT NULL CHECK (
    status IN (
      'offer_pending',
      'offer_accepted',
      'onboarded',
      'invoice_ready',
      'invoice_sent',
      'paid',
      'guarantee_active',
      'guarantee_completed',
      'replacement_required',
      'cancelled'
    )
  ),
  offer_details jsonb,
  offer_accepted_at timestamptz,
  start_date date,
  onboarded_at timestamptz,
  guarantee_days integer,
  guarantee_expires_at date,
  cancelled_at timestamptz,
  cancel_reason text CHECK (cancel_reason IS NULL OR btrim(cancel_reason) <> ''),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX placement_org_status_idx
  ON recruiting.placement (organization_id, status);
CREATE INDEX placement_org_job_idx
  ON recruiting.placement (organization_id, job_id);
CREATE INDEX placement_org_candidate_idx
  ON recruiting.placement (organization_id, candidate_id);
CREATE INDEX placement_org_company_idx
  ON recruiting.placement (organization_id, company_id);
CREATE UNIQUE INDEX placement_org_job_candidate_uidx
  ON recruiting.placement (organization_id, job_id, candidate_id)
  WHERE status <> 'cancelled';

COMMENT ON TABLE recruiting.placement IS
  'Task 16D: placement record linking job, candidate, and company with offer and guarantee tracking.';

CREATE TABLE recruiting.commission (
  commission_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  placement_id uuid NOT NULL
    REFERENCES recruiting.placement (placement_id),
  consultant_id uuid NOT NULL
    REFERENCES identity.user_account (user_account_id),
  status text NOT NULL CHECK (status IN ('pending', 'calculated', 'paid', 'withheld')),
  commission_type text NOT NULL CHECK (
    commission_type IN ('full_fee', 'split', 'referral', 'override')
  ),
  amount numeric(18, 2),
  currency text CHECK (currency IS NULL OR btrim(currency) <> ''),
  split_percentage numeric(5, 2),
  calculation_details jsonb,
  paid_at timestamptz,
  withheld_reason text CHECK (withheld_reason IS NULL OR btrim(withheld_reason) <> ''),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX commission_org_placement_idx
  ON recruiting.commission (organization_id, placement_id);
CREATE INDEX commission_org_consultant_idx
  ON recruiting.commission (organization_id, consultant_id, status);
CREATE INDEX commission_org_status_idx
  ON recruiting.commission (organization_id, status);

COMMENT ON TABLE recruiting.commission IS
  'Task 16D: commission baseline for placement fee tracking.';
