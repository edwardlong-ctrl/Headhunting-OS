# Task 47: Industry Pack Expansion and Calibration

## Scope

Task 47 closes the first backend-owned multi-pack calibration baseline for all
v2.1 industry packs. The slice keeps pack maturity honest: only the
semiconductor pack is marked `production`; the newly expanded packs are seeded
with curated rules, examples, score caps, and drift signals, but are not claimed
as production-calibrated.

## Delivered

- Added `V32__expand_industry_pack_calibration.sql` as an append-only migration.
- Expanded `recruiting.industry_pack` with calibration governance fields:
  `calibration_review_by`, `gold_cases`, `negative_cases`,
  `pack_anti_patterns`, `score_caps`, and `drift_signals`.
- Seeded all v2.1 industry packs: `general`, `semiconductor`, `finance`,
  `healthcare`, `internet_ai`, `sales`, `executive_search`, and
  `manufacturing`.
- Added active ontology versions, role-family templates, skill concepts,
  gold cases, negative cases, anti-patterns, score caps, and review deadlines
  for every pack.
- Promoted `semiconductor` to an honest `production` maturity state with a
  Task 47 `ontology-semiconductor-v2` calibration baseline.
- Preserved the existing semiconductor production role-family surface on the
  active v2 ontology: `dv_verification`, `physical_design`, `dft`,
  `analog_mixed_signal`, and `firmware_embedded`.
- Kept all non-semiconductor packs at `seeded` maturity with explicit score-cap
  text requiring high-trust evidence for any 5-score behavior.
- Added `IndustryPackCalibrationProfile` and `IndustryPackReviewQueueItem`
  contracts plus read-service behavior for calibration profile listing and
  deterministic review queue generation.
- Updated Admin `industry-packs` and `ontology-governance` read models to show
  Task 47 pack count, production-pack count, review queue count, maturity,
  ontology version, review deadline, gold/negative/anti-pattern/score-cap
  coverage, and drift signal counts.

## Deliberate Non-Claims

- No learned calibration executor is added.
- No automatic ontology update from outcomes is added.
- No admin write UI for editing industry packs is added.
- No pack other than `semiconductor` is labeled `production`.
- No client-facing match report delivery is added.

These are intentional for Task 47: the product now has honest multi-pack
metadata and a review queue surface, while automatic calibration remains future
production work.

## Regression Coverage

- `JdbcIndustryPackReadPortIntegrationTest` proves all 8 v2.1 industry packs
  exist with active ontology review metadata, gold cases, negative cases,
  anti-patterns, score caps, no fake production labels, and no regression of
  active semiconductor production role families.
- `GovernanceReadServicePostgresIntegrationTest` proves Admin
  `industry-packs` exposes 8 packs, 1 production pack, and 7 review-queue items
  against a real migrated PostgreSQL schema, and that a production pack whose
  ontology or calibration review deadline expires is reclassified into the
  review queue.
