ALTER TABLE recruiting.match_report
  ADD COLUMN industry_pack_key text CHECK (industry_pack_key IS NULL OR btrim(industry_pack_key) <> ''),
  ADD COLUMN industry_pack_maturity text CHECK (
    industry_pack_maturity IS NULL OR
    industry_pack_maturity IN ('cold', 'seeded', 'calibrated', 'production')
  ),
  ADD COLUMN ontology_stale boolean,
  ADD COLUMN selection_reason text CHECK (selection_reason IS NULL OR btrim(selection_reason) <> ''),
  ADD COLUMN anti_pattern_warnings jsonb;
