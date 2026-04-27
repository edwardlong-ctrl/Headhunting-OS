CREATE TABLE intake.extraction_run (
  extraction_run_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  information_packet_id uuid NOT NULL,
  mode text NOT NULL CHECK (
    mode IN (
      'DETERMINISTIC_PLACEHOLDER'
    )
  ),
  status text NOT NULL CHECK (
    status IN (
      'CREATED',
      'SUCCEEDED',
      'FAILED'
    )
  ),
  input_schema_version text NOT NULL CHECK (btrim(input_schema_version) <> ''),
  output_schema_version text NOT NULL CHECK (btrim(output_schema_version) <> ''),
  extractor_version text NOT NULL CHECK (btrim(extractor_version) <> ''),
  source_snapshot_hash text NOT NULL CHECK (btrim(source_snapshot_hash) <> ''),
  output_json jsonb,
  failure_reason text CHECK (failure_reason IS NULL OR btrim(failure_reason) <> ''),
  created_at timestamptz NOT NULL,
  completed_at timestamptz,
  UNIQUE (extraction_run_id, organization_id),
  CONSTRAINT intake_extraction_run_packet_fk
    FOREIGN KEY (information_packet_id, organization_id)
    REFERENCES intake.information_packet (information_packet_id, organization_id),
  CONSTRAINT intake_extraction_run_status_output_ck
    CHECK (
      (status = 'SUCCEEDED' AND output_json IS NOT NULL AND failure_reason IS NULL)
      OR (status = 'FAILED' AND output_json IS NULL AND failure_reason IS NOT NULL)
      OR status = 'CREATED'
    ),
  CONSTRAINT intake_extraction_run_output_size_ck
    CHECK (output_json IS NULL OR length(output_json::text) <= 65535)
);

CREATE INDEX intake_extraction_run_org_packet_created_idx
  ON intake.extraction_run (organization_id, information_packet_id, created_at);
CREATE INDEX intake_extraction_run_org_status_idx
  ON intake.extraction_run (organization_id, status, created_at);

COMMENT ON TABLE intake.extraction_run IS
  'Task 5B governed-intake deterministic extraction run/output envelope. Output is an intermediate JSONB envelope only; it is not canonical fact storage, ClaimLedger, ReviewEvent, CanonicalWrite, or client-facing candidate/profile data.';
COMMENT ON COLUMN intake.extraction_run.output_json IS
  'Intermediate extraction output envelope. It must not contain raw Candidate/Profile payloads and must not be treated as canonical fact, ClaimLedger, ReviewEvent, or CanonicalWrite output.';
