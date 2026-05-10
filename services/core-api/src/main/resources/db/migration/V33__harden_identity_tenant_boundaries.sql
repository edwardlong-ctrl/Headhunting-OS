-- ============================================================================
-- V33: Harden Identity Tenant Boundaries (Task 51)
--
-- Adds organization-scoped identity constraints for auth/session/support/audit
-- surfaces that rely on identity.user_account. Existing simple FKs preserve
-- entity existence; these composite FKs additionally ensure the referenced user
-- belongs to the same organization as the row being written.
--
-- Constraints are NOT VALID to avoid retroactive migration risk on older local
-- data, but PostgreSQL still enforces them for new or updated rows.
-- ============================================================================

ALTER TABLE identity.user_account
  ADD CONSTRAINT uq_user_account_id_org UNIQUE (user_account_id, organization_id);

COMMENT ON CONSTRAINT uq_user_account_id_org ON identity.user_account IS
  'Task 51: stable organization-scoped identity parent key for tenant-boundary composite FKs.';

ALTER TABLE identity.role_assignment
  ADD CONSTRAINT uq_role_assignment_id_org UNIQUE (role_assignment_id, organization_id),
  ADD CONSTRAINT fk_role_assignment_user_org
    FOREIGN KEY (user_account_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID,
  ADD CONSTRAINT fk_role_assignment_granted_by_org
    FOREIGN KEY (granted_by, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID,
  ADD CONSTRAINT fk_role_assignment_revoked_by_org
    FOREIGN KEY (revoked_by, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

COMMENT ON CONSTRAINT fk_role_assignment_user_org ON identity.role_assignment IS
  'Task 51: role assignments cannot bind a user account from another organization.';

ALTER TABLE identity.session
  ADD CONSTRAINT uq_session_id_org UNIQUE (session_id, organization_id),
  ADD CONSTRAINT fk_session_user_org
    FOREIGN KEY (user_account_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

COMMENT ON CONSTRAINT fk_session_user_org ON identity.session IS
  'Task 51: authenticated sessions cannot bind a user account from another organization.';

ALTER TABLE audit.audit_log
  ADD CONSTRAINT fk_audit_log_actor_user_org
    FOREIGN KEY (actor_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

COMMENT ON CONSTRAINT fk_audit_log_actor_user_org ON audit.audit_log IS
  'Task 51: access/support audit rows must be scoped to an actor in the same organization.';

ALTER TABLE workflow.workflow_event
  ADD CONSTRAINT fk_workflow_event_actor_user_org
    FOREIGN KEY (actor_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

COMMENT ON CONSTRAINT fk_workflow_event_actor_user_org ON workflow.workflow_event IS
  'Task 51: workflow events cannot attribute transitions to a user from another organization.';

ALTER TABLE governance.review_event
  ADD CONSTRAINT fk_review_event_reviewer_user_org
    FOREIGN KEY (reviewer_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

ALTER TABLE governance.canonical_write_attempt
  ADD CONSTRAINT fk_canonical_write_attempt_actor_user_org
    FOREIGN KEY (actor_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

ALTER TABLE governance.ai_task_run
  ADD CONSTRAINT fk_ai_task_run_requested_by_user_org
    FOREIGN KEY (requested_by_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

ALTER TABLE privacy.client_unlock_request
  ADD CONSTRAINT fk_client_unlock_request_actor_user_org
    FOREIGN KEY (client_actor_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

ALTER TABLE operations.notification
  ADD CONSTRAINT fk_notification_recipient_user_org
    FOREIGN KEY (recipient_user_account_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

ALTER TABLE operations.notification_preference
  ADD CONSTRAINT fk_notification_preference_user_org
    FOREIGN KEY (user_account_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID,
  ADD CONSTRAINT fk_notification_preference_updated_by_user_org
    FOREIGN KEY (updated_by_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

ALTER TABLE operations.notification_schedule
  ADD CONSTRAINT fk_notification_schedule_user_org
    FOREIGN KEY (user_account_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

ALTER TABLE recruiting.follow_up_submission
  ADD CONSTRAINT fk_follow_up_submission_submitter_user_org
    FOREIGN KEY (submitted_by_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID,
  ADD CONSTRAINT fk_follow_up_submission_reviewer_user_org
    FOREIGN KEY (reviewed_by_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

ALTER TABLE recruiting.commission
  ADD CONSTRAINT fk_commission_consultant_user_org
    FOREIGN KEY (consultant_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID;

COMMENT ON CONSTRAINT fk_commission_consultant_user_org ON recruiting.commission IS
  'Task 51: commission consultant links must remain inside the same organization.';
