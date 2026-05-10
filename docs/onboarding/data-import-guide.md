# Data Import Guide

## Goal

Define the customer onboarding path for historical recruiting data and pilot
seed data without bypassing truth, privacy, consent, audit, or workflow gates.

## Current Boundary

Task 38 provides deterministic controlled-pilot seed/import/reset/export
commands for synthetic data. Real customer historical import and migration are
Task 55 scope and must not be described as complete unless the current branch
contains that implementation and evidence.

Until Task 55-compatible tooling is available for the requested source type,
real customer data import must be approved, constrained, and either deferred or
handled through an explicitly governed path. Direct database edits are not an
onboarding method.

## Import Principles

- Raw inputs become `SourceItem` / `InformationPacket` / claims first, not
  confirmed facts.
- Canonical candidate, company, job, consent, disclosure, placement, and
  commission states must be written through domain services and gates.
- Candidate personal data must remain organization-scoped and role-scoped.
- Client users must not receive raw Candidate objects through import output,
  validation reports, exports, errors, or debug metadata.
- Duplicate detection and conflict review must run before imported information
  becomes operationally trusted.
- Import rollback/reset must preserve required audit records and legal hold
  evidence where applicable.

## Supported Dry-Run Path

For synthetic pilot validation:

```sh
rtk npm run pilot:data:rebuild
rtk npm run pilot:data:validate
rtk npm run pilot:data:export
RTO_PILOT_DATA_ALLOW_RESET=true rtk npm run pilot:data:reset
```

Use this dry-run path to train operators before touching customer data.

## Customer Data Intake

Collect a source inventory before import approval.

| Source type | Examples | Required decision |
| --- | --- | --- |
| Candidate documents | CV, resume, portfolio summary, LinkedIn text | Is personal data approved for pilot import? |
| Candidate notes | Call notes, WeChat notes, availability, compensation notes | Which fields are sensitive or internal-only? |
| Company data | Company profile, contacts, preferences | Which client users can view or edit it? |
| Job data | JD files, scorecards, must-have requirements | Which jobs are in pilot scope? |
| Feedback | Interview notes, client feedback, rejection reasons | Which feedback can become claims? |
| Commercial data | Fee agreements, invoice, guarantee, commission | Does this fit the Task 48 read-only accounting handoff scope, or does it require external accounting integration? |
| ATS/CRM export | CSV, spreadsheets, API export | Does Task 55 mapping exist for this source? |

## Pre-Import Approval

The data owner and security approver must sign off on:

- Organization scope.
- Data classes and sensitivity.
- Source system owner.
- Allowed file types and size expectations.
- Candidate consent or notice expectations.
- Fields excluded from pilot import.
- Duplicate detection rules.
- Conflict review owner.
- Import validation report format.
- Rollback or reset plan.
- Whether the import depends on Task 55 work.

## Mapping Rules

| Incoming field | Governed target |
| --- | --- |
| Candidate name, contact, identity fields | Candidate-private fields; never client pre-disclosure output |
| Resume or document text | Source document and parsed evidence, then claims |
| Skills, projects, timeline | Claims with source/provenance; canonical only after review |
| Compensation, motivation, do-not-contact | Sensitive candidate-private data with strict role visibility |
| Company preference | Company preference or claim, depending on source strength |
| Job requirement | Job intake claim or scorecard field after consultant/client review |
| Interview feedback | Feedback record and suggested update for human review |
| Prior contact/application | Risk claim requiring review before disclosure |
| Fee agreement, invoice, commission | Commercial workflow only inside the Task 48 fee agreement, invoice readiness, commission input, and read-only accounting export scope |

## Dry-Run Import Review

Before importing real data, run a dry-run using representative synthetic or
sanitized records.

Dry-run review must answer:

- How many records were accepted, rejected, duplicated, or held for review?
- Which fields were excluded for privacy or unsupported mapping?
- Which records produced conflicts or stale-field flags?
- Which candidate records could create re-identification risk if sent to a
  client?
- Which records require consultant follow-up or candidate confirmation?
- Which records depend on integration, OCR/STT, or ATS/HRIS work from Task 49
  or Task 55?

## Import Execution

1. Freeze the approved source dataset.
2. Record checksum or source export identifier where possible.
3. Confirm the target organization and role owner.
4. Run the approved import path.
5. Run validation.
6. Review import report with data owner and security approver.
7. Resolve duplicates, conflicts, and excluded fields.
8. Only then train consultants on the imported operational records.

## Validation Requirements

At minimum, validation must prove:

- Organization scope is correct.
- No client-visible output contains raw candidate identity pre-disclosure.
- No canonical facts were created from AI or import output without the required
  review and gate.
- Duplicate and conflict reports are available to operators.
- Import errors do not expose raw PII to unauthorized roles.
- Reset/rollback decision is documented.

## Import Stop Conditions

Stop the import if:

- Source ownership or data approval is unclear.
- Candidate personal data is present but not approved.
- Mapping requires unsupported direct canonical writes.
- Import output would expose raw candidate data to Client role.
- Duplicate/conflict results are unavailable for the dataset.
- Rollback/reset behavior is undefined for the import type.
- The source depends on Task 55 mapping that is not present in the current
  branch.
