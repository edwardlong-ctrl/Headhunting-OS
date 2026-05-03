-- Task 32: Client Portal v1 unlock request persistence

CREATE TABLE privacy.client_unlock_request (
  client_unlock_request_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  shortlist_id uuid NOT NULL,
  shortlist_candidate_card_id uuid NOT NULL,
  job_id uuid NOT NULL,
  client_actor_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  anonymous_candidate_card_ref text NOT NULL,
  request_reason text NOT NULL,
  status text NOT NULL,
  unlock_decision_ref text,
  approved_disclosure_record_ref text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1,
  CONSTRAINT client_unlock_request_status_check CHECK (
    status IN ('requested', 'under_review', 'approved', 'rejected', 'cancelled')
  )
);

CREATE INDEX client_unlock_request_org_shortlist_card_idx
  ON privacy.client_unlock_request (organization_id, shortlist_id, shortlist_candidate_card_id, created_at DESC);

CREATE INDEX client_unlock_request_org_actor_idx
  ON privacy.client_unlock_request (organization_id, client_actor_id, created_at DESC);

CREATE INDEX client_unlock_request_org_status_idx
  ON privacy.client_unlock_request (organization_id, status, created_at DESC);
