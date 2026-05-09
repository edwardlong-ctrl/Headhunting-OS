ALTER TABLE recruiting.industry_pack
  ADD COLUMN calibration_review_by timestamptz,
  ADD COLUMN gold_cases jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN negative_cases jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN pack_anti_patterns jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN score_caps jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN drift_signals jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE recruiting.industry_pack
  ADD CONSTRAINT industry_pack_gold_cases_array CHECK (jsonb_typeof(gold_cases) = 'array'),
  ADD CONSTRAINT industry_pack_negative_cases_array CHECK (jsonb_typeof(negative_cases) = 'array'),
  ADD CONSTRAINT industry_pack_anti_patterns_array CHECK (jsonb_typeof(pack_anti_patterns) = 'array'),
  ADD CONSTRAINT industry_pack_score_caps_array CHECK (jsonb_typeof(score_caps) = 'array'),
  ADD CONSTRAINT industry_pack_drift_signals_array CHECK (jsonb_typeof(drift_signals) = 'array');

INSERT INTO recruiting.industry_pack (
  industry_pack_id, pack_key, display_name, maturity, calibration_review_by,
  gold_cases, negative_cases, pack_anti_patterns, score_caps, drift_signals
) VALUES
  (
    '00000000-0000-0000-0000-000000280001', 'general', 'General', 'seeded', TIMESTAMPTZ '2026-09-30T00:00:00Z',
    jsonb_build_array('clear role scope plus verified achievements', 'stable job requirements with independent evidence'),
    jsonb_build_array('keyword-only resume with no work evidence', 'unclear seniority or compensation expectations'),
    jsonb_build_array('treating generic business titles as equivalent across functions'),
    jsonb_build_array('5 requires two independent high-trust evidence items', 'cold-start fallback cannot exceed 3'),
    jsonb_build_array('monitor generic title inflation')
  ),
  (
    '00000000-0000-0000-0000-000000280002', 'semiconductor', 'Semiconductor', 'production', TIMESTAMPTZ '2026-12-31T00:00:00Z',
    jsonb_build_array('DV candidate with SystemVerilog/UVM ownership and coverage closure', 'PD candidate with P&R, timing closure, and signoff evidence'),
    jsonb_build_array('software QA represented as IC verification', 'PCB layout represented as physical design'),
    jsonb_build_array('generic testing is not DV without verification ownership', 'generic CAD is not chip physical design'),
    jsonb_build_array('production calibrated pack can score 5 only with high-trust semiconductor evidence', 'stale ontology caps at 4'),
    '[]'::jsonb
  ),
  (
    '00000000-0000-0000-0000-000000280003', 'finance', 'Finance', 'seeded', TIMESTAMPTZ '2026-09-30T00:00:00Z',
    jsonb_build_array('quant candidate with model ownership and live trading or risk evidence', 'compliance candidate with regulator-facing case ownership'),
    jsonb_build_array('Excel reporting only represented as risk modeling', 'generic sales profile represented as private banking coverage'),
    jsonb_build_array('tool usage is not model ownership', 'finance title without product or regulatory context is weak evidence'),
    jsonb_build_array('5 requires external deal, audit, trading, or regulatory evidence', 'seeded pack requires review before production label'),
    jsonb_build_array('review product taxonomy drift across quant and wealth roles')
  ),
  (
    '00000000-0000-0000-0000-000000280004', 'healthcare', 'Healthcare', 'seeded', TIMESTAMPTZ '2026-09-30T00:00:00Z',
    jsonb_build_array('clinical operations candidate with trial phase and site evidence', 'market access candidate with reimbursement dossier ownership'),
    jsonb_build_array('general hospital admin represented as medical affairs', 'pharma sales represented as clinical development'),
    jsonb_build_array('healthcare claims need role, product, and regulated-context evidence'),
    jsonb_build_array('5 requires clinical, regulatory, reimbursement, or verified product evidence', 'seeded pack requires review before production label'),
    jsonb_build_array('review regulatory terminology drift')
  ),
  (
    '00000000-0000-0000-0000-000000280005', 'internet_ai', 'Internet / AI', 'seeded', TIMESTAMPTZ '2026-09-30T00:00:00Z',
    jsonb_build_array('AI engineer with deployed model or platform ownership', 'product leader with measurable growth and experiment evidence'),
    jsonb_build_array('prompt usage represented as ML engineering', 'generic web CRUD represented as platform architecture'),
    jsonb_build_array('AI buzzwords are weak without system, data, or deployment evidence'),
    jsonb_build_array('5 requires shipped system, model, platform, or metric evidence', 'seeded pack requires review before production label'),
    jsonb_build_array('review fast-changing AI tooling vocabulary')
  ),
  (
    '00000000-0000-0000-0000-000000280006', 'sales', 'Sales', 'seeded', TIMESTAMPTZ '2026-09-30T00:00:00Z',
    jsonb_build_array('enterprise sales candidate with quota attainment and deal-cycle evidence', 'channel leader with partner revenue and territory evidence'),
    jsonb_build_array('inside sales activity represented as enterprise closing', 'relationship claims without revenue evidence'),
    jsonb_build_array('sales fit needs quota, segment, ACV, cycle, and buyer-context evidence'),
    jsonb_build_array('5 requires verified quota or revenue evidence', 'seeded pack requires review before production label'),
    jsonb_build_array('review segment and channel taxonomy drift')
  ),
  (
    '00000000-0000-0000-0000-000000280007', 'executive_search', 'Executive Search', 'seeded', TIMESTAMPTZ '2026-09-30T00:00:00Z',
    jsonb_build_array('CXO candidate with board-level scope and transformation evidence', 'VP candidate with P&L, org scale, and succession evidence'),
    jsonb_build_array('manager title represented as executive scope', 'advisory role represented as operating leadership'),
    jsonb_build_array('executive fit requires scope, mandate, board, P&L, or transformation evidence'),
    jsonb_build_array('5 requires verified scope, outcome, and reference-grade evidence', 'seeded pack requires review before production label'),
    jsonb_build_array('review title inflation and confidentiality risk')
  ),
  (
    '00000000-0000-0000-0000-000000280008', 'manufacturing', 'Manufacturing', 'seeded', TIMESTAMPTZ '2026-09-30T00:00:00Z',
    jsonb_build_array('quality leader with yield, audit, and supplier corrective-action evidence', 'operations leader with line efficiency and safety evidence'),
    jsonb_build_array('warehouse coordination represented as plant operations', 'inspection activity represented as quality-system ownership'),
    jsonb_build_array('manufacturing claims need plant, process, quality, supply-chain, or engineering evidence'),
    jsonb_build_array('5 requires verified plant, quality, supply-chain, or production outcome evidence', 'seeded pack requires review before production label'),
    jsonb_build_array('review lean, quality, and supply-chain vocabulary drift')
  )
