CREATE SCHEMA IF NOT EXISTS intake;

CREATE TABLE intake.source_item (
  source_item_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  source_type text NOT NULL CHECK (
    source_type IN (
      'CV',
      'LINKEDIN_TEXT',
      'WECHAT_NOTE',
      'CALL_NOTE',
      'EMAIL',
      'INTERVIEW_FEEDBACK',
      'JD',
      'COMPANY_MATERIAL',
      'OLD_SYSTEM_EXPORT',
      'OTHER'
    )
  ),
  origin text NOT NULL CHECK (
    origin IN (
      'CONSULTANT_UPLOAD',
      'CANDIDATE_UPLOAD',
      'CLIENT_UPLOAD',
      'EMAIL_IMPORT',
      'SYSTEM_IMPORT',
      'ADMIN_IMPORT',
      'OTHER'
    )
  ),
  title text CHECK (title IS NULL OR btrim(title) <> ''),
  content_hash text CHECK (content_hash IS NULL OR btrim(content_hash) <> ''),
  external_ref text CHECK (external_ref IS NULL OR btrim(external_ref) <> ''),
  storage_ref text CHECK (storage_ref IS NULL OR btrim(storage_ref) <> ''),
  raw_ref text CHECK (raw_ref IS NULL OR btrim(raw_ref) <> ''),
  language text CHECK (language IS NULL OR btrim(language) <> ''),
  uploaded_by_actor_type governance.actor_role NOT NULL,
  uploaded_by_actor_id uuid REFERENCES identity.user_account (user_account_id),
  received_at timestamptz NOT NULL,
  metadata_json jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (length(metadata_json::text) <= 8192),
  status text NOT NULL CHECK (
    status IN (
      'RECEIVED',
      'REGISTERED',
      'ATTACHED_TO_PACKET',
      'SUPERSEDED',
      'REJECTED'
    )
  ),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (source_item_id, organization_id)
);

CREATE UNIQUE INDEX intake_source_item_org_content_hash_uidx
  ON intake.source_item (organization_id, content_hash)
  WHERE content_hash IS NOT NULL;
CREATE INDEX intake_source_item_org_status_idx
  ON intake.source_item (organization_id, status);
CREATE INDEX intake_source_item_org_type_received_idx
  ON intake.source_item (organization_id, source_type, received_at);
CREATE INDEX intake_source_item_uploaded_actor_idx
  ON intake.source_item (organization_id, uploaded_by_actor_type, uploaded_by_actor_id);

CREATE TABLE intake.information_packet (
  information_packet_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  packet_type text NOT NULL CHECK (
    packet_type IN (
      'CANDIDATE',
      'COMPANY',
      'JOB',
      'CALL_NOTE',
      'FEEDBACK',
      'MIXED'
    )
  ),
  intended_entity_type text NOT NULL CHECK (
    intended_entity_type IN (
      'CANDIDATE',
      'COMPANY',
      'JOB',
      'INTERVIEW',
      'SHORTLIST',
      'UNKNOWN'
    )
  ),
  intended_entity_id uuid,
  created_by_actor_type governance.actor_role NOT NULL,
  created_by_actor_id uuid REFERENCES identity.user_account (user_account_id),
  processing_status text NOT NULL CHECK (
    processing_status IN (
      'CREATED',
      'SOURCES_ATTACHED',
      'READY_FOR_EXTRACTION',
      'EXTRACTION_PENDING',
      'EXTRACTION_COMPLETE',
      'REVIEW_PENDING',
      'APPROVED',
      'PUBLISHED',
      'REJECTED'
    )
  ),
  notes text CHECK (notes IS NULL OR btrim(notes) <> ''),
  metadata_json jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (length(metadata_json::text) <= 8192),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (information_packet_id, organization_id)
);

CREATE INDEX intake_information_packet_org_type_status_idx
  ON intake.information_packet (organization_id, packet_type, processing_status);
CREATE INDEX intake_information_packet_intended_entity_idx
  ON intake.information_packet (organization_id, intended_entity_type, intended_entity_id);
CREATE INDEX intake_information_packet_created_at_idx
  ON intake.information_packet (organization_id, created_at);

CREATE TABLE intake.information_packet_source_item (
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  information_packet_id uuid NOT NULL,
  source_item_id uuid NOT NULL,
  attached_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (organization_id, information_packet_id, source_item_id),
  CONSTRAINT intake_packet_source_item_packet_fk
    FOREIGN KEY (information_packet_id, organization_id)
    REFERENCES intake.information_packet (information_packet_id, organization_id),
  CONSTRAINT intake_packet_source_item_source_fk
    FOREIGN KEY (source_item_id, organization_id)
    REFERENCES intake.source_item (source_item_id, organization_id)
);

CREATE INDEX intake_packet_source_item_source_idx
  ON intake.information_packet_source_item (organization_id, source_item_id);
CREATE INDEX intake_packet_source_item_packet_idx
  ON intake.information_packet_source_item (organization_id, information_packet_id, attached_at);

COMMENT ON TABLE intake.source_item IS
  'Task 5A governed-intake provenance record. SourceItem records raw/source metadata and refs only; it is not canonical fact storage and is not client-facing raw Candidate exposure.';
COMMENT ON TABLE intake.information_packet IS
  'Task 5A governed-intake grouping record. InformationPacket groups SourceItem records and processing intent; it does not imply extraction, review, consent, disclosure, or canonical write success.';
COMMENT ON TABLE intake.information_packet_source_item IS
  'Task 5A link table for packet-to-source grouping with organization-scoped foreign keys and duplicate attach protection.';
