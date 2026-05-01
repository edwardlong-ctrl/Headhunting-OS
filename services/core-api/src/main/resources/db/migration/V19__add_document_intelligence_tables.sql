ALTER TABLE intake.extraction_run
  DROP CONSTRAINT IF EXISTS intake_extraction_run_status_output_ck,
  DROP CONSTRAINT IF EXISTS intake_extraction_run_mode_check,
  DROP CONSTRAINT IF EXISTS extraction_run_mode_check;

ALTER TABLE intake.extraction_run
  ADD CONSTRAINT intake_extraction_run_mode_check
    CHECK (mode IN ('DETERMINISTIC_PLACEHOLDER', 'DOCUMENT_INTELLIGENCE_V1')),
  ADD CONSTRAINT intake_extraction_run_status_output_ck
    CHECK (
      (status = 'SUCCEEDED' AND output_json IS NOT NULL AND failure_reason IS NULL)
      OR (status = 'FAILED' AND output_json IS NULL AND failure_reason IS NOT NULL)
      OR status = 'CREATED'
    );

CREATE TABLE intake.parsed_document (
  parsed_document_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  source_item_id uuid NOT NULL,
  processing_status text NOT NULL CHECK (
    processing_status IN (
      'PENDING_EXTERNAL_PROCESSING',
      'SUCCEEDED',
      'FAILED',
      'UNSUPPORTED_FOR_V1'
    )
  ),
  parser_name text NOT NULL CHECK (btrim(parser_name) <> ''),
  parser_version text NOT NULL CHECK (btrim(parser_version) <> ''),
  media_type text NOT NULL CHECK (btrim(media_type) <> ''),
  content_hash text,
  language text,
  ocr_required boolean NOT NULL DEFAULT false,
  failure_reason text,
  created_at timestamptz NOT NULL,
  completed_at timestamptz,
  UNIQUE (parsed_document_id, organization_id),
  CONSTRAINT parsed_document_source_item_fk
    FOREIGN KEY (source_item_id, organization_id)
    REFERENCES intake.source_item (source_item_id, organization_id),
  CONSTRAINT parsed_document_status_completion_ck
    CHECK (
      (processing_status = 'SUCCEEDED' AND completed_at IS NOT NULL AND failure_reason IS NULL)
      OR (processing_status = 'FAILED' AND completed_at IS NOT NULL AND failure_reason IS NOT NULL)
      OR (processing_status = 'PENDING_EXTERNAL_PROCESSING' AND completed_at IS NULL)
      OR (processing_status = 'UNSUPPORTED_FOR_V1' AND completed_at IS NOT NULL)
    )
);

CREATE INDEX intake_parsed_document_org_source_created_idx
  ON intake.parsed_document (organization_id, source_item_id, created_at DESC, parsed_document_id DESC);

CREATE TABLE intake.parsed_document_chunk (
  parsed_document_chunk_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  parsed_document_id uuid NOT NULL,
  chunk_index integer NOT NULL CHECK (chunk_index >= 0),
  page_number integer,
  start_offset integer NOT NULL CHECK (start_offset >= 0),
  end_offset integer NOT NULL CHECK (end_offset > start_offset),
  chunk_text text NOT NULL CHECK (btrim(chunk_text) <> ''),
  created_at timestamptz NOT NULL,
  UNIQUE (parsed_document_id, organization_id, chunk_index),
  UNIQUE (parsed_document_chunk_id, organization_id),
  CONSTRAINT parsed_document_chunk_document_fk
    FOREIGN KEY (parsed_document_id, organization_id)
    REFERENCES intake.parsed_document (parsed_document_id, organization_id)
    ON DELETE CASCADE
);

CREATE INDEX intake_parsed_document_chunk_org_document_idx
  ON intake.parsed_document_chunk (organization_id, parsed_document_id, chunk_index);

CREATE TABLE intake.parsed_document_span (
  parsed_document_span_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  parsed_document_id uuid NOT NULL,
  parsed_document_chunk_id uuid NOT NULL,
  span_index integer NOT NULL CHECK (span_index >= 0),
  page_number integer,
  start_offset integer NOT NULL CHECK (start_offset >= 0),
  end_offset integer NOT NULL CHECK (end_offset > start_offset),
  created_at timestamptz NOT NULL,
  UNIQUE (parsed_document_span_id, organization_id),
  UNIQUE (parsed_document_chunk_id, organization_id, span_index),
  CONSTRAINT parsed_document_span_document_fk
    FOREIGN KEY (parsed_document_id, organization_id)
    REFERENCES intake.parsed_document (parsed_document_id, organization_id)
    ON DELETE CASCADE,
  CONSTRAINT parsed_document_span_chunk_fk
    FOREIGN KEY (parsed_document_chunk_id, organization_id)
    REFERENCES intake.parsed_document_chunk (parsed_document_chunk_id, organization_id)
    ON DELETE CASCADE
);

CREATE INDEX intake_parsed_document_span_org_document_idx
  ON intake.parsed_document_span (organization_id, parsed_document_id, parsed_document_chunk_id, span_index);

COMMENT ON TABLE intake.parsed_document IS
  'Task 22 parsed document audit record. Parsed documents are evidence artifacts only and are not canonical fact storage.';
COMMENT ON TABLE intake.parsed_document_chunk IS
  'Task 22 parsed document chunks for evidence retrieval. Chunks are not canonical facts and are not client-safe by default.';
COMMENT ON TABLE intake.parsed_document_span IS
  'Task 22 source span mapping for parsed chunks. Spans are evidence locators only and are not canonical facts.';