ON CONFLICT (pack_key) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  maturity = EXCLUDED.maturity,
  calibration_review_by = EXCLUDED.calibration_review_by,
  gold_cases = EXCLUDED.gold_cases,
  negative_cases = EXCLUDED.negative_cases,
  pack_anti_patterns = EXCLUDED.pack_anti_patterns,
  score_caps = EXCLUDED.score_caps,
  drift_signals = EXCLUDED.drift_signals,
  updated_at = now();

ALTER TABLE recruiting.industry_pack
  ALTER COLUMN calibration_review_by SET NOT NULL;

INSERT INTO recruiting.ontology_version (
  ontology_version_id, industry_pack_id, version_key, source, owner, effective_from, review_by
) VALUES
  ('00000000-0000-0000-0000-000000280031', '00000000-0000-0000-0000-000000280001', 'ontology-general-v2', 'task47-calibration', 'system', TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280032', '00000000-0000-0000-0000-000000280002', 'ontology-semiconductor-v2', 'task47-production-calibration', 'system', TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280033', '00000000-0000-0000-0000-000000280003', 'ontology-finance-v1', 'task47-seed', 'system', TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280034', '00000000-0000-0000-0000-000000280004', 'ontology-healthcare-v1', 'task47-seed', 'system', TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280035', '00000000-0000-0000-0000-000000280005', 'ontology-internet-ai-v1', 'task47-seed', 'system', TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280036', '00000000-0000-0000-0000-000000280006', 'ontology-sales-v1', 'task47-seed', 'system', TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280037', '00000000-0000-0000-0000-000000280007', 'ontology-executive-search-v1', 'task47-seed', 'system', TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280038', '00000000-0000-0000-0000-000000280008', 'ontology-manufacturing-v1', 'task47-seed', 'system', TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z')
ON CONFLICT (industry_pack_id, version_key) DO UPDATE SET
  source = EXCLUDED.source,
  owner = EXCLUDED.owner,
  effective_from = EXCLUDED.effective_from,
  review_by = EXCLUDED.review_by,
  deprecated_at = NULL;

