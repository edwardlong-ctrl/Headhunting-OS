-- ============================================================================
-- V15: Identity/Auth infrastructure baseline
--
-- Extends existing identity.user_account with local password authentication and
-- adds identity.session for refresh-token-backed session lifecycle.
-- This task intentionally does NOT migrate existing product controllers away
-- from temporary header-based access context. That remains Task 19B.
-- ============================================================================

ALTER TABLE identity.user_account
  ADD COLUMN password_hash text;
COMMENT ON COLUMN identity.user_account.password_hash IS
  'Task 19A: BCrypt hash for backend-owned local password authentication.';

CREATE TABLE identity.session (
  session_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  user_account_id uuid NOT NULL REFERENCES identity.user_account (user_account_id),
  role governance.actor_role NOT NULL,
  refresh_token_hash text NOT NULL,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  last_used_at timestamptz,
  version integer NOT NULL DEFAULT 1 CHECK (version > 0),
  CHECK (role::text IN ('owner', 'consultant', 'client', 'candidate', 'admin'))
);

CREATE UNIQUE INDEX session_active_refresh_token_hash_uidx
  ON identity.session (refresh_token_hash)
  WHERE revoked_at IS NULL;

CREATE INDEX session_user_account_active_idx
  ON identity.session (organization_id, user_account_id, revoked_at, expires_at);
