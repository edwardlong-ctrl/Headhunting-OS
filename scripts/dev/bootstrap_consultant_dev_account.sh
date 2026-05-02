#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

ORG_ID="${ORG_ID:-00000000-0000-0000-0000-000000240001}"
USER_ID="${USER_ID:-00000000-0000-0000-0000-000000240002}"
EMAIL="${EMAIL:-consultant@example.com}"
PASSWORD="${PASSWORD:-secret123}"
DISPLAY_NAME="${DISPLAY_NAME:-Consultant Dev}"
LEGAL_NAME="${LEGAL_NAME:-Local Consultant QA Org}"
PORTAL_ROLE="${PORTAL_ROLE:-consultant}"

docker compose -f "$ROOT_DIR/docker-compose.yml" exec -T postgres \
  psql -U recruiting_os -d recruiting_os \
  -v org_id="$ORG_ID" \
  -v user_id="$USER_ID" \
  -v email="$EMAIL" \
  -v password="$PASSWORD" \
  -v display_name="$DISPLAY_NAME" \
  -v legal_name="$LEGAL_NAME" \
  -v portal_role="$PORTAL_ROLE" <<'SQL'
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO identity.organization (
  organization_id,
  legal_name,
  display_name,
  status,
  default_timezone
) VALUES (
  :'org_id',
  :'legal_name',
  :'legal_name',
  'active',
  'UTC'
)
ON CONFLICT (organization_id) DO UPDATE
SET legal_name = EXCLUDED.legal_name,
    display_name = EXCLUDED.display_name,
    status = 'active',
    default_timezone = 'UTC',
    updated_at = now(),
    version = identity.organization.version + 1;

INSERT INTO identity.user_account (
  user_account_id,
  organization_id,
  email,
  display_name,
  status,
  password_hash
) VALUES (
  :'user_id',
  :'org_id',
  :'email',
  :'display_name',
  'active',
  crypt(:'password', gen_salt('bf'))
)
ON CONFLICT (user_account_id) DO UPDATE
SET organization_id = EXCLUDED.organization_id,
    email = EXCLUDED.email,
    display_name = EXCLUDED.display_name,
    status = 'active',
    password_hash = crypt(:'password', gen_salt('bf')),
    updated_at = now(),
    version = identity.user_account.version + 1;

INSERT INTO identity.role_assignment (
  role_assignment_id,
  organization_id,
  user_account_id,
  role,
  scope_type,
  status,
  reason
)
SELECT
  gen_random_uuid(),
  :'org_id',
  :'user_id',
  :'portal_role'::governance.actor_role,
  'organization',
  'active',
  'Local development bootstrap'
WHERE NOT EXISTS (
  SELECT 1
  FROM identity.role_assignment
  WHERE organization_id = :'org_id'
    AND user_account_id = :'user_id'
    AND role = :'portal_role'::governance.actor_role
    AND scope_type = 'organization'
    AND status = 'active'
);

SELECT
  :'org_id' AS organization_id,
  :'email' AS email,
  :'portal_role' AS portal_role,
  'ready' AS status;
SQL