INSERT INTO recruiting.industry_role_family_template (
  industry_role_family_template_id, industry_pack_id, ontology_version_id, role_family, display_name,
  scorecard_dimensions, scoring_guidance, interview_question_templates, evidence_examples, anti_patterns, required_skill_keys
) VALUES
  ('00000000-0000-0000-0000-000000280301', '00000000-0000-0000-0000-000000280001', '00000000-0000-0000-0000-000000280031', 'general_recruiting', 'General Recruiting', 'role fit, evidence strength, motivation fit, compensation fit', 'Prefer verified responsibility, scope, and outcome evidence over generic keyword overlap.', jsonb_build_array('Which outcomes can be independently evidenced?', 'What scope and seniority are verified?'), jsonb_build_array('verified role scope', 'measurable outcome evidence'), jsonb_build_array('keyword-only role fit', 'unclear seniority'), jsonb_build_array('verified_scope', 'outcome_evidence')),
  ('00000000-0000-0000-0000-000000280302', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'dv_verification', 'DV / Verification', 'technical fit, industry fit, evidence strength, tapeout relevance', 'Reward SystemVerilog/UVM, coverage closure, assertions, regression ownership, and tapeout-stage verification evidence.', jsonb_build_array('Which blocks did you verify with SystemVerilog and UVM?', 'How did you drive coverage closure or assertion strategy?'), jsonb_build_array('SystemVerilog testbench ownership', 'UVM environment bring-up', 'Coverage closure metrics'), jsonb_build_array('software QA or software testing without IC verification evidence'), jsonb_build_array('systemverilog', 'uvm', 'coverage_closure', 'assertion')),
  ('00000000-0000-0000-0000-000000280321', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'physical_design', 'Physical Design', 'technical fit, industry fit, evidence strength, signoff relevance', 'Reward backend chip implementation evidence such as floorplan, P&R, timing closure, STA, IR drop, EM, and advanced-node signoff ownership.', jsonb_build_array('Which blocks did you own through floorplan or P&R?', 'What timing closure or signoff issues did you resolve?'), jsonb_build_array('floorplan ownership', 'place and route execution', 'timing closure and signoff evidence'), jsonb_build_array('PCB layout or generic CAD without chip physical-design evidence'), jsonb_build_array('floorplan', 'place_route', 'timing_closure', 'sta', 'signoff')),
  ('00000000-0000-0000-0000-000000280322', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'dft', 'DFT', 'technical fit, industry fit, evidence strength, test-coverage relevance', 'Reward scan, ATPG, BIST, JTAG, compression, and fault-coverage ownership; avoid generic manufacturing-test inflation.', jsonb_build_array('Which DFT flows or ATPG tooling did you own?', 'How did you improve scan or fault coverage?'), jsonb_build_array('ATPG pattern generation', 'scan insertion or debug', 'fault coverage improvement'), jsonb_build_array('manufacturing quality test without DFT ownership evidence'), jsonb_build_array('scan', 'atpg', 'bist', 'jtag', 'fault_coverage')),
  ('00000000-0000-0000-0000-000000280323', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'analog_mixed_signal', 'Analog / Mixed Signal', 'technical fit, industry fit, block ownership, integration evidence', 'Reward analog and mixed-signal block evidence such as PLL, ADC/DAC, SerDes, LDO, simulation, layout parasitic, and noise trade-off work.', jsonb_build_array('Which analog or mixed-signal blocks did you design or validate?', 'How did you handle parasitic, noise, or simulation trade-offs?'), jsonb_build_array('PLL or ADC/DAC ownership', 'SerDes or LDO evidence', 'layout parasitic mitigation'), jsonb_build_array('pure digital frontend represented as analog design'), jsonb_build_array('pll', 'adc', 'dac', 'serdes', 'ldo', 'simulation')),
  ('00000000-0000-0000-0000-000000280324', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'firmware_embedded', 'Firmware / Embedded', 'technical fit, hardware-near evidence, bring-up ownership, debug evidence', 'Reward low-level firmware and embedded ownership such as bring-up, drivers, RTOS, SoC integration, board support, and hardware-near debug.', jsonb_build_array('What low-level bring-up or driver work did you own?', 'Which RTOS or board-support responsibilities were yours?'), jsonb_build_array('board bring-up ownership', 'device-driver evidence', 'RTOS integration', 'SoC debug'), jsonb_build_array('upper-layer application work represented as firmware'), jsonb_build_array('bring_up', 'driver', 'rtos', 'soc_integration', 'board_support')),
  ('00000000-0000-0000-0000-000000280303', '00000000-0000-0000-0000-000000280003', '00000000-0000-0000-0000-000000280033', 'quant_risk', 'Quant / Risk', 'model evidence, product context, regulatory fit, outcome evidence', 'Reward verified model ownership, portfolio/risk impact, and controlled production use.', jsonb_build_array('Which model or risk framework did you own?', 'What production or regulatory evidence exists?'), jsonb_build_array('risk model ownership', 'live strategy or portfolio impact'), jsonb_build_array('Excel reporting only', 'generic analytics without financial context'), jsonb_build_array('risk_model', 'portfolio_impact')),
  ('00000000-0000-0000-0000-000000280304', '00000000-0000-0000-0000-000000280004', '00000000-0000-0000-0000-000000280034', 'clinical_medical_affairs', 'Clinical / Medical Affairs', 'regulated evidence, product context, stakeholder fit, outcome evidence', 'Reward trial, medical, regulatory, or market-access evidence with explicit product and phase context.', jsonb_build_array('Which trial, product, or dossier did you own?', 'What regulated stakeholder evidence exists?'), jsonb_build_array('trial phase ownership', 'reimbursement dossier evidence'), jsonb_build_array('hospital admin only', 'sales activity as clinical development'), jsonb_build_array('clinical_trial', 'regulatory_dossier')),
  ('00000000-0000-0000-0000-000000280305', '00000000-0000-0000-0000-000000280005', '00000000-0000-0000-0000-000000280035', 'ai_platform_engineering', 'AI / Platform Engineering', 'system ownership, data/model fit, production evidence, product impact', 'Reward shipped systems, model/data ownership, production reliability, and measurable product impact.', jsonb_build_array('Which model, data, or platform component shipped?', 'What production metric changed?'), jsonb_build_array('deployed model service', 'platform reliability metric'), jsonb_build_array('prompt usage only', 'CRUD app represented as platform architecture'), jsonb_build_array('deployed_model', 'platform_reliability')),
  ('00000000-0000-0000-0000-000000280306', '00000000-0000-0000-0000-000000280006', '00000000-0000-0000-0000-000000280036', 'enterprise_sales', 'Enterprise Sales', 'quota evidence, segment fit, buyer map, deal-cycle evidence', 'Reward quota attainment, ACV/segment fit, enterprise buyer access, and repeatable deal execution.', jsonb_build_array('What quota and ACV did you carry?', 'Which buyer groups and deal cycles are verified?'), jsonb_build_array('quota attainment', 'enterprise deal-cycle ownership'), jsonb_build_array('activity volume without revenue evidence'), jsonb_build_array('quota_attainment', 'deal_cycle')),
  ('00000000-0000-0000-0000-000000280307', '00000000-0000-0000-0000-000000280007', '00000000-0000-0000-0000-000000280037', 'cxo_vp', 'CXO / VP Search', 'scope evidence, mandate fit, leadership outcomes, confidentiality risk', 'Reward verified P&L, board, transformation, succession, and organization-scale evidence.', jsonb_build_array('What mandate and org scale are verified?', 'Which transformation outcomes are evidenced?'), jsonb_build_array('P&L ownership', 'board-level transformation'), jsonb_build_array('manager title as executive scope'), jsonb_build_array('p_l_ownership', 'board_scope')),
  ('00000000-0000-0000-0000-000000280308', '00000000-0000-0000-0000-000000280008', '00000000-0000-0000-0000-000000280038', 'quality_operations', 'Quality / Operations', 'plant context, process evidence, quality system, outcome evidence', 'Reward plant, process, quality, supply-chain, safety, and production outcome evidence.', jsonb_build_array('Which plant, process, or quality system did you own?', 'What yield, safety, or efficiency evidence exists?'), jsonb_build_array('yield improvement', 'supplier corrective action'), jsonb_build_array('warehouse coordination as plant operations'), jsonb_build_array('yield_improvement', 'quality_system'))
