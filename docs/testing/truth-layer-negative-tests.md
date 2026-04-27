# Truth Layer Negative Test Plan

## Scope

This document defines negative tests for the Task 2A truth-layer design. It is planning only. It does not add test files, fixtures, fake demo data, migrations, services, or UI.

## Test Principles

- Test the server boundary as hostile, not cooperative.
- Treat AI output as untrusted input.
- Treat Client role responses as privacy-critical.
- Verify every protected state transition creates WorkflowEvent.
- Verify every AI-assisted transition creates AITaskRun.
- Verify bulk approval cannot become verified truth.
- Verify cross-organization access fails even when ids are known.

## Contract Validation Negative Tests

| Area | Negative case | Expected result |
| --- | --- | --- |
| Organization | Create truth-layer record without `organization_id` | Reject at schema/service boundary |
| UserAccount | Duplicate active email inside organization | Reject or require explicit merge/invite handling |
| RoleAssignment | Client-supplied role in request body attempts elevation | Ignore request role and reject unauthorized action |
| Candidate | Client role requests raw Candidate object by id | 403 or 404 with no identity clue |
| CandidateProfile | Publish canonical profile without source claim refs for material fields | Reject |
| CandidateDocument | Quarantined document used as evidence | Reject |
| CandidateEvidenceItem | Evidence lacks source reference | Reject |
| Company | Client contact reads another company's job/shortlist | Reject |
| CompanyContact | Inactive contact requests unlock | Reject and audit |
| Job | Activate job without scorecard/commercial gate | Reject |
| JobRequirement | Removed requirement included in active scorecard | Reject |
| JobScorecard | Active scorecard references stale ontology without warning | Reject or mark stale and cap confidence |
| SourceItem | Source from one org attached to another org packet | Reject |
| InformationPacket | Rejected packet publishes canonical entity | Reject |
| ClaimLedgerItem | `weak_signal` intent writes `interested_confirmed` | Reject |
| ReviewEvent | Bulk approve sets `external_verified` | Reject |
| WorkflowEvent | Key state transition occurs without event | Fail transaction |
| ConsentRecord | Consent confirmed without profile version | Reject |
| DisclosureRecord | Identity disclosure without record created first | Reject |
| MatchReport | Score 5 with cold industry pack | Reject or cap at 3 |
| Shortlist | Send shortlist with unapproved card | Reject |
| ShortlistCandidateCard | L2 card contains exact identity fingerprint | Reject and require redaction |
| AITaskDefinition | Active task missing input/output schema versions | Reject activation |
| AITaskRun | AI-assisted write lacks task run record | Reject write |
| AuditLog | Sensitive read occurs without audit log | Fail test and block release |
| PriorContactClaim | Accepted blocking prior contact ignored during disclosure | Reject disclosure |
| PriorApplicationClaim | Blocking prior application ignored during shortlist send | Reject send |
| IndustryPack | Deprecated pack selected for new job without warning | Reject or require explicit override |
| OntologyVersion | MatchReport generated without ontology version | Reject |
| SkillConcept | Active concept has no definition | Reject activation |

## Canonical Write-Back Gate Tests

| Case | Expected result |
| --- | --- |
| AI output tries to update CandidateProfile field directly | Block; create or require ClaimLedgerItem |
| Claim has `system_inference` status and target field is client-visible | Block |
| Claim has `conflicting` status and target field has existing canonical value | Block until conflict review |
| T2 field lacks source span | Block |
| T3 client-visible summary is bulk approved | Block |
| T4 unlock/disclosure lacks second approval reason | Block |
| Candidate answer tries to write canonical profile without consultant confirmation | Block |
| Consultant overwrites candidate-confirmed field without reason | Block and audit |
| AI attempts to mutate consent, disclosure, commercial, placement, or commission state | Block |
| Canonical write lacks before/after state or entity version | Block |

## WorkflowEvent Negative Tests

Every listed transition must fail if no WorkflowEvent is created in the same transaction:

- Candidate status changes.
- Job activation, pause, close, or cancel.
- Shortlist ready, send, view, feedback, select, unlock, or close.
- Consent requested, viewed, confirmed, declined, expired, or revoked.
- Disclosure requested, approved, disclosed, denied, or fee protection activated.
- Prior contact/application claim decision.
- MatchReport approval, block, or supersession.
- T3/T4 ReviewEvent decision that changes product state.

Additional failures to test:

- WorkflowEvent can be updated destructively.
- WorkflowEvent can be deleted.
- Client-visible timeline includes raw candidate identity in reason/source fields.
- AI-assisted event lacks `ai_task_run_id`.
- Idempotent command creates duplicate events for the same idempotency key.

## AITaskRun Negative Tests

