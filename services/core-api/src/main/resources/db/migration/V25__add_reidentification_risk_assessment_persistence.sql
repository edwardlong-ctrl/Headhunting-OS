-- Task 30: Privacy Redaction and Re-identification v1
-- Adds persistence for the re-identification risk assessment that gates
-- every anonymous client-safe projection.
--
-- Spec source: docs/specs/v2.1/product-spec-v2.1.md (Section 13.4),
-- docs/roadmap/productization-roadmap.md (Task 30),
-- docs/security/client-safe-data-boundary.md.

CREATE TABLE privacy.reidentification_risk_assessment (
  reidentification_risk_assessment_ref text NOT NULL,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  candidate_card_id text NOT NULL,
  candidate_ref text,
  job_ref text,
  redaction_level text NOT NULL,
  risk_level text NOT NULL,
  decision text NOT NULL,
  unsafe_features text[] NOT NULL DEFAULT ARRAY[]::text[],
  risk_score double precision NOT NULL,
  explanation text NOT NULL,
  workflow_event_id uuid REFERENCES workflow.workflow_event (workflow_event_id),
  recorded_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (organization_id, reidentification_risk_assessment_ref),
  CONSTRAINT reidentification_risk_assessment_score_range CHECK (
    risk_score >= 0.0 AND risk_score <= 1.0
  )
);

CREATE INDEX reidentification_risk_assessment_org_card_idx
  ON privacy.reidentification_risk_assessment (
    organization_id,
    candidate_card_id,
    recorded_at
  );

CREATE INDEX reidentification_risk_assessment_org_decision_idx
  ON privacy.reidentification_risk_assessment (
    organization_id,
    decision,
    recorded_at
  );

-- Task 30 emits workflow_event rows with entity_namespace = 'privacy' to
-- audit re-identification assessments and client-safe redaction blocks.
-- Extend the namespace allowlist so the workflow event insertion succeeds.
ALTER TABLE workflow.workflow_event
  DROP CONSTRAINT IF EXISTS workflow_event_entity_namespace_check;

ALTER TABLE workflow.workflow_event
  ADD CONSTRAINT workflow_event_entity_namespace_check
  CHECK (entity_namespace IN (
    'identity',
    'recruiting',
    'governance',
    'workflow',
    'audit',
    'intake',
    'privacy'
  ));