ON CONFLICT (ontology_version_id, role_family) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  scorecard_dimensions = EXCLUDED.scorecard_dimensions,
  scoring_guidance = EXCLUDED.scoring_guidance,
  interview_question_templates = EXCLUDED.interview_question_templates,
  evidence_examples = EXCLUDED.evidence_examples,
  anti_patterns = EXCLUDED.anti_patterns,
  required_skill_keys = EXCLUDED.required_skill_keys;

INSERT INTO recruiting.skill_concept (
  skill_concept_id, industry_pack_id, ontology_version_id, role_family, concept_key, label,
  aliases, definition, evidence_examples, anti_patterns, effective_from, review_by
) VALUES
  ('00000000-0000-0000-0000-000000280401', '00000000-0000-0000-0000-000000280001', '00000000-0000-0000-0000-000000280031', 'general_recruiting', 'outcome_evidence', 'Outcome Evidence', jsonb_build_array('impact evidence'), 'Verified business outcome evidence tied to the role scope.', jsonb_build_array('metric, shipped artifact, signed feedback, or audited outcome'), jsonb_build_array('self-described responsibility only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280402', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'dv_verification', 'coverage_closure', 'Coverage Closure', jsonb_build_array('functional coverage'), 'Evidence that the candidate owned verification closure rather than only running tests.', jsonb_build_array('coverage plan, regression closure, signoff evidence'), jsonb_build_array('manual QA only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280421', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'physical_design', 'timing_closure', 'Timing Closure', jsonb_build_array('sta'), 'Closing timing across backend chip implementation and signoff.', jsonb_build_array('resolved setup or hold violations during backend flow'), jsonb_build_array('PCB routing only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280422', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'dft', 'atpg', 'ATPG', jsonb_build_array('automatic test pattern generation'), 'Generating structural test patterns and improving fault coverage.', jsonb_build_array('ATPG ownership or scan debug'), jsonb_build_array('manufacturing quality inspection only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280423', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'analog_mixed_signal', 'pll', 'PLL', jsonb_build_array('phase locked loop'), 'Analog timing and synchronization block common in mixed-signal design.', jsonb_build_array('PLL design or verification evidence'), jsonb_build_array('pure digital frontend only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280424', '00000000-0000-0000-0000-000000280002', '00000000-0000-0000-0000-000000280032', 'firmware_embedded', 'rtos', 'RTOS', jsonb_build_array('real time operating system'), 'Real-time operating systems for embedded and firmware workloads.', jsonb_build_array('RTOS integration or scheduling ownership'), jsonb_build_array('top-layer application work only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-12-31T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280403', '00000000-0000-0000-0000-000000280003', '00000000-0000-0000-0000-000000280033', 'quant_risk', 'risk_model', 'Risk Model', jsonb_build_array('model risk'), 'Financial risk model ownership with product, portfolio, or regulatory context.', jsonb_build_array('model validation or production risk framework'), jsonb_build_array('generic dashboard only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280404', '00000000-0000-0000-0000-000000280004', '00000000-0000-0000-0000-000000280034', 'clinical_medical_affairs', 'regulatory_dossier', 'Regulatory Dossier', jsonb_build_array('submission dossier'), 'Regulated healthcare dossier, submission, or evidence package ownership.', jsonb_build_array('NMPA, FDA, CE, or reimbursement dossier evidence'), jsonb_build_array('generic admin paperwork'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280405', '00000000-0000-0000-0000-000000280005', '00000000-0000-0000-0000-000000280035', 'ai_platform_engineering', 'deployed_model', 'Deployed Model', jsonb_build_array('production model'), 'Model or AI system deployed into a production workflow with observable behavior.', jsonb_build_array('serving, evaluation, monitoring, or product metric evidence'), jsonb_build_array('prompt use only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280406', '00000000-0000-0000-0000-000000280006', '00000000-0000-0000-0000-000000280036', 'enterprise_sales', 'quota_attainment', 'Quota Attainment', jsonb_build_array('quota'), 'Verified quota carrying and attainment evidence for the relevant segment.', jsonb_build_array('quota letter, CRM summary, finance-confirmed revenue'), jsonb_build_array('activity count only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280407', '00000000-0000-0000-0000-000000280007', '00000000-0000-0000-0000-000000280037', 'cxo_vp', 'board_scope', 'Board Scope', jsonb_build_array('board-level'), 'Leadership scope involving board, P&L, or enterprise transformation context.', jsonb_build_array('board mandate, transformation record, P&L scope'), jsonb_build_array('advisory-only title'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z'),
  ('00000000-0000-0000-0000-000000280408', '00000000-0000-0000-0000-000000280008', '00000000-0000-0000-0000-000000280038', 'quality_operations', 'quality_system', 'Quality System', jsonb_build_array('QMS'), 'Manufacturing quality system ownership tied to plant or supplier outcomes.', jsonb_build_array('audit closure, yield, supplier corrective-action evidence'), jsonb_build_array('inspection activity only'), TIMESTAMPTZ '2026-05-01T00:00:00Z', TIMESTAMPTZ '2026-09-30T00:00:00Z')
ON CONFLICT (ontology_version_id, role_family, concept_key) DO UPDATE SET
  label = EXCLUDED.label,
  aliases = EXCLUDED.aliases,
  definition = EXCLUDED.definition,
  evidence_examples = EXCLUDED.evidence_examples,
  anti_patterns = EXCLUDED.anti_patterns,
  review_by = EXCLUDED.review_by,
  deprecated_at = NULL,
  replaced_by = NULL;
