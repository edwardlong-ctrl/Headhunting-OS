ALTER TABLE workflow.workflow_event
  ADD COLUMN causation_id uuid;

UPDATE workflow.workflow_event
SET causation_id = previous_event_id
WHERE previous_event_id IS NOT NULL
  AND causation_id IS NULL;

ALTER TABLE workflow.workflow_event
  ADD CONSTRAINT workflow_event_idempotency_key_length_chk
  CHECK (idempotency_key IS NULL OR char_length(idempotency_key) <= 200);

CREATE INDEX workflow_event_org_correlation_idx
  ON workflow.workflow_event (organization_id, correlation_id)
  WHERE correlation_id IS NOT NULL;

CREATE INDEX workflow_event_org_causation_idx
  ON workflow.workflow_event (organization_id, causation_id)
  WHERE causation_id IS NOT NULL;

COMMENT ON COLUMN workflow.workflow_event.idempotency_key IS
  'Optional operation key scoped by organization_id; duplicate equivalent appends return the existing event and different payloads are idempotency conflicts.';
COMMENT ON COLUMN workflow.workflow_event.correlation_id IS
  'Groups WorkflowEvent rows that belong to the same business operation.';
COMMENT ON COLUMN workflow.workflow_event.causation_id IS
  'Links this audit event to the prior event, request, or service boundary that caused it; nullable for root events and intentionally not a target-entity lookup.';
COMMENT ON COLUMN workflow.workflow_event.previous_event_id IS
  'Legacy event-only lineage column retained from V2; Task 4B service code writes generic causation_id instead.';
