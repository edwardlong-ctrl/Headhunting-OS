# RC1 Capability Map

## Classification

- `implemented`: Source and current verification prove the capability works.
- `partial`: Some behavior exists, but a transaction step or gate is incomplete.
- `scaffold`: Code/docs/routes exist, but no current proof shows the capability works.
- `no-op`: Provider intentionally records or simulates behavior without external action.
- `not-verified`: No current RC1 evidence has been collected.

## Transaction Capability Map

| Capability | Golden Path Step | User Surface | Backend Entry Points | Persistence / Tables | Workflow / Audit | AI Task / Provider | Current Evidence | Status | Next Action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Candidate intake | Consultant candidate intake | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| AI draft claims | AI draft claims | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Claim Ledger governance | AI draft claims / human review / canonical write | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map source span, assertion strength, speaker, verification status, canonical-write permission, client shareability, AITaskRun, and review/audit linkage |
| Human review | Human review | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Canonical/truth write | Canonical/truth write | not-verified | not-verified | `recruiting.candidate`, `recruiting.candidate_profile`, `governance.claim_ledger_item`, `recruiting.profile_field_lineage`, and `governance.canonical_write_attempt` applied by RC1-03 migrations | `workflow.workflow_event` exists; `governance.canonical_write_attempt.workflow_event_id` exists; runtime event emission not verified | `governance.ai_task_run` and ClaimLedger links exist; provider behavior not verified | RC1-03 `release:migrations` exit 0; V2/V10/V11 schema evidence only | partial | Run RC1-03A/RC1-05/RC1-09 to verify transaction data, service behavior, and audit correlation |
| Job intake | Client/job intake | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Matching | Match report | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| MatchReport v2.1 governance | Match report | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map score confidence, evidence coverage, provenance weighting, authenticity risk, score caps, and ontology version |
| Shortlist | Anonymous shortlist | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Re-identification risk | Anonymous shortlist / client-safe card | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map assessment, redaction level, allowed/blocked result, and WorkflowEvent |
| Consent | Candidate consent | not-verified | not-verified | `privacy.consent_record` applied with candidate/profile/job refs, `profile_version`, `consent_text_version`, status, permitted disclosure levels, expiry/revocation fields, and workflow entity id | Workflow entity id exists; consent WorkflowEvent creation not verified | not-verified | RC1-03 `release:migrations` exit 0; V8/V20 schema evidence only | partial | Run RC1-03A/RC1-06/RC1-09 to verify candidate consent transaction behavior and audit trace |
| Unlock/disclosure | Client unlock request and consultant approval | not-verified | not-verified | `privacy.client_unlock_request`, `privacy.unlock_decision`, and `privacy.disclosure_record` applied; disclosure links to consent/unlock refs and has `workflow_event_id`; unlock requests have workflow entity id | Disclosure workflow event reference and unlock workflow entity id exist; approval flow/audit emission not verified | not-verified | RC1-03 `release:migrations` exit 0; V8/V20/V26/V27 schema evidence only | partial | Run RC1-06/RC1-06B/RC1-09 to verify unlock/disclosure gates, privacy boundary, and audit correlation |
| Feedback | Interview feedback | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Map during RC1 gate execution |
| Placement | Placement | not-verified | not-verified | `recruiting.placement` applied with organization, job, candidate, company, offer/onboarding/guarantee statuses, offer fields, and uniqueness/indexes | Placement WorkflowEvent emission not verified | not-verified | RC1-03 `release:migrations` exit 0; V10 schema evidence only | partial | Run RC1-08A/RC1-09 to verify runtime placement action, persistence, and audit correlation |
| Commission | Commission/revenue | not-verified | not-verified | `recruiting.commission` applied with placement/consultant links, status, commission type, amount/currency/split fields, and indexes; V33 adds same-organization consultant constraint | Commission WorkflowEvent/audit emission not verified | not-verified | RC1-03 `release:migrations` exit 0; V10/V33 schema evidence only | partial | Run RC1-08A/RC1-09 to verify runtime commission calculation/payment evidence and audit correlation |
| Owner commercial/accounting proof | Owner revenue / accounting handoff | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Correlate to placement and commission IDs |
| Admin governance/audit/replay proof | Admin governance / audit trace | not-mapped | not-mapped | not-mapped | not-mapped | not-mapped | not-verified | not-verified | Correlate to transaction ledger and WorkflowEvent IDs |
| Governance/audit | End-to-end trace | not-verified | not-verified | `governance.claim_ledger_item`, `governance.review_event`, `governance.canonical_write_attempt`, `workflow.workflow_event`, and `audit.audit_log` applied | WorkflowEvent and audit_log tables exist with idempotency/correlation/causation and same-organization actor constraints; transaction trace not verified | `governance.ai_task_run` exists; AI governance runtime not verified | RC1-03 `release:migrations` exit 0; V2/V3/V11/V33 schema evidence only | partial | Run RC1-05/RC1-06/RC1-09/RC1-10 to verify runtime event creation, audit replay, and trace correlation |

## AI Agent Readiness Notes

| Candidate Agent | Current Subsystem | Evidence Source | What It Can Do Today | What Is Missing Before Agentization | Status |
| --- | --- | --- | --- | --- | --- |
| Intake agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
| Matching agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
| Consent/disclosure guard agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
| Workflow follow-up agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
| Governance/audit agent | not-mapped | not-verified | not-verified | not-verified | not-verified |
