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
    'intake'
  ));
