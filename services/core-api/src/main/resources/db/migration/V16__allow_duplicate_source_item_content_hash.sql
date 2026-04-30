DROP INDEX IF EXISTS intake.intake_source_item_org_content_hash_uidx;
DROP INDEX IF EXISTS intake.uq_source_item_org_content_hash;

CREATE INDEX IF NOT EXISTS intake_source_item_org_content_hash_idx
  ON intake.source_item (organization_id, content_hash)
  WHERE content_hash IS NOT NULL;
