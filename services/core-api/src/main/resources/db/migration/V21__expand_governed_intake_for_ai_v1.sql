ALTER TABLE intake.extraction_run
  DROP CONSTRAINT IF EXISTS intake_extraction_run_mode_check;

ALTER TABLE intake.extraction_run
  ADD CONSTRAINT intake_extraction_run_mode_check
    CHECK (mode IN (
      'DETERMINISTIC_PLACEHOLDER',
      'DOCUMENT_INTELLIGENCE_V1',
      'GOVERNED_AI_V1'
    ));
