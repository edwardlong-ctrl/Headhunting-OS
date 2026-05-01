ALTER TABLE privacy.consent_record
  ADD COLUMN workflow_entity_id uuid;

ALTER TABLE privacy.disclosure_record
  ADD COLUMN workflow_entity_id uuid;

CREATE UNIQUE INDEX ux_consent_record_workflow_entity_id
  ON privacy.consent_record (organization_id, workflow_entity_id)
  WHERE workflow_entity_id IS NOT NULL;

CREATE UNIQUE INDEX ux_disclosure_record_workflow_entity_id
  ON privacy.disclosure_record (organization_id, workflow_entity_id)
  WHERE workflow_entity_id IS NOT NULL;
