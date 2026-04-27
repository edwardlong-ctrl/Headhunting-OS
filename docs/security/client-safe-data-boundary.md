# Client-Safe Data Boundary

## Scope

This document defines the privacy and DTO boundary that prevents raw Candidate leakage before consent and disclosure gates. It is a design artifact only and does not implement endpoints, serializers, UI, or tests.

## Core Rule

Client users must never receive raw Candidate objects before unlock/disclosure. This includes direct fields, nested objects, ids, source spans, event reasons, AI explanations, errors, exports, and debug metadata.

## Raw Candidate Means

Raw Candidate data includes:

- Candidate id or stable person id.
- Real name, contact details, personal email, phone, WeChat, LinkedIn URL, portfolio URL, or resume file.
- Exact current or prior employer when it can identify the candidate.
- Exact project names, rare achievements, precise timelines, patents, papers, publications, or team details that enable re-identification.
- Compensation bottom line, sensitive motivation, do-not-contact reason, internal notes, consultant notes, and other client/company interactions.
- SourceItem, CandidateDocument, raw source spans, claim ledger internals, AI prompts, and unredacted evidence.
- Any field marked `internal_only`, `consent_required`, or `forbidden`.

## DTO Taxonomy

| DTO | Allowed roles | Purpose | Candidate data rule |
| --- | --- | --- | --- |
| `CandidateInternalDTO` | Consultant, Admin under policy | Full operational candidate view | Never returned to Client role |
| `CandidateOwnerRiskDTO` | Owner, Admin under policy | Business and risk summary | Redacts personal data unless policy allows |
| `CandidateSelfDTO` | Candidate self | Candidate-owned profile, confirmations, consent surfaces | Only self-scoped records |
| `ClientSafeCandidateCardDTO` | Client | Pre-disclosure shortlist card | Uses alias and redacted evidence only |
| `ClientSafeMatchReportDTO` | Client | Match explanation before disclosure | No raw evidence, no identity clues |
| `ClientDisclosureDTO` | Client after L4 | Identity/contact fields approved by DisclosureRecord | Only after valid DisclosureRecord |
| `WorkflowTimelineDTO` | Role-filtered | Timeline display | Redacts reason/source fields by role |
| `AITaskRunSummaryDTO` | Owner/Admin/Consultant under policy | AI observability | Redacts prompts/source text/secrets |

Frontend code should never branch from raw Candidate fields to create client-safe output. Client-safe output must be generated server-side.

## Redaction Levels

| Level | Client-visible scope | Gate |
| --- | --- | --- |
| `L0_TEASER` | Role family, broad years range, broad capability direction | Candidate not yet consented or early client scan |
| `L1_GENERALIZED` | Generalized company type and generalized project area | Low identity risk and internal approval |
| `L2_CLIENT_SAFE` | Default shortlist detail with redacted evidence and risks | Re-identification risk scorer passed |
| `L3_CONSENTED_DETAIL` | More specific project and experience details | Candidate consent for this job/profile version |
| `L4_IDENTITY_DISCLOSED` | Real identity/contact/complete profile subset | DisclosureRecord exists and gates passed |

## Pre-Disclosure Client Allow List

Client-safe pre-disclosure DTOs may contain:

- `client_alias`, such as "Candidate A" or another scoped alias.
- Broad seniority and years range.
- Generalized location and availability.
- Skill categories and role-family concepts.
- Evidence coverage percentage and score confidence.
- Match score only with score cap reason and client-safe explanation.
- Redacted strengths, risks, and follow-up questions.
- Redaction level and whether more detail requires consent/unlock.

## Pre-Disclosure Client Deny List

Client-safe pre-disclosure DTOs must not contain:

- `candidate_id`, `candidate_profile_id`, raw `match_report_id` if it can be used to query candidate internals.
- Name, contact, direct social/profile URLs, document URLs, resume text, or raw source excerpts.
- Exact employer, exact team, exact project, exact dates, rare achievement fingerprints, or unique public artifact references.
- Compensation bottom line, motivation details, sensitive constraints, internal notes, or consultant-private reasoning.
- ClaimLedgerItem ids, SourceItem ids, CandidateDocument ids, AITaskRun raw output, raw model prompts, or tool call payloads.
- Other client interactions, prior application details, prior contact details, or fee protection internal notes.
- Stack traces, validation errors, or export columns containing raw field names and values.

## API Boundary Rules

1. Role and organization scope come only from server-side authentication and RoleAssignment, never from request body fields.
2. Client endpoints must return DTOs from a client-safe projection layer, not domain entities.
3. No endpoint accepts `includeRaw`, `debug`, `withClaims`, `withDocuments`, or similar client-controlled expansion for Client role.
4. Pre-disclosure client URLs should use `shortlist_id` and `shortlist_candidate_card_id`, not Candidate ids.
5. Server errors must use generic messages when the target is candidate-sensitive.
6. Export endpoints must use the same DTO serializers and leakage scanner as JSON endpoints.
7. GraphQL-style nested selection, if ever introduced, must deny raw Candidate fields by role at resolver level.

## Client-Safe Generation Pipeline

The safe path is:

1. MatchReport is generated from CandidateProfile, JobScorecard, evidence, score caps, and ontology version.
2. ShortlistCandidateCard is generated from MatchReport with a redaction level.
3. Re-identification risk scorer identifies unsafe features.
4. Client-safe summary generator removes or generalizes unsafe features.
5. ReviewEvent records consultant approval for T3 client-visible content.
6. WorkflowEvent records shortlist state transition.
7. ClientSafeCandidateCardDTO is serialized from the approved ShortlistCandidateCard projection.

Any failure in steps 2 through 5 must block sending the card to the client.

## Consent and Disclosure DTO Rules

ConsentRecord must include the CandidateProfile version and consent text version. DisclosureRecord must exist before L4 identity/contact fields leave the server.

L4 disclosure requires:

- ConsentRecord status is `consent_confirmed`.
- ConsentRecord profile version equals the profile version being disclosed.
- Job status is compatible with disclosure.
- Fee/commercial protection gate is active.
- Client requested unlock.
- Consultant approved.
- No accepted blocking PriorContactClaim or PriorApplicationClaim applies.
- DisclosureRecord is created and linked to the shortlist card or job/candidate pair.

## Workflow and Audit Redaction

Client-visible timelines must not leak protected data through:

- `reason`
- `before_state` or `after_state`
- actor names for internal users when unnecessary
- source refs
- AI task summaries
- prior contact/application notes
- denial reasons

Use role-specific timeline DTOs. Internal WorkflowEvent remains append-only; DTO projection redacts.

## Raw Leakage Prevention Tests

Client-safe boundary tests must scan:

- JSON keys and values.
- Nested arrays and maps.
- Error responses.
- Export files.
- Timeline/event DTOs.
- AI explanation fields.
- Redacted evidence text.
- Source and document references.

Forbidden key patterns include:

- `candidate_id`
- `candidate_profile_id`
- `real_name`
- `email`
- `phone`
- `wechat`
- `linkedin`
- `document_url`
- `storage_ref`
- `source_span`
- `claim_ledger_item_id`
- `internal_note`
- `compensation_bottom_line`

The exact list should live in contract tests and be extended when new sensitive fields are added.

## Review Requirements

Any client-visible candidate statement at T3 risk requires ReviewEvent. Bulk approval cannot approve client-visible high-risk content. L2 or higher summaries must record redaction level, risk score, unsafe features removed, reviewer, and reason.