| Case | Expected result |
| --- | --- |
| AI task runs without active AITaskDefinition | Reject |
| AITaskRun lacks input or output schema version | Reject |
| AITaskRun success contains invalid output schema | Mark failed and block write-back |
| AITaskRun includes provider secret in stored metadata | Fail security test |
| AITaskRun writes canonical field without ReviewEvent | Block |
| AITaskRun tries to send sensitive follow-up to client or candidate without policy | Block |
| AITaskRun output creates MatchReport explanation without evidence ids | Block or mark report needs review |
| AITaskRun model error returns raw prompt/source text to client | Reject response projection |

## Consent and Disclosure Gate Tests

| Gate | Negative case | Expected result |
| --- | --- | --- |
| Consent | Consent status is not `consent_confirmed` | Disclosure blocked |
| Profile version | Consent references older CandidateProfile version | Disclosure blocked until re-consent or approved compatible version |
| Job | Job is not activated | Disclosure blocked |
| Commercial | Fee/commercial protection is missing | Disclosure blocked |
| Client request | No client unlock request | Disclosure blocked |
| Consultant approval | No consultant approval | Disclosure blocked |
| Prior contact | Accepted PriorContactClaim blocks disclosure | Disclosure blocked |
| Prior application | Accepted blocking PriorApplicationClaim exists | Shortlist/disclosure blocked according to decision |
| DisclosureRecord | Record missing before identity DTO generation | Identity DTO blocked |
| Revocation | Consent revoked after shortlist send | Future disclosure blocked and card marked stale |

## Raw Candidate Leakage Prevention Tests

Run these tests against every Client role endpoint, export, and timeline DTO once implemented:

1. Response does not contain raw candidate identifiers before L4 disclosure.
2. Response does not contain direct contact fields.
3. Response does not contain document storage refs, source spans, claim ids, or raw evidence text.
4. Response does not contain exact employer/project/timeline fingerprints when redaction level is L0, L1, or L2.
5. Response does not include internal notes, compensation bottom line, motivation details, or do-not-contact reasons.
6. Response errors do not distinguish "candidate exists but forbidden" from "not found" in a way that enables probing.
7. Client-safe export columns match ClientSafe DTO fields only.
8. Workflow timeline projection redacts sensitive reason/source fields.
9. AI explanation fields are generated from redacted evidence, not raw evidence.
10. Snapshot tests scan both keys and values for forbidden patterns.

Forbidden pre-disclosure patterns should include:

- `candidate_id`
- `candidate_profile_id`
- `real_name`
- `email`
- `phone`
- `wechat`
- `linkedin`
- `resume`
- `document_url`
- `storage_ref`
- `source_span`
- `claim_ledger_item_id`
- `internal_note`
- `compensation_bottom_line`

## Cross-Organization Isolation Tests

| Case | Expected result |
| --- | --- |
| Valid user from Org A requests Candidate id from Org B | Reject with no data |
| Org A SourceItem attached to Org B InformationPacket | Reject |
| Org A user assigned role scoped to Org B company | Reject |
| Org A client contact requests unlock on Org B shortlist card | Reject and audit |
| AITaskRun from Org A references target entity in Org B | Reject |
| Search endpoint returns count including inaccessible org data | Reject design or filter before count |

## Review Quality and Bulk Approval Tests

| Case | Expected result |
| --- | --- |
| Bulk approve attempts T3/T4 fields | Reject |
| Bulk approve produces `candidate_confirmed` | Reject |
| Bulk approve produces `external_verified` | Reject |
| High-risk review lacks source span | Reject |
| ReviewEvent duration suggests impossible high-risk review speed | Mark for sample audit |
| Review failed sample audit but canonical field remains verified | Revert through reviewed correction or downgrade status |

## Industry Pack and Ontology Tests

| Case | Expected result |
| --- | --- |
| Cold IndustryPack generates score 5 | Cap at 3 |
| Seeded IndustryPack generates score 5 without external high-trust evidence | Cap below 5 |
| OntologyVersion is stale and MatchReport confidence is high | Reject or downgrade confidence |
| SkillConcept is deprecated but used in new scorecard without replacement warning | Reject or warn and require review |
| MatchReport lacks `evidence_coverage` | Reject |
| Match explanation references only CV keywords for core skill | Cap score at 3 |

## Release Gate for First Production Slice

Before implementing client-facing candidate endpoints, the future codebase should have passing tests for:

- Cross-organization isolation.
- Raw Candidate leakage prevention.
- Claim-to-canonical write-back blocking.
- WorkflowEvent required transitions.
- AITaskRun required recording.
- Consent/disclosure blocking rules.
- Bulk approve downgrade rules.
- Industry pack score caps.

