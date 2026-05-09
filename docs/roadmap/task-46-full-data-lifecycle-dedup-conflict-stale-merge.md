# Task 46: Full Data Lifecycle, Deduplication, Conflict, Stale, and Merge

## Scope

Task 46 closes the first backend-owned data lifecycle decision layer for
candidate, company, and job data quality. The slice makes duplicate, merge,
conflict, stale, refresh, and retention decisions auditable before any canonical
record can be changed.

## Delivered

- Added `DataLifecycleService` as the backend-owned policy boundary and
  `DataLifecycleModels` as the public command/result contract set for:
  - candidate/company/job duplicate detection.
  - high-confidence duplicate blocks.
  - low-confidence duplicate warnings with justification.
  - merge proposals.
  - confirmed-fact merge conflict blocks.
  - conflict-resolution workflow recording.
  - stale-field detection and refresh workflow requests.
  - retention/deletion policy decisions with confirmed-fact tombstone
    protection.
- Added workflow action vocabulary and policies for all Task 46 lifecycle
  decisions:
  - `DATA_DUPLICATE_BLOCKED`
  - `DATA_DUPLICATE_WARNING_RECORDED`
  - `DATA_MERGE_PROPOSED`
  - `DATA_MERGE_BLOCKED_CONFIRMED_FACT_CONFLICT`
  - `DATA_CONFLICT_RESOLUTION_RECORDED`
  - `DATA_REFRESH_REQUESTED`
  - `DATA_RETENTION_DELETION_BLOCKED`
  - `DATA_RETENTION_DELETION_EXECUTED`
- Added `COMPANY` to the workflow entity vocabulary so company lifecycle
  decisions can be audited with the same policy registry as candidates and
  jobs.
- Wired Owner `data-quality` governance metrics to the existing
  `workflow.workflow_event` read model for duplicate blocks, merge conflict
  blocks, refresh requests, and retention/deletion decisions.

## Deliberate Non-Claims

- No physical row deletion executor is added.
- No direct merge mutation of `recruiting.candidate`, `recruiting.company`, or
  `recruiting.job` is added.
- No canonical CandidateProfile field overwrite is performed by the data
  lifecycle service.
- No external data-quality worker queue, fuzzy search index, or ML duplicate
  model is added.

These are intentional for Task 46: lifecycle decisions are proposals or audited
policy outcomes first. A later product slice can attach controlled mutation
executors behind the same audit vocabulary.

## Regression Coverage

- `DataLifecycleServiceTest` proves:
  - candidate high-confidence duplicate blocks by identity fingerprint.
  - company low-confidence duplicate warnings preserve justification.
  - job low-confidence duplicate warnings use same-company normalized title
    matching.
  - blank company/job identifiers do not create false duplicate warnings.
  - lifecycle audit state remains valid JSON when human-entered values contain
    quotes.
  - merge proposals block confirmed target fact overwrites.
  - conflict-resolution workflow decisions are audited without canonical
    mutation.
  - stale non-confirmed fields request refresh while confirmed facts are left
    alone.
  - retention/deletion execution blocks confirmed facts unless a tombstone is
    preserved.
- `WorkflowActionPolicyTest` proves the new data lifecycle actions are covered
  by the workflow action registry and bound to candidate/company/job entities.
