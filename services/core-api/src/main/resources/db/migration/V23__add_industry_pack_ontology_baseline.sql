CREATE TABLE recruiting.industry_pack (
  industry_pack_id uuid PRIMARY KEY,
  pack_key text NOT NULL UNIQUE CHECK (btrim(pack_key) <> ''),
  display_name text NOT NULL CHECK (btrim(display_name) <> ''),
  maturity text NOT NULL CHECK (maturity IN ('cold', 'seeded', 'calibrated', 'production')),
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE recruiting.ontology_version (
  ontology_version_id uuid PRIMARY KEY,
  industry_pack_id uuid NOT NULL REFERENCES recruiting.industry_pack (industry_pack_id),
  version_key text NOT NULL CHECK (btrim(version_key) <> ''),
  source text NOT NULL CHECK (btrim(source) <> ''),
  owner text NOT NULL CHECK (btrim(owner) <> ''),
  effective_from timestamptz NOT NULL,
  review_by timestamptz NOT NULL,
  deprecated_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (industry_pack_id, version_key)
);

CREATE TABLE recruiting.skill_concept (
  skill_concept_id uuid PRIMARY KEY,
  industry_pack_id uuid NOT NULL REFERENCES recruiting.industry_pack (industry_pack_id),
  ontology_version_id uuid NOT NULL REFERENCES recruiting.ontology_version (ontology_version_id),
  role_family text NOT NULL CHECK (btrim(role_family) <> ''),
  concept_key text NOT NULL CHECK (btrim(concept_key) <> ''),
  label text NOT NULL CHECK (btrim(label) <> ''),
  aliases jsonb NOT NULL DEFAULT '[]'::jsonb,
  definition text NOT NULL CHECK (btrim(definition) <> ''),
  evidence_examples jsonb NOT NULL DEFAULT '[]'::jsonb,
  anti_patterns jsonb NOT NULL DEFAULT '[]'::jsonb,
  effective_from timestamptz NOT NULL,
  review_by timestamptz NOT NULL,
  deprecated_at timestamptz,
  replaced_by text CHECK (replaced_by IS NULL OR btrim(replaced_by) <> ''),
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (ontology_version_id, role_family, concept_key)
);

CREATE TABLE recruiting.industry_role_family_template (
  industry_role_family_template_id uuid PRIMARY KEY,
  industry_pack_id uuid NOT NULL REFERENCES recruiting.industry_pack (industry_pack_id),
  ontology_version_id uuid NOT NULL REFERENCES recruiting.ontology_version (ontology_version_id),
  role_family text NOT NULL CHECK (btrim(role_family) <> ''),
  display_name text NOT NULL CHECK (btrim(display_name) <> ''),
  scorecard_dimensions text NOT NULL CHECK (btrim(scorecard_dimensions) <> ''),
  scoring_guidance text NOT NULL CHECK (btrim(scoring_guidance) <> ''),
  interview_question_templates jsonb NOT NULL DEFAULT '[]'::jsonb,
  evidence_examples jsonb NOT NULL DEFAULT '[]'::jsonb,
  anti_patterns jsonb NOT NULL DEFAULT '[]'::jsonb,
  required_skill_keys jsonb NOT NULL DEFAULT '[]'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (ontology_version_id, role_family)
);

CREATE INDEX industry_pack_pack_key_idx
  ON recruiting.industry_pack (pack_key);

CREATE INDEX ontology_version_pack_review_idx
  ON recruiting.ontology_version (industry_pack_id, review_by DESC, effective_from DESC);

CREATE INDEX skill_concept_pack_role_idx
  ON recruiting.skill_concept (industry_pack_id, role_family, concept_key);

CREATE INDEX industry_role_family_template_pack_role_idx
  ON recruiting.industry_role_family_template (industry_pack_id, role_family);

ALTER TABLE recruiting.job
  ADD CONSTRAINT fk_job_industry_pack
  FOREIGN KEY (industry_pack_id)
  REFERENCES recruiting.industry_pack (industry_pack_id);

ALTER TABLE recruiting.candidate
  ADD CONSTRAINT fk_candidate_default_industry_pack
  FOREIGN KEY (default_industry_pack_id)
  REFERENCES recruiting.industry_pack (industry_pack_id);

INSERT INTO recruiting.industry_pack (
  industry_pack_id, pack_key, display_name, maturity
) VALUES
  ('00000000-0000-0000-0000-000000280001', 'general', 'General', 'cold'),
  ('00000000-0000-0000-0000-000000280002', 'semiconductor', 'Semiconductor', 'seeded');

INSERT INTO recruiting.ontology_version (
  ontology_version_id, industry_pack_id, version_key, source, owner, effective_from, review_by
) VALUES
  ('00000000-0000-0000-0000-000000280011', '00000000-0000-0000-0000-000000280001', 'ontology-general-v1', 'task28-seed', 'system', TIMESTAMPTZ '2026-01-01T00:00:00Z', TIMESTAMPTZ '2027-01-01T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280012', '00000000-0000-0000-0000-000000280002', 'ontology-semiconductor-v1', 'task28-seed', 'system', TIMESTAMPTZ '2026-01-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z');

INSERT INTO recruiting.industry_role_family_template (
  industry_role_family_template_id, industry_pack_id, ontology_version_id, role_family, display_name,
  scorecard_dimensions, scoring_guidance, interview_question_templates, evidence_examples, anti_patterns, required_skill_keys
) VALUES
  (
    '00000000-0000-0000-0000-000000280101', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012',
    'dv_verification', 'DV / Verification',
    'technical fit, industry fit, evidence strength, motivation fit',
    'Reward direct SystemVerilog/UVM verification evidence, coverage closure ownership, regression infrastructure, and tapeout-stage verification examples.',
    '["Which blocks did you verify with SystemVerilog and UVM?", "How did you drive coverage closure or assertion strategy?", "What evidence shows tapeout-facing verification ownership?"]'::jsonb,
    '["SystemVerilog testbench ownership", "UVM environment bring-up", "Coverage closure metrics", "Tapeout verification signoff"]'::jsonb,
    '["Treating software QA or generic manual testing as IC verification", "Claiming DV fit without SystemVerilog/UVM or verification ownership evidence"]'::jsonb,
    '["systemverilog", "uvm", "coverage_closure", "assertion", "regression_infra", "tapeout_verification"]'::jsonb
  ),
  (
    '00000000-0000-0000-0000-000000280102', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012',
    'physical_design', 'Physical Design',
    'technical fit, industry fit, evidence strength, availability fit',
    'Reward backend chip implementation evidence such as floorplan, P&R, timing closure, signoff, and advanced-node ownership.',
    '["Which blocks did you own through floorplan or P&R?", "What timing closure or STA issues did you resolve?", "What signoff or advanced-node work can you evidence?"]'::jsonb,
    '["Floorplan ownership", "Place and route execution", "STA/timing closure examples", "IR drop or EM signoff work"]'::jsonb,
    '["Treating PCB layout or generic CAD work as chip physical design", "Claiming PD fit without backend implementation evidence"]'::jsonb,
    '["floorplan", "place_route", "timing_closure", "sta", "ir_drop", "em", "signoff", "advanced_node"]'::jsonb
  ),
  (
    '00000000-0000-0000-0000-000000280103', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012',
    'dft', 'DFT',
    'technical fit, industry fit, evidence strength, seniority fit',
    'Reward direct scan/ATPG/BIST/JTAG/fault-coverage ownership and avoid generic manufacturing-test inflation.',
    '["Which DFT flows or ATPG tooling did you own?", "How did you improve scan or fault coverage?", "What BIST/JTAG work is directly evidenced?"]'::jsonb,
    '["ATPG pattern generation", "Scan insertion or debug", "BIST implementation", "Fault coverage improvement"]'::jsonb,
    '["Treating manufacturing quality test as DFT", "Claiming DFT fit without scan or ATPG evidence"]'::jsonb,
    '["scan", "atpg", "bist", "jtag", "fault_coverage", "test_compression"]'::jsonb
  ),
  (
    '00000000-0000-0000-0000-000000280104', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012',
    'analog_mixed_signal', 'Analog / Mixed Signal',
    'technical fit, industry fit, evidence strength, culture or manager fit',
    'Reward direct analog/mixed-signal design evidence such as PLL, ADC/DAC, SerDes, LDO, simulation, and parasitic/noise trade-off work.',
    '["Which analog blocks did you design or validate?", "How did you handle parasitic, noise, or simulation trade-offs?", "What mixed-signal integration evidence is strongest?"]'::jsonb,
    '["PLL design examples", "ADC/DAC ownership", "SerDes or LDO evidence", "Layout parasitic or noise mitigation work"]'::jsonb,
    '["Treating generic digital frontend work as analog design", "Claiming analog fit without block-level analog evidence"]'::jsonb,
    '["pll", "adc", "dac", "serdes", "ldo", "simulation", "layout_parasitic", "noise"]'::jsonb
  ),
  (
    '00000000-0000-0000-0000-000000280105', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012',
    'firmware_embedded', 'Firmware / Embedded',
    'technical fit, industry fit, evidence strength, motivation fit',
    'Reward low-level firmware and embedded ownership such as bring-up, drivers, RTOS, SoC integration, board support, and debug.',
    '["What low-level bring-up or driver work did you own?", "Which RTOS or board-support responsibilities were yours?", "How did you debug SoC or firmware integration issues?"]'::jsonb,
    '["Board bring-up ownership", "Device-driver evidence", "RTOS integration", "SoC debug and board-support work"]'::jsonb,
    '["Treating upper-layer application development as firmware", "Claiming embedded fit without low-level hardware-near evidence"]'::jsonb,
    '["bring_up", "driver", "rtos", "soc_integration", "debug", "board_support"]'::jsonb
  );

INSERT INTO recruiting.skill_concept (
  skill_concept_id, industry_pack_id, ontology_version_id, role_family, concept_key, label,
  aliases, definition, evidence_examples, anti_patterns, effective_from, review_by
) VALUES
  ('00000000-0000-0000-0000-000000280201', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012', 'dv_verification', 'systemverilog', 'SystemVerilog', '["sv"]'::jsonb, 'Hardware verification language used in IC verification environments.', '["SystemVerilog testbench ownership"]'::jsonb, '["Generic software testing only"]'::jsonb, TIMESTAMPTZ '2026-01-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280202', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012', 'dv_verification', 'uvm', 'UVM', '["universal verification methodology"]'::jsonb, 'Verification methodology for reusable testbench environments.', '["Built UVM environment or components"]'::jsonb, '["Manual QA without verification methodology"]'::jsonb, TIMESTAMPTZ '2026-01-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280203', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012', 'physical_design', 'timing_closure', 'Timing Closure', '["sta"]'::jsonb, 'Closing timing across backend implementation and signoff.', '["Resolved setup/hold violations during backend flow"]'::jsonb, '["PCB routing only"]'::jsonb, TIMESTAMPTZ '2026-01-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280204', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012', 'dft', 'atpg', 'ATPG', '["automatic test pattern generation"]'::jsonb, 'Generating structural test patterns and improving fault coverage.', '["ATPG ownership or debug"]'::jsonb, '["Manufacturing quality inspection only"]'::jsonb, TIMESTAMPTZ '2026-01-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280205', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012', 'analog_mixed_signal', 'pll', 'PLL', '["phase locked loop"]'::jsonb, 'Analog timing/synchronization block common in mixed-signal design.', '["PLL design or verification evidence"]'::jsonb, '["Pure digital frontend only"]'::jsonb, TIMESTAMPTZ '2026-01-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280206', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280012', 'firmware_embedded', 'rtos', 'RTOS', '["real time operating system"]'::jsonb, 'Real-time operating systems for embedded and firmware workloads.', '["RTOS integration or scheduling ownership"]'::jsonb, '["Top-layer application work only"]'::jsonb, TIMESTAMPTZ '2026-01-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z');
