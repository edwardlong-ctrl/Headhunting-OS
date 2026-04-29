CREATE TABLE privacy.consent_record (
  consent_record_ref text NOT NULL,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  candidate_ref text NOT NULL,
  candidate_profile_ref text NOT NULL,
  job_ref text NOT NULL,
  profile_version text NOT NULL,
  consent_text_version text NOT NULL,
  status text NOT NULL,
  permitted_disclosure_levels text[] NOT NULL DEFAULT ARRAY[]::text[],
  confirmed_at timestamptz NOT NULL,
  expires_at timestamptz,
  revoked boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (organization_id, consent_record_ref)
);

CREATE INDEX consent_record_org_candidate_job_idx
  ON privacy.consent_record (organization_id, candidate_ref, job_ref, confirmed_at);

CREATE TABLE privacy.unlock_decision (
  unlock_decision_ref text NOT NULL,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  candidate_ref text NOT NULL,
  candidate_profile_ref text NOT NULL,
  job_ref text NOT NULL,
  client_ref text NOT NULL,
  requested_disclosure_level text NOT NULL,
  status text NOT NULL,
  review_status text NOT NULL,
  risk_tier governance.risk_tier NOT NULL,
  approved_by_user_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  approved_by_role governance.actor_role NOT NULL,
  decided_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (organization_id, unlock_decision_ref)
);

CREATE INDEX unlock_decision_org_scope_idx
  ON privacy.unlock_decision (
    organization_id,
    candidate_ref,
    candidate_profile_ref,
    job_ref,
    client_ref,
    decided_at
  );

CREATE TABLE privacy.disclosure_record (
  disclosure_record_ref text NOT NULL,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  candidate_ref text NOT NULL,
  candidate_profile_ref text NOT NULL,
  job_ref text NOT NULL,
  client_ref text NOT NULL,
  status text NOT NULL,
  disclosure_level text NOT NULL,
  redaction_level text NOT NULL,
  unlock_decision_ref text NOT NULL,
  consent_record_ref text NOT NULL,
  workflow_event_id uuid REFERENCES workflow.workflow_event (workflow_event_id),
  decided_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (organization_id, disclosure_record_ref),
  FOREIGN KEY (organization_id, unlock_decision_ref)
    REFERENCES privacy.unlock_decision (organization_id, unlock_decision_ref),
  FOREIGN KEY (organization_id, consent_record_ref)
    REFERENCES privacy.consent_record (organization_id, consent_record_ref)
);

CREATE INDEX disclosure_record_org_scope_idx
  ON privacy.disclosure_record (
    organization_id,
    candidate_ref,
    candidate_profile_ref,
    job_ref,
    client_ref,
    decided_at
  );
