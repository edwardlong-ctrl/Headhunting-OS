ALTER TABLE recruiting.shortlist_candidate_card
  ADD CONSTRAINT uq_shortlist_candidate_card_id_org UNIQUE (shortlist_candidate_card_id, organization_id);

CREATE TABLE recruiting.match_report (
  match_report_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  job_id uuid NOT NULL,
  candidate_id uuid,
  shortlist_candidate_card_id uuid,
  subject_type text NOT NULL CHECK (
    subject_type IN ('candidate', 'shortlist_card')
  ),
  match_subject_ref text NOT NULL CHECK (btrim(match_subject_ref) <> ''),
  proposed_score integer NOT NULL CHECK (proposed_score BETWEEN 1 AND 5),
  overall_score integer NOT NULL CHECK (overall_score BETWEEN 1 AND 5),
  score_confidence text NOT NULL CHECK (
    score_confidence IN ('LOW', 'MEDIUM', 'HIGH')
  ),
  cap_applied boolean NOT NULL DEFAULT false,
  cap_reason text NOT NULL CHECK (btrim(cap_reason) <> ''),
  cap_safe_explanation text NOT NULL CHECK (btrim(cap_safe_explanation) <> ''),
  human_review_required boolean NOT NULL DEFAULT false,
  additional_evidence_required boolean NOT NULL DEFAULT false,
  client_delivery_blocked boolean NOT NULL DEFAULT false,
  authenticity_risk text NOT NULL CHECK (
    authenticity_risk IN ('LOW', 'MEDIUM', 'HIGH')
  ),
  reidentification_risk_signal text NOT NULL CHECK (
    reidentification_risk_signal IN ('LOW', 'MEDIUM', 'HIGH')
  ),
  ontology_version text NOT NULL CHECK (btrim(ontology_version) <> ''),
  industry_pack_version text NOT NULL CHECK (btrim(industry_pack_version) <> ''),
  dimension_scores jsonb NOT NULL DEFAULT '{}'::jsonb,
  evidence_coverage jsonb NOT NULL DEFAULT '{}'::jsonb,
  provenance_summary jsonb NOT NULL DEFAULT '{}'::jsonb,
  explanations jsonb NOT NULL DEFAULT '[]'::jsonb,
  interview_questions jsonb NOT NULL DEFAULT '[]'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  generated_at timestamptz NOT NULL,
  CONSTRAINT fk_match_report_job_org
    FOREIGN KEY (job_id, organization_id)
    REFERENCES recruiting.job (job_id, organization_id),
  CONSTRAINT fk_match_report_candidate_org
    FOREIGN KEY (candidate_id, organization_id)
    REFERENCES recruiting.candidate (candidate_id, organization_id),
  CONSTRAINT fk_match_report_shortlist_card_org
    FOREIGN KEY (shortlist_candidate_card_id, organization_id)
    REFERENCES recruiting.shortlist_candidate_card (shortlist_candidate_card_id, organization_id),
  CONSTRAINT ck_match_report_subject_ref_choice
    CHECK (
      (subject_type = 'candidate' AND candidate_id IS NOT NULL)
      OR (subject_type = 'shortlist_card' AND shortlist_candidate_card_id IS NOT NULL)
    )
);

CREATE INDEX match_report_org_job_created_idx
  ON recruiting.match_report (organization_id, job_id, created_at DESC);

CREATE INDEX match_report_org_candidate_created_idx
  ON recruiting.match_report (organization_id, candidate_id, created_at DESC);

CREATE INDEX match_report_org_shortlist_card_created_idx
  ON recruiting.match_report (organization_id, shortlist_candidate_card_id, created_at DESC);

COMMENT ON TABLE recruiting.match_report IS
  'Task 27: persisted consultant-internal match report. Stores evidence-backed, non-canonical matching output for consultant workflows only.';
