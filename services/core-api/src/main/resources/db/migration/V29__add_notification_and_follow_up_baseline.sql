-- Task 34 combined slice: candidate portal hardening plus notification/follow-up baseline.

CREATE SCHEMA IF NOT EXISTS operations;

CREATE TABLE operations.notification (
  notification_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  recipient_user_account_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  recipient_portal_role governance.actor_role NOT NULL,
  notification_type text NOT NULL CHECK (btrim(notification_type) <> ''),
  status text NOT NULL CHECK (status IN ('pending', 'delivered', 'read', 'dismissed')),
  title text NOT NULL CHECK (btrim(title) <> ''),
  body_summary text NOT NULL CHECK (btrim(body_summary) <> ''),
  deep_link text,
  entity_type text,
  entity_id uuid,
  source_ref text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  read_at timestamptz,
  dismissed_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1
);

CREATE INDEX notification_recipient_idx
  ON operations.notification (organization_id, recipient_user_account_id, status, created_at DESC);

CREATE INDEX notification_entity_idx
  ON operations.notification (organization_id, entity_type, entity_id, created_at DESC);

CREATE TABLE operations.notification_delivery_attempt (
  notification_delivery_attempt_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  notification_id uuid NOT NULL REFERENCES operations.notification (notification_id) ON DELETE CASCADE,
  channel text NOT NULL CHECK (channel IN ('in_app', 'email', 'sms')),
  provider_key text NOT NULL CHECK (btrim(provider_key) <> ''),
  status text NOT NULL CHECK (status IN ('delivered', 'skipped', 'failed')),
  safe_error_code text,
  attempted_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX notification_delivery_attempt_notification_idx
  ON operations.notification_delivery_attempt (organization_id, notification_id, attempted_at DESC);

CREATE TABLE operations.notification_preference (
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  user_account_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  portal_role governance.actor_role NOT NULL,
  in_app_enabled boolean NOT NULL DEFAULT true,
  email_enabled boolean NOT NULL DEFAULT false,
  sms_enabled boolean NOT NULL DEFAULT false,
  reminder_enabled boolean NOT NULL DEFAULT true,
  unsubscribed boolean NOT NULL DEFAULT false,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by_user_id uuid REFERENCES identity.user_account (user_account_id),
  version integer NOT NULL DEFAULT 1,
  PRIMARY KEY (organization_id, user_account_id, portal_role)
);

CREATE TABLE operations.notification_schedule (
  notification_schedule_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  user_account_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  portal_role governance.actor_role NOT NULL,
  notification_type text NOT NULL CHECK (btrim(notification_type) <> ''),
  entity_type text NOT NULL CHECK (btrim(entity_type) <> ''),
  entity_id uuid NOT NULL,
  due_at timestamptz NOT NULL,
  status text NOT NULL CHECK (status IN ('scheduled', 'processed', 'cancelled')),
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  last_attempted_at timestamptz,
  processed_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1
);

CREATE INDEX notification_schedule_due_idx
  ON operations.notification_schedule (organization_id, status, due_at);

CREATE INDEX notification_schedule_user_idx
  ON operations.notification_schedule (organization_id, user_account_id, status, due_at);

CREATE TABLE recruiting.follow_up_submission (
  follow_up_submission_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  candidate_id uuid NOT NULL,
  candidate_profile_id uuid NOT NULL,
  form_id text NOT NULL CHECK (btrim(form_id) <> ''),
  field_path text NOT NULL CHECK (btrim(field_path) <> ''),
  answer_json text NOT NULL CHECK (btrim(answer_json) <> ''),
  status text NOT NULL CHECK (status IN ('submitted', 'under_review', 'resolved')),
  submitted_by_user_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  reviewed_by_user_id uuid REFERENCES identity.user_account (user_account_id),
  workflow_event_id uuid REFERENCES workflow.workflow_event (workflow_event_id),
  submitted_at timestamptz NOT NULL,
  reviewed_at timestamptz,
  notes text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version integer NOT NULL DEFAULT 1,
  CONSTRAINT follow_up_submission_candidate_org_fk
    FOREIGN KEY (candidate_id, organization_id)
    REFERENCES recruiting.candidate (candidate_id, organization_id),
  CONSTRAINT follow_up_submission_profile_org_fk
    FOREIGN KEY (candidate_profile_id, organization_id)
    REFERENCES recruiting.candidate_profile (candidate_profile_id, organization_id)
);

CREATE INDEX follow_up_submission_candidate_idx
  ON recruiting.follow_up_submission (organization_id, candidate_id, submitted_at DESC);

CREATE INDEX follow_up_submission_profile_idx
  ON recruiting.follow_up_submission (organization_id, candidate_profile_id, status, submitted_at DESC);

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
    'intake',
    'privacy',
    'operations'
  ));
