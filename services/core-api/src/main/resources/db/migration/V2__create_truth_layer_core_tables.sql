CREATE TYPE governance.verification_status AS ENUM (
  'ai_extracted',
  'human_acknowledged',
  'consultant_attested',
  'candidate_confirmed',
  'external_verified',
  'system_inference',
  'conflicting',
  'needs_confirmation',
  'rejected',
  'retracted'
);

CREATE TYPE governance.claim_type AS ENUM (
  'fact',
  'preference',
  'intent',
  'risk',
  'inference',
  'prediction'
);

CREATE TYPE governance.assertion_strength AS ENUM (
  'explicit',
  'implied',
  'weak_signal',
  'contradiction',
  'unknown'
);

CREATE TYPE governance.client_shareability AS ENUM (
  'internal_only',
  'client_safe',
  'consent_required',
  'forbidden'
);

CREATE TYPE governance.risk_tier AS ENUM (
  'T0_AUTOMATED_CLEANUP',
  'T1_LOW_RISK',
  'T2_MEDIUM_RISK',
  'T3_HIGH_RISK',
  'T4_TRANSACTION_LEGAL_BLOCKING'
);

CREATE TYPE governance.actor_role AS ENUM (
  'owner',
  'consultant',
  'client',
  'candidate',
  'admin',
  'system',
  'ai'
);

