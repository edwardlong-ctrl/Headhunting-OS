-- V11: Canonical Write Attempt Ledger (Task 17)
-- Records every canonical write attempt — allowed, blocked, and require_review.
-- Serves as both audit trail for allowed writes and blocked-attempt ledger.

CREATE TABLE governance.canonical_write_attempt (
  canonical_write_attempt_id uuid PRIMARY KEY,
  organization_id            uuid NOT NULL
    REFERENCES identity.organization (organization_id),
  entity_type                text NOT NULL CHECK (btrim(entity_type) <> ''),
  entity_id                  uuid NOT NULL,
  entity_version             integer,
  target_field_path          text NOT NULL CHECK (btrim(target_field_path) <> ''),
  proposed_value_ref         text NOT NULL CHECK (btrim(proposed_value_ref) <> ''),
  source_span_ref            text CHECK (source_span_ref IS NULL OR btrim(source_span_ref) <> ''),
  claim_ledger_item_id       uuid,
  review_event_id            uuid,
  decision                   text NOT NULL CHECK (decision IN ('allow', 'block', 'require_review')),
  reason_codes               text[] NOT NULL DEFAULT ARRAY[]::text[],
  actor_user_id              uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  actor_role                 governance.actor_role NOT NULL,
  ai_task_run_id             uuid REFERENCES governance.ai_task_run (ai_task_run_id),
  idempotency_key            text CHECK (char_length(idempotency_key) <= 200),
  correlation_id             uuid,
  causation_id               uuid,
  workflow_event_id          uuid,
  occurred_at                timestamptz NOT NULL,
  created_at                 timestamptz NOT NULL DEFAULT now(),
  metadata                   jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX cwa_org_decision_occurred_idx ON governance.canonical_write_attempt
  (organization_id, decision, occurred_at DESC);
CREATE INDEX cwa_org_entity_idx ON governance.canonical_write_attempt
  (organization_id, entity_type, entity_id);
CREATE INDEX cwa_org_claim_idx ON governance.canonical_write_attempt
  (organization_id, claim_ledger_item_id)
  WHERE claim_ledger_item_id IS NOT NULL;
CREATE INDEX cwa_org_review_idx ON governance.canonical_write_attempt
  (organization_id, review_event_id)
  WHERE review_event_id IS NOT NULL;
CREATE INDEX cwa_org_workflow_event_idx ON governance.canonical_write_attempt
  (organization_id, workflow_event_id)
  WHERE workflow_event_id IS NOT NULL;
CREATE INDEX cwa_org_actor_idx ON governance.canonical_write_attempt
  (organization_id, actor_user_id);
CREATE UNIQUE INDEX cwa_org_idempotency_uidx ON governance.canonical_write_attempt
  (organization_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

COMMENT ON TABLE governance.canonical_write_attempt IS
  'Task 17: immutable ledger of every canonical write attempt — allow, block, require_review.';
