ALTER TABLE privacy.client_unlock_request
  ADD COLUMN workflow_entity_id uuid;

UPDATE privacy.client_unlock_request
SET workflow_entity_id = client_unlock_request_id
WHERE workflow_entity_id IS NULL;

ALTER TABLE privacy.client_unlock_request
  ALTER COLUMN workflow_entity_id SET NOT NULL;

CREATE INDEX client_unlock_request_org_workflow_entity_idx
  ON privacy.client_unlock_request (organization_id, workflow_entity_id, created_at DESC);
