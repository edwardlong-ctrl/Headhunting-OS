# Task 57 Reporting, Exports, and Legal Audit Packages

Date: 2026-05-10

Branch: `codex/task-57-reporting-exports`

## Scope

Task 57 adds a backend-owned reporting/export package boundary for business,
client, candidate, commercial, retention, and legal-audit exports.

Implemented behavior:

- `reportingexport` request/result model with organization id, actor role,
  target scope, export type, field visibility policy, and audit id.
- Fail-closed export authorization before adapter lookup, so cross-org or
  role-ineligible requests do not reveal target record existence.
- Field-level export filtering that drops adapter-supplied fields outside the
  requested visibility policy and records the withheld field names.
- Owner report adapter over the existing Owner revenue and placement query
  services.
- Consultant activity adapter over existing observability workflow/review event
  read services.
- Client shortlist/feedback adapter that projects candidate cards through the
  existing client-safe projection service before export packaging.
- Candidate personal-data adapter boundary for self-scoped profile, document,
  consent, and status sections.
- Disclosure legal-audit adapter over the existing observability disclosure
  audit export.
- Placement/commission export adapter over the existing read-only accounting
  handoff.
- Retention/delete evidence adapter boundary that packages lifecycle evidence
  without executing destructive deletion.
- `ReportingExportResult` is an API-safe, whitelisted response DTO.

## Policy Boundary

The Task 57 service is a policy facade, not a new source of truth. It composes
existing Task 30, Task 33, Task 40, Task 48, and Task 51 surfaces through
adapters:

- client-facing candidate data must pass through client-safe projection and
  redaction policy;
- disclosure packages are derived from existing disclosure audit provenance;
- placement/commission packages retain Task 48 read-only accounting handoff
  semantics;
- tenant scope is checked before adapter lookup;
- retention packages document eligibility and evidence only.

## Out of Scope

- No Task 49 external integration/export delivery to third-party systems.
- No Task 55 import/migration workflow.
- No Task 56 support mutation/retry tooling.
- No physical deletion executor or silent destructive deletion.
- No raw candidate data in client-facing exports before unlock/disclosure.
- No frontend UI changes.

## Tests Added

- `ReportingExportServicePolicyTest`
  - cross-org exports fail closed before adapter lookup;
  - role-ineligible exports do not reveal target refs;
  - client shortlist exports withhold raw undisclosed candidate fields;
  - candidate personal-data exports require self candidate scope and same org;
  - placement/commission export remains read-only accounting handoff;
  - disclosure legal package includes audit/provenance evidence;
  - retention evidence package does not silently delete.
- `ReportingExportSafeDtoTest`
  - reporting export DTO is `ApiSafeResponseBody` and field-whitelisted.

## Validation Evidence

Focused Task 57 TDD red evidence:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=ReportingExportServicePolicyTest,ReportingExportSafeDtoTest test
```

Result: failed at compile because the new Task 57 package and whitelist did not
exist yet.

Focused Task 57 green evidence:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=ReportingExportServicePolicyTest,ReportingExportSafeDtoTest test
```

Result: 8 tests, 0 failures, 0 errors, 0 skipped.

The full validation evidence for branch closeout is recorded in the final
handoff response after the required verification commands complete.
