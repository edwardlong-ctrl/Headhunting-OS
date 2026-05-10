# Task 55: Data Import and Migration from Existing Systems

Date: 2026-05-10

Branch: `codex/task-55-import-migration`

## Scope

Task 55 adds the first governed historical import/migration boundary for real
customer data. It lets teams bring legacy ATS/CRM rows and resume/document
batches into the existing v2.1 truth pipeline without bypassing governed
intake, claim review, or organization boundaries.

This task does not implement Task 49 external provider integrations, Task 56
support tooling, Task 57 reporting/legal export packages, or direct confirmed
canonical fact writes.

## Delivered

- Added `importmigration` backend package with:
  - `ImportMigrationService` for CSV import, document batch import, validation
    reporting, duplicate screening, and rollback/reset orchestration.
  - `ImportMigrationModels` for import batches, source type, validation status,
    row/document counts, duplicate/conflict counts, rollback status, validation
    reports, legacy mappings, draft records, entity references, and governed
    lineage.
  - `GovernedImportGateway` as the service boundary for governed writes.
  - `GovernedIntakeImportGateway` as the concrete adapter into existing
    `GovernedIntakeService`, `DocumentUploadService`,
    `IntakeExtractionRunPort`, and `IntakeClaimLedgerBridgeService` APIs.
  - `ImportBatchRepository` as the batch persistence boundary.
- Added strict CSV import behavior for candidate, company, and job rows:
  - all rows are parsed and validated before any governed write;
  - missing required fields produce row-level validation errors;
  - job company references are rejected when they resolve to another
    organization;
  - accepted rows are normalized as draft records, not canonical facts.
- Added legacy ATS/CRM mapping contracts:
  - external headers normalize into import draft fields;
  - email/website values normalize for duplicate screening;
  - mapped values become claim candidates in the governed path, not confirmed
    canonical fields.
- Added Task 46 duplicate decision reuse:
  - incoming candidate/company/job drafts are converted to
    `DataLifecycleEntitySnapshot` inputs;
  - high-confidence duplicate decisions block the row and are reported as
    conflicts;
  - low-confidence duplicate decisions remain reportable warnings.
- Added resume/document batch import:
  - each document enters the existing document-upload path;
  - SourceItem, InformationPacket, filename, and legacy external ID lineage is
    preserved;
  - document imports remain pending governed review and do not write confirmed
    facts.
- Added rollback/reset behavior:
  - rollback is batch and organization scoped;
  - only lineages belonging to the requested batch and organization are passed
    to the governed gateway;
  - existing governed intake records remain append-only, with batch rollback
    status recording the reset outcome.

## Deliberate Non-Claims

- No live Greenhouse/Lever/Workday/Bullhorn or other provider connector is
  added.
- No support/admin repair console is added.
- No broad legal audit package or customer export feature is added.
- No direct mutation of confirmed canonical candidate, company, or job facts is
  performed.
- No fuzzy duplicate index or ML duplicate matcher is added; Task 55 reuses the
  current Task 46 decision concepts.

## Regression Coverage

- `ImportMigrationServiceTest` proves:
  - invalid CSV returns a validation report and writes no partial accepted
    data;
  - cross-organization references are rejected before governed writes;
  - wrong-entity job references are rejected before governed writes;
  - duplicate rows are detected and reported using Task 46 duplicate decision
    concepts;
  - accepted CSV rows enter SourceItem/InformationPacket/claim-ledger governed
    review lineage instead of confirmed facts;
  - rollback/reset touches only records from the requested batch and
    organization;
  - document batch import preserves document/source lineage;
  - invalid document batches return validation reports before any partial
    governed document writes;
  - oversized document batches fail validation before any partial governed
    document writes;
  - legacy ATS/CRM mappings normalize external fields into draft records, not
    canonical facts.

## Validation Evidence

Focused red evidence before adding the package:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=ImportMigrationServiceTest test
```

Result: failed at test compilation because the Task 55 import/migration package
did not exist.

Focused green evidence after implementation:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=ImportMigrationServiceTest test
```

Result: 10 tests, 0 failures, 0 errors, 0 skipped.

Required regression evidence:

```bash
rtk git diff --check
rtk docker info
rtk mvn -f services/core-api/pom.xml -Dtest=ImportMigrationServiceTest test
rtk mvn -f services/core-api/pom.xml -Dtest=PilotDataCommandTest,PilotDataPostgresIntegrationTest,GovernedIntakePostgresPersistenceIntegrationTest,GovernedIntakeEndToEndRegressionTest,DataLifecycleServiceTest test
rtk mvn -f services/core-api/pom.xml test
```

Results:

- `rtk git diff --check`: passed.
- `rtk docker info`: Docker client/server reachable.
- Task 55 focused suite: 10 tests, 0 failures, 0 errors, 0 skipped.
- Required pilot/governed-intake/data-lifecycle regression batch: 38 tests, 0
  failures, 0 errors, 0 skipped.
- Full core-api Maven suite: 1103 tests, 0 failures, 0 errors, 3 skipped.

## Remaining Gaps

- Persistent database tables for import batch history can be added behind
  `ImportBatchRepository` when the product needs operator-facing batch history.
- Provider-specific external connector jobs remain Task 49 scope.
- Operator UI and retry/repair workflows remain Task 56 scope.
- Legal export packages and broad customer reporting remain Task 57 scope.
