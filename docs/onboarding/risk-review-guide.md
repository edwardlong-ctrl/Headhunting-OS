# Risk Review Guide

## Goal

Give owners, admins, security reviewers, and implementation leads a repeatable
pilot risk review before go-live, high-risk disclosure, or real customer data
import.

## Review Inputs

- Customer onboarding checklist.
- Admin setup notes.
- Data import decision and mapping.
- Consultant training results.
- Client training results.
- Candidate consent FAQ review.
- Integration status.
- Task dependency log.
- Pilot readiness checklist.
- Security docs for client-safe data and Task 41 hardening.

## Decision Levels

| Level | Meaning | Action |
| --- | --- | --- |
| Green | Risk is controlled for pilot scope | Proceed and monitor |
| Yellow | Risk is acceptable only with named mitigation | Proceed only after mitigation owner and date are recorded |
| Red | Risk blocks onboarding or launch | Do not proceed until resolved or formally descoped |

## Risk Areas

### 1. Client-Safe Privacy

Review:

- Client users cannot receive raw Candidate objects before disclosure.
- Anonymous card output excludes identity clues.
- Exact employer, exact project, exact dates, rare public artifacts, patents,
  papers, and unique achievements are generalized or blocked before consent.
- Errors, exports, timelines, and AI explanations do not leak raw candidate
  data.

Red conditions:

- Any client route, export, error, or training flow exposes raw candidate
  identity before approved disclosure.
- Shortlist cards can be sent with unresolved high re-identification risk.

### 2. Consent and Disclosure

Review:

- Consent request binds opportunity, profile version, consent text version, and
  shared fields.
- L3 consented detail is not treated as L4 identity disclosure.
- L4 requires valid consent, consultant approval, privacy review, and
  DisclosureRecord evidence.
- Revoked, expired, missing, or mismatched consent blocks disclosure.

Red conditions:

- Client can get identity by role alone.
- Consultant can approve disclosure without consent or required review.
- Candidate-facing wording promises broader or narrower sharing than the
  product records.

### 3. Prior Contact, Prior Application, and Fee Protection

Review:

- Prior-contact and prior-application claims are captured and reviewed before
  disclosure.
- Fee/commercial preconditions are checked before unlock where required.
- Customer understands Task 48 commercial depth may still be pending in this
  branch.

Red conditions:

- The pilot requires fee protection or accounting workflow behavior not present
  in the current baseline.
- Prior-contact or prior-application risk is ignored to accelerate disclosure.

### 4. Data Import

Review:

- Source ownership and approval are clear.
- Candidate personal data is approved or excluded.
- Import path creates source records/claims first.
- Duplicate, conflict, and stale-field review is available for the dataset.
- Task 55 dependency is recorded for unsupported historical import.

Red conditions:

- Import requires direct database edits.
- Import maps raw input directly into confirmed facts without review.
- Validation report cannot prove privacy and organization scope.

### 5. AI Governance

Review:

- AI outputs are claims, suggestions, or audited task outputs.
- Prompt/schema/task versions are visible where supported.
- Model routing is known for pilot tasks.
- AI cannot send shortlist, reject candidates, disclose identity, unlock
  contact information, or make commercial promises.

Red conditions:

- Training or configuration treats AI output as confirmed fact.
- AI route or integration bypasses human review and backend write gates.

### 6. Access and Organization Boundaries

Review:

- User roster matches pilot scope.
- Client users are company/job scoped.
- Candidate users are self-scoped.
- Admin is not used as a workaround for missing workflow.
- Multi-organization boundary hardening expectations are recorded as Task 51
  dependency where needed.

Red conditions:

- Shared accounts are used for pilot operations.
- Client, candidate, or consultant can access another organization's data.

### 7. Security and Compliance

Review:

- Task 41 controlled-pilot controls are enabled where applicable.
- Production security claims are not made before Task 52 evidence.
- Vulnerability, pen-test, privacy-retention, key rotation, MFA, SSO, and full
  access-review gaps are recorded when in scope.

Red conditions:

- Customer requires production security certification that is not available.
- Sensitive export or document access lacks the required permission/audit path.

### 8. Integrations, Support, DR, Performance, and Reporting

Review:

- Integration-dependent messages are configured, deferred, or manually handled.
- Support path avoids direct database fixes.
- Backup/restore and incident expectations are appropriate for controlled
  pilot scope.
- Performance and AI cost expectations are pilot-sized.
- Reports and legal audit packages expected by customer match current product
  evidence or are recorded as Task 57 dependency.

Red conditions:

- Launch depends on unconfigured email/SMS/calendar/OCR/STT/ATS behavior.
- Customer requires production DR, performance, support tooling, or legal audit
  exports that are not available in the current branch.

## Risk Review Output

Record:

- Date and reviewers.
- Customer organization and pilot scope.
- Green/yellow/red decision by risk area.
- Required mitigations and owners.
- Dependencies on Tasks 48, 49, 51, 52, 53, 54, 55, 56, 57, or 58.
- Launch decision: proceed, proceed with mitigations, defer, or block.

## Minimum Go-Live Standard

Do not launch a pilot unless:

- No red risks remain.
- Yellow risks have owners and written mitigations.
- Client-safe privacy and consent/disclosure risks are green.
- Data import is approved, deferred, or out of pilot scope.
- Integration gaps have an approved manual or deferred path.
- Support and escalation path is known to all trained users.