CREATE TABLE identity.organization (
  organization_id uuid PRIMARY KEY,
  legal_name text NOT NULL,
  display_name text NOT NULL,
  status text NOT NULL CHECK (status IN ('active', 'suspended', 'archived')),
  default_timezone text NOT NULL,
  billing_profile_ref text,
  data_region text,
  retention_policy_id uuid,
  risk_policy_id uuid,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_by uuid,
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE UNIQUE INDEX organization_active_legal_name_uidx
  ON identity.organization (lower(legal_name))
  WHERE status = 'active';
CREATE INDEX organization_status_idx ON identity.organization (status);
CREATE INDEX organization_created_at_idx ON identity.organization (created_at);

CREATE TABLE identity.user_account (
  user_account_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  email text NOT NULL,
  display_name text NOT NULL,
  status text NOT NULL CHECK (status IN ('invited', 'active', 'suspended', 'deactivated')),
  phone text,
  identity_provider_subject text,
  last_login_at timestamptz,
  mfa_enabled boolean NOT NULL DEFAULT false,
  locale text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_by uuid,
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE UNIQUE INDEX user_account_org_email_uidx
  ON identity.user_account (organization_id, lower(email));
CREATE INDEX user_account_org_status_idx ON identity.user_account (organization_id, status);
CREATE INDEX user_account_identity_provider_subject_idx
  ON identity.user_account (identity_provider_subject);

CREATE TABLE identity.role_assignment (
  role_assignment_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  user_account_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  role governance.actor_role NOT NULL,
  scope_type text NOT NULL,
  scope_id uuid,
  status text NOT NULL CHECK (status IN ('active', 'expired', 'revoked')),
  expires_at timestamptz,
  granted_by uuid REFERENCES identity.user_account (user_account_id),
  revoked_by uuid REFERENCES identity.user_account (user_account_id),
  reason text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0),
  CHECK (role::text IN ('owner', 'consultant', 'client', 'candidate', 'admin'))
);

CREATE INDEX role_assignment_user_status_idx
  ON identity.role_assignment (organization_id, user_account_id, status);
CREATE INDEX role_assignment_role_status_idx
  ON identity.role_assignment (organization_id, role, status);
CREATE UNIQUE INDEX role_assignment_active_scope_uidx
  ON identity.role_assignment (user_account_id, role, scope_type, scope_id)
  WHERE status = 'active';

CREATE TABLE recruiting.information_packet (
  information_packet_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  packet_type text NOT NULL,
  processing_status text NOT NULL CHECK (
    processing_status IN (
      'uploaded',
      'classifying',
      'extracting',
      'reviewing',
      'approved',
      'published',
      'rejected',
      'superseded'
    )
  ),
  source_item_ids uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],
  detected_conflicts jsonb NOT NULL DEFAULT '[]'::jsonb,
  stale_fields jsonb NOT NULL DEFAULT '[]'::jsonb,
  missing_fields jsonb NOT NULL DEFAULT '[]'::jsonb,
  suggested_followups jsonb NOT NULL DEFAULT '[]'::jsonb,
  published_entity_type text,
  published_entity_id uuid,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX information_packet_org_type_status_idx
  ON recruiting.information_packet (organization_id, packet_type, processing_status);
CREATE INDEX information_packet_published_entity_idx
  ON recruiting.information_packet (published_entity_type, published_entity_id);
CREATE INDEX information_packet_created_at_idx ON recruiting.information_packet (created_at);

CREATE TABLE recruiting.source_item (
  source_item_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  information_packet_id uuid REFERENCES recruiting.information_packet (information_packet_id),
  source_type text NOT NULL,
  origin_actor_type governance.actor_role NOT NULL,
  status text NOT NULL CHECK (
    status IN (
      'uploaded',
      'classifying',
      'classified',
      'parsing',
      'parsed',
      'failed',
      'quarantined',
      'archived'
    )
  ),
  received_at timestamptz NOT NULL,
  storage_ref text,
  content_hash text,
  language text,
  external_ref text,
  source_timestamp timestamptz,
  sensitivity_level text,
  parsed_text_ref text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX source_item_org_status_idx ON recruiting.source_item (organization_id, status);
CREATE UNIQUE INDEX source_item_org_content_hash_uidx
  ON recruiting.source_item (organization_id, content_hash)
  WHERE content_hash IS NOT NULL;
CREATE INDEX source_item_type_received_at_idx ON recruiting.source_item (source_type, received_at);

CREATE TABLE recruiting.candidate (
  candidate_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  status text NOT NULL CHECK (
    status IN (
      'new',
      'profile_parsed',
      'consultant_review',
      'available',
      'matched_to_job',
      'outreach',
      'interested',
      'consent_pending',
      'consent_confirmed',
      'shortlisted',
      'client_review',
      'identity_disclosed',
      'interviewing',
      'offer_pending',
      'placed',
      'rejected',
      'archived',
      'do_not_contact',
      'merged'
    )
  ),
  current_profile_id uuid,
  privacy_status text NOT NULL DEFAULT 'internal_only',
  owner_consultant_id uuid REFERENCES identity.user_account (user_account_id),
  do_not_contact_reason text,
  merged_into_candidate_id uuid REFERENCES recruiting.candidate (candidate_id),
  last_activity_at timestamptz,
  default_industry_pack_id uuid,
  identity_fingerprint_hash text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX candidate_org_status_idx ON recruiting.candidate (organization_id, status);
CREATE INDEX candidate_owner_consultant_idx
  ON recruiting.candidate (organization_id, owner_consultant_id);
CREATE INDEX candidate_current_profile_idx ON recruiting.candidate (organization_id, current_profile_id);
CREATE INDEX candidate_do_not_contact_idx
  ON recruiting.candidate (organization_id)
  WHERE status = 'do_not_contact';
CREATE INDEX candidate_identity_fingerprint_idx
  ON recruiting.candidate (organization_id, identity_fingerprint_hash)
  WHERE identity_fingerprint_hash IS NOT NULL;

CREATE TABLE recruiting.candidate_profile (
  candidate_profile_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  candidate_id uuid NOT NULL REFERENCES recruiting.candidate (candidate_id),
  profile_version integer NOT NULL CHECK (profile_version > 0),
  status text NOT NULL CHECK (status IN ('draft', 'in_review', 'canonical', 'superseded', 'locked')),
  field_status_map jsonb NOT NULL,
  headline text,
  location jsonb,
  skills jsonb NOT NULL DEFAULT '[]'::jsonb,
  experience_summary text,
  education jsonb NOT NULL DEFAULT '[]'::jsonb,
  compensation_expectation jsonb,
  availability jsonb,
  motivation text,
  source_claim_ids uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],
  superseded_by_profile_id uuid,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE UNIQUE INDEX candidate_profile_version_uidx
  ON recruiting.candidate_profile (candidate_id, profile_version);
CREATE INDEX candidate_profile_org_candidate_status_idx
  ON recruiting.candidate_profile (organization_id, candidate_id, status);
CREATE INDEX candidate_profile_skills_gin_idx ON recruiting.candidate_profile USING gin (skills);

ALTER TABLE recruiting.candidate
  ADD CONSTRAINT candidate_current_profile_fk
  FOREIGN KEY (current_profile_id)
  REFERENCES recruiting.candidate_profile (candidate_profile_id);

CREATE TABLE governance.ai_task_definition (
  ai_task_definition_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  task_key text NOT NULL,
  task_version text NOT NULL,
  status text NOT NULL CHECK (status IN ('draft', 'active', 'deprecated', 'retired')),
  input_schema_version text NOT NULL,
  output_schema_version text NOT NULL,
  human_review_policy jsonb NOT NULL,
  description text,
  model_routing_policy jsonb,
  write_back_target text,
  eval_suite_ref text,
  owner_user_id uuid REFERENCES identity.user_account (user_account_id),
  deprecated_at timestamptz,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE UNIQUE INDEX ai_task_definition_key_version_uidx
  ON governance.ai_task_definition (organization_id, task_key, task_version);
CREATE INDEX ai_task_definition_status_idx ON governance.ai_task_definition (status);
CREATE INDEX ai_task_definition_write_back_target_idx
  ON governance.ai_task_definition (write_back_target);

CREATE TABLE governance.ai_task_run (
  ai_task_run_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  ai_task_definition_id uuid NOT NULL REFERENCES governance.ai_task_definition (ai_task_definition_id),
  task_version text NOT NULL,
  status text NOT NULL CHECK (
    status IN (
      'queued',
      'running',
      'succeeded',
      'failed',
      'blocked_by_gate',
      'requires_review',
      'write_back_completed',
      'cancelled'
    )
  ),
  input_schema_version text NOT NULL,
  output_schema_version text NOT NULL,
  prompt_version text NOT NULL,
  model_provider text NOT NULL,
  model_name text NOT NULL,
  model_version text,
  tool_calls jsonb NOT NULL DEFAULT '[]'::jsonb,
  source_ref_ids uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],
  target_entity_type text,
  target_entity_id uuid,
  write_back_target text,
  human_review_status text NOT NULL,
  started_at timestamptz NOT NULL,
  completed_at timestamptz,
  error_code text,
  cost_units numeric(18, 6),
  trace_ref text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX ai_task_run_definition_status_idx
  ON governance.ai_task_run (organization_id, ai_task_definition_id, status);
CREATE INDEX ai_task_run_target_entity_idx
  ON governance.ai_task_run (target_entity_type, target_entity_id);
CREATE INDEX ai_task_run_started_at_idx ON governance.ai_task_run (started_at);
CREATE INDEX ai_task_run_error_code_idx ON governance.ai_task_run (error_code);

CREATE TABLE governance.claim_ledger_item (
  claim_ledger_item_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  entity_type text NOT NULL,
  entity_id uuid NOT NULL,
  claim_type governance.claim_type NOT NULL,
  assertion_strength governance.assertion_strength NOT NULL,
  source_span_ref text NOT NULL,
  speaker governance.actor_role NOT NULL,
  verification_status governance.verification_status NOT NULL,
  canonical_write_allowed boolean NOT NULL DEFAULT false,
  client_shareability governance.client_shareability NOT NULL DEFAULT 'internal_only',
  target_field_path text,
  source_item_id uuid REFERENCES recruiting.source_item (source_item_id),
  candidate_evidence_item_id uuid,
  confidence numeric(5, 4) CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
  contradicts_claim_id uuid REFERENCES governance.claim_ledger_item (claim_ledger_item_id),
  review_event_id uuid,
  ai_task_run_id uuid REFERENCES governance.ai_task_run (ai_task_run_id),
  expires_at timestamptz,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES identity.user_account (user_account_id),
  updated_by uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0)
);

CREATE INDEX claim_ledger_entity_idx
  ON governance.claim_ledger_item (organization_id, entity_type, entity_id);
CREATE INDEX claim_ledger_target_status_idx
  ON governance.claim_ledger_item (target_field_path, verification_status);
CREATE INDEX claim_ledger_source_item_idx ON governance.claim_ledger_item (source_item_id);
CREATE INDEX claim_ledger_client_shareability_idx
  ON governance.claim_ledger_item (client_shareability);

CREATE TABLE governance.review_event (
  review_event_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  reviewer_user_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  target_entity_type text NOT NULL,
  target_entity_id uuid NOT NULL,
  field_path text NOT NULL,
  risk_tier governance.risk_tier NOT NULL,
  decision text NOT NULL,
  bulk_flag boolean NOT NULL DEFAULT false,
  duration_ms integer NOT NULL CHECK (duration_ms >= 0),
  claim_ledger_item_id uuid REFERENCES governance.claim_ledger_item (claim_ledger_item_id),
  source_span_ref text,
  reason text,
  sample_audit_status text,
  review_velocity_bucket text,
  previous_value_hash text,
  after_value_hash text,
  status text NOT NULL DEFAULT 'completed' CHECK (
    status IN ('completed', 'escalated', 'sampled_for_audit', 'failed_audit', 'superseded_by_review')
  ),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX review_event_reviewer_created_at_idx
  ON governance.review_event (organization_id, reviewer_user_id, created_at);
CREATE INDEX review_event_risk_bulk_idx ON governance.review_event (risk_tier, bulk_flag);
CREATE INDEX review_event_target_idx
  ON governance.review_event (target_entity_type, target_entity_id);

ALTER TABLE governance.claim_ledger_item
  ADD CONSTRAINT claim_ledger_item_review_event_fk
  FOREIGN KEY (review_event_id)
  REFERENCES governance.review_event (review_event_id);

CREATE TABLE workflow.workflow_event (
  workflow_event_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  entity_namespace text NOT NULL CHECK (
    entity_namespace IN ('identity', 'recruiting', 'governance', 'workflow', 'audit')
  ),
  entity_type text NOT NULL,
  entity_id uuid NOT NULL,
  entity_version integer,
  action text NOT NULL,
  before_state jsonb NOT NULL,
  after_state jsonb NOT NULL,
  actor_user_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  actor_role governance.actor_role NOT NULL,
  source_type text NOT NULL,
  source_ref_id uuid,
  ai_task_run_id uuid REFERENCES governance.ai_task_run (ai_task_run_id),
  review_event_id uuid REFERENCES governance.review_event (review_event_id),
  reason text NOT NULL,
  idempotency_key text,
  correlation_id uuid,
  previous_event_id uuid REFERENCES workflow.workflow_event (workflow_event_id),
  occurred_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX workflow_event_entity_timeline_idx
  ON workflow.workflow_event (organization_id, entity_type, entity_id, occurred_at);
CREATE INDEX workflow_event_actor_timeline_idx
  ON workflow.workflow_event (actor_user_id, occurred_at);
CREATE UNIQUE INDEX workflow_event_org_idempotency_uidx
  ON workflow.workflow_event (organization_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

CREATE TABLE audit.audit_log (
  audit_log_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  actor_user_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  actor_role governance.actor_role NOT NULL,
  action text NOT NULL,
  target_entity_type text NOT NULL,
  target_entity_id uuid NOT NULL,
  result text NOT NULL,
  occurred_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  ip_hash text,
  user_agent_hash text,
  before_state_hash text,
  after_state_hash text,
  reason text,
  workflow_event_id uuid REFERENCES workflow.workflow_event (workflow_event_id),
  ai_task_run_id uuid REFERENCES governance.ai_task_run (ai_task_run_id),
  sensitivity_level text,
  status text NOT NULL DEFAULT 'recorded' CHECK (status IN ('recorded', 'correction_recorded')),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX audit_log_target_timeline_idx
  ON audit.audit_log (organization_id, target_entity_type, target_entity_id, occurred_at);
CREATE INDEX audit_log_actor_timeline_idx ON audit.audit_log (actor_user_id, occurred_at);
CREATE INDEX audit_log_action_result_idx ON audit.audit_log (action, result);

COMMENT ON TABLE recruiting.candidate IS
  'Raw Candidate must not be exposed to Client-facing contracts. Client-safe DTO generation is server-owned and implemented later. Consent/disclosure behavior is intentionally not implemented in Task 2B.';
COMMENT ON COLUMN recruiting.candidate_profile.field_status_map IS
  'Candidate canonical fields must have verification/status metadata.';
COMMENT ON COLUMN recruiting.candidate_profile.source_claim_ids IS
  'Canonical profile fields retain lineage to ClaimLedgerItem ids; AI output must enter ClaimLedgerItem first.';
COMMENT ON TABLE governance.claim_ledger_item IS
  'AI output must enter ClaimLedgerItem, not canonical Candidate fields directly.';
COMMENT ON TABLE workflow.workflow_event IS
  'WorkflowEvent is required for key state transitions and is append-only.';
COMMENT ON TABLE governance.ai_task_run IS
  'AITaskRun records AI-assisted actions without exposing provider secrets.';
COMMENT ON TABLE audit.audit_log IS
  'AuditLog is the immutable security and governance audit trail for sensitive reads and writes.';
