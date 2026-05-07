CREATE TABLE governance.config_entry (
  governance_config_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  config_type text NOT NULL,
  config_key text NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  enabled boolean NOT NULL DEFAULT true,
  created_by_user_id uuid REFERENCES identity.user_account (user_account_id),
  updated_by_user_id uuid REFERENCES identity.user_account (user_account_id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1 CHECK (version > 0),
  CONSTRAINT config_entry_type_not_blank CHECK (btrim(config_type) <> ''),
  CONSTRAINT config_entry_key_not_blank CHECK (btrim(config_key) <> ''),
  CONSTRAINT config_entry_org_type_key_uidx UNIQUE (organization_id, config_type, config_key)
);

CREATE INDEX governance_config_entry_org_type_idx
  ON governance.config_entry (organization_id, config_type, updated_at DESC);
