ALTER TABLE governance.ai_task_run
  ADD COLUMN input_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN output_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN replayed_from_ai_task_run_id uuid REFERENCES governance.ai_task_run (ai_task_run_id);

CREATE INDEX ai_task_run_replay_idx
  ON governance.ai_task_run (organization_id, replayed_from_ai_task_run_id)
  WHERE replayed_from_ai_task_run_id IS NOT NULL;

COMMENT ON COLUMN governance.ai_task_run.input_payload IS
  'Validated task input payload used for replayable AI task execution. Not canonical fact storage.';

COMMENT ON COLUMN governance.ai_task_run.output_payload IS
  'Validated task output payload captured for audited replayable AI task execution. Not canonical fact storage.';

COMMENT ON COLUMN governance.ai_task_run.replayed_from_ai_task_run_id IS
  'When present, points to the original AI task run that was explicitly replayed.';
