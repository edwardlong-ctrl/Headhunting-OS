ALTER TABLE intake.source_item
  ADD COLUMN IF NOT EXISTS mime_type VARCHAR(127),
  ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT,
  ADD COLUMN IF NOT EXISTS original_filename VARCHAR(512),
  ADD COLUMN IF NOT EXISTS scan_status VARCHAR(50) NOT NULL DEFAULT 'not_scanned';

COMMENT ON COLUMN intake.source_item.mime_type IS 'Task 20 Document Storage v1 — MIME type of the uploaded file.';
COMMENT ON COLUMN intake.source_item.file_size_bytes IS 'Task 20 Document Storage v1 — Size of the stored file in bytes.';
COMMENT ON COLUMN intake.source_item.original_filename IS 'Task 20 Document Storage v1 — Sanitized original filename for download Content-Disposition.';
COMMENT ON COLUMN intake.source_item.scan_status IS 'Task 20 Document Storage v1 — Virus scan status: not_scanned (default v1), clean, infected, error.';

CREATE UNIQUE INDEX IF NOT EXISTS uq_source_item_org_content_hash
  ON intake.source_item (organization_id, content_hash)
  WHERE content_hash IS NOT NULL;
