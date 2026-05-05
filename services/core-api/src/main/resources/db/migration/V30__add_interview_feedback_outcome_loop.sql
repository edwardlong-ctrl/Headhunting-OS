ALTER TABLE recruiting.interview_feedback
  ADD COLUMN decision text,
  ADD COLUMN reject_reason_taxonomy text,
  ADD COLUMN ratings_schema_version text,
  ADD COLUMN ai_task_run_id uuid,
  ADD COLUMN reviewed_at timestamptz,
  ADD COLUMN reviewed_by_user_id uuid REFERENCES identity.user_account (user_account_id);

ALTER TABLE recruiting.interview_feedback
  ADD CONSTRAINT chk_interview_feedback_decision
    CHECK (decision IS NULL OR decision IN ('proceed', 'hold', 'reject'));

ALTER TABLE recruiting.interview_feedback
  ADD CONSTRAINT chk_interview_feedback_reject_reason
    CHECK (
      reject_reason_taxonomy IS NULL
      OR reject_reason_taxonomy IN (
        'compensation_mismatch',
        'skill_gap',
        'experience_gap',
        'culture_mismatch',
        'communication_concern',
        'availability_constraint',
        'location_mismatch',
        'other'
      )
    );

CREATE INDEX interview_feedback_org_decision_idx
  ON recruiting.interview_feedback (organization_id, decision)
  WHERE decision IS NOT NULL;

CREATE TABLE recruiting.interview_feedback_suggestion (
  interview_feedback_suggestion_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  interview_feedback_id uuid NOT NULL,
  candidate_company_interaction_id uuid NOT NULL,
  job_id uuid NOT NULL,
  candidate_id uuid,
  ai_task_run_id uuid,
  scope text NOT NULL CHECK (
    scope IN ('interaction', 'candidate_profile', 'company_preference', 'job_outcome')
  ),
  suggestion_type text NOT NULL CHECK (
    suggestion_type IN ('outcome_label', 'profile_update', 'company_preference_update')
  ),
  status text NOT NULL CHECK (
    status IN ('pending_review', 'approved', 'rejected', 'deferred')
  ),
  outcome_label text,
  reject_reason_taxonomy text,
  title text NOT NULL CHECK (btrim(title) <> ''),
  rationale text,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  reviewed_by_user_id uuid REFERENCES identity.user_account (user_account_id),
  reviewed_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT fk_interview_feedback_suggestion_feedback
    FOREIGN KEY (interview_feedback_id)
    REFERENCES recruiting.interview_feedback (interview_feedback_id),
  CONSTRAINT fk_interview_feedback_suggestion_interaction_org
    FOREIGN KEY (candidate_company_interaction_id, organization_id)
    REFERENCES recruiting.candidate_company_interaction (candidate_company_interaction_id, organization_id),
  CONSTRAINT fk_interview_feedback_suggestion_job_org
    FOREIGN KEY (job_id, organization_id)
    REFERENCES recruiting.job (job_id, organization_id),
  CONSTRAINT chk_interview_feedback_suggestion_reject_reason
    CHECK (
      reject_reason_taxonomy IS NULL
      OR reject_reason_taxonomy IN (
        'compensation_mismatch',
        'skill_gap',
        'experience_gap',
        'culture_mismatch',
        'communication_concern',
        'availability_constraint',
        'location_mismatch',
        'other'
      )
    )
);

CREATE INDEX interview_feedback_suggestion_org_feedback_idx
  ON recruiting.interview_feedback_suggestion (organization_id, interview_feedback_id);
CREATE INDEX interview_feedback_suggestion_org_status_idx
  ON recruiting.interview_feedback_suggestion (organization_id, status, created_at DESC);
CREATE INDEX interview_feedback_suggestion_org_interaction_idx
  ON recruiting.interview_feedback_suggestion (organization_id, candidate_company_interaction_id, created_at DESC);

CREATE TABLE recruiting.match_calibration_signal (
  match_calibration_signal_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  interview_feedback_id uuid NOT NULL,
  candidate_company_interaction_id uuid NOT NULL,
  job_id uuid NOT NULL,
  candidate_id uuid,
  industry_pack_key text,
  decision text,
  outcome_label text,
  reject_reason_taxonomy text,
  confidence text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT fk_match_calibration_feedback
    FOREIGN KEY (interview_feedback_id)
    REFERENCES recruiting.interview_feedback (interview_feedback_id),
  CONSTRAINT fk_match_calibration_interaction_org
    FOREIGN KEY (candidate_company_interaction_id, organization_id)
    REFERENCES recruiting.candidate_company_interaction (candidate_company_interaction_id, organization_id),
  CONSTRAINT fk_match_calibration_job_org
    FOREIGN KEY (job_id, organization_id)
    REFERENCES recruiting.job (job_id, organization_id),
  CONSTRAINT chk_match_calibration_decision
    CHECK (decision IS NULL OR decision IN ('proceed', 'hold', 'reject')),
  CONSTRAINT chk_match_calibration_reject_reason
    CHECK (
      reject_reason_taxonomy IS NULL
      OR reject_reason_taxonomy IN (
        'compensation_mismatch',
        'skill_gap',
        'experience_gap',
        'culture_mismatch',
        'communication_concern',
        'availability_constraint',
        'location_mismatch',
        'other'
      )
    )
);

CREATE INDEX match_calibration_signal_org_job_idx
  ON recruiting.match_calibration_signal (organization_id, job_id, created_at DESC);
CREATE INDEX match_calibration_signal_org_feedback_idx
  ON recruiting.match_calibration_signal (organization_id, interview_feedback_id);
