ALTER TABLE governance.ai_task_run
  ADD COLUMN requested_by_user_id uuid REFERENCES identity.user_account (user_account_id),
  ADD COLUMN requested_by_role governance.actor_role,
  ADD COLUMN correlation_id uuid,
  ADD COLUMN causation_id uuid,
  ADD COLUMN failure_reason text;

ALTER TABLE governance.ai_task_run
  DROP CONSTRAINT ai_task_run_status_check;

ALTER TABLE governance.ai_task_run
  ADD CONSTRAINT ai_task_run_status_check
  CHECK (status IN ('created', 'running', 'succeeded', 'failed', 'cancelled'));

ALTER TABLE governance.ai_task_run
  ADD CONSTRAINT ai_task_run_completion_order_chk
  CHECK (completed_at IS NULL OR completed_at >= started_at);

ALTER TABLE governance.ai_task_run
  ADD CONSTRAINT ai_task_run_failed_reason_chk
  CHECK (status <> 'failed' OR failure_reason IS NOT NULL AND btrim(failure_reason) <> '');

CREATE INDEX ai_task_run_requested_by_idx
  ON governance.ai_task_run (organization_id, requested_by_user_id, started_at);

CREATE INDEX ai_task_run_correlation_idx
  ON governance.ai_task_run (organization_id, correlation_id)
  WHERE correlation_id IS NOT NULL;

CREATE INDEX ai_task_run_causation_idx
  ON governance.ai_task_run (organization_id, causation_id)
  WHERE causation_id IS NOT NULL;

COMMENT ON COLUMN governance.ai_task_run.failure_reason IS
  'Safe single-line failure reason only; do not store stack traces, provider secrets, prompts, or raw model output here.';
