# Truth Layer Service Boundary

## Current Boundary Chain

Governed intake foundation:

`GovernedIntakeService`
→ `SourceItemPersistencePort`
→ `InformationPacketPersistencePort`
→ `intake.source_item` / `intake.information_packet` / `intake.information_packet_source_item`

Governed intake extraction placeholder:

`DeterministicIntakeExtractionService`
→ `InformationPacketPersistencePort`
→ `IntakeExtractionRunPort`
→ `intake.extraction_run`

Governed intake ClaimLedger bridge:

`IntakeClaimLedgerBridgeService`
→ `IntakeExtractionRunPort`
→ `InformationPacketPersistencePort`
→ `ClaimLedgerSourceReferenceLookupPort`
→ `ClaimLedgerService`
→ `governance.claim_ledger_item`

Governed intake ReviewEvent bridge:

`IntakeReviewBridgeService`
→ `ClaimLedgerItemReviewLookupPort`
→ `ReviewEventSourceReferenceLookupPort`
→ `ReviewEventService`
→ `governance.review_event`

Governed intake CanonicalWrite boundary bridge:

`IntakeCanonicalWriteBridgeService`
→ `ClaimLedgerItemCanonicalWriteLookupPort`
→ `ReviewEventCanonicalWriteLookupPort`
→ `CanonicalWriteService`
→ `CanonicalWriteGate`
→ `WorkflowEventService` when the existing boundary is allowed/audited

Claim/review/canonical chain:

`ClaimLedgerService`
→ `ReviewEventService`
→ `CanonicalWriteGate`
→ `CanonicalWriteService`
→ `WorkflowEventService`

Transition audit side:

`WorkflowTransitionAuditService`
→ `WorkflowEventService`

Read-only audit side:

`WorkflowAuditQueryService`
→ `WorkflowAuditReadPort`
→ `workflow.workflow_event`

CandidateProfile backend-internal persistence side:

`CandidateProfile` / `CandidateProfileField`
→ `CandidateProfileService`
→ `CandidateProfilePersistencePort`
→ `JdbcCandidateProfilePersistencePort`
→ `recruiting.candidate_profile`

Task 5F regression closure covers the governed-intake minimal slice as a safe
backend-only chain:

`SourceItem` / `InformationPacket`
→ deterministic extraction output envelope
→ `ClaimLedgerItem` claim
→ `ReviewEvent` evidence
→ `CanonicalWriteService` boundary attempt
→ `CanonicalWriteGate` decision
→ no canonical persistence

This closure does not add real AI, CandidateProfile persistence, raw
Candidate/Profile persistence, API/UI exposure, client-safe projection,
Consent/Disclosure behavior, RBAC/ABAC, workflow engine behavior, transition
legality validation, or cleanup/deprecation of earlier V2 `recruiting.*`
source/packet skeleton tables.

Task 6A adds the CandidateProfile canonical contract vocabulary. Task 6B adds
backend-internal CandidateProfile persistence skeletons only. Task 6C hardens
the canonical write transaction boundary with real Spring/JDBC transaction
coordination and rollback tests. Task 6B/6C do not wire governed intake into
CandidateProfile, call `CanonicalWriteService` from CandidateProfile persistence,
bypass `CanonicalWriteGate`, add API/UI exposure, create client-safe projection,
implement Consent/Disclosure behavior, implement RBAC/ABAC, wire real AI
extraction, add workflow engine behavior, add transition legality validation, or
implement the real canonical write flow.

## GovernedIntakeService

- Backend-owned service boundary for Task 5A SourceItem + InformationPacket persistence.
- Registers `SourceItem` records as raw/source provenance metadata and refs.
- Creates `InformationPacket` records that group source material by intended packet/entity type.
- Attaches a SourceItem to an InformationPacket only when both records are visible in the same `organization_id`.
- Rejects duplicate SourceItem attachment to the same packet.
- Reads SourceItem and InformationPacket records only through organization-scoped lookup/list methods.
- Does not parse CV/JD/WeChat/email/call-note content.
- Does not run real AI extraction, OCR, STT, file conversion, or deterministic extraction.
- Does not append ClaimLedgerItem, create ReviewEvent, call CanonicalWriteService, or write CandidateProfile.
- Does not append WorkflowEvent in Task 5A; packet/source registration audit is intentionally deferred until a later governed-intake subtask defines suitable workflow action vocabulary and state semantics.
- Does not mutate Job, Candidate, Company, Shortlist, Consent, Disclosure, Placement, or Commission state.
- Does not expose API/controller/UI behavior.

## SourceItem / InformationPacket Persistence Ports

- `SourceItemPersistencePort` is narrow: append/register SourceItem and find SourceItem by organization-scoped id.
- `InformationPacketPersistencePort` is narrow: create InformationPacket, find by organization-scoped id, attach SourceItem, test duplicate attachment, and list SourceItems for a packet.
- `intake.source_item` stores provenance/raw-source metadata, source type, origin, actor provenance, refs/hashes, language, timestamps, metadata JSON, and status.
- `intake.information_packet` stores packet type, intended entity type/id, creator provenance, processing status, notes, timestamps, and metadata JSON.
- `intake.information_packet_source_item` is the Task 5A link table; its primary key rejects duplicate attachment and its composite foreign keys preserve organization isolation.
- These tables are not client-facing views and are not canonical fact tables.

## DeterministicIntakeExtractionService

- Backend-owned Task 5B service boundary for deterministic governed-intake extraction placeholders.
- Takes `organization_id` and `information_packet_id`, then loads the `InformationPacket` and attached `SourceItem` records through organization-scoped governed-intake ports.
- Uses only safe source metadata such as source ids, source type, title, content hash, and external reference.
- Does not inspect, parse, copy, or expose raw source payloads, raw Candidate/Profile data, `raw_ref`, `storage_ref`, or source metadata JSON contents.
- Performs no real AI extraction, no LLM call, no OCR/STT/file conversion, and no semantic parsing.
- Does not infer business facts such as salary, intent, seniority, skills, job fit, consent, identity, or candidate confirmation.
- Successful output is an `IntakeExtractionOutputEnvelope` intermediate envelope only. It is not canonical fact storage, ClaimLedger, ReviewEvent, CandidateProfile persistence, client-safe projection, or CanonicalWrite output.
- The output envelope explicitly marks `real_ai_extraction_performed=false`, `semantic_parsing_performed=false`, `claim_ledger_append_allowed=false`, `canonical_write_allowed=false`, and `needs_future_extraction=true`.
- If a packet has no attached source items, the service persists a `FAILED` extraction run with no output envelope rather than inventing data.
- Duplicate extraction is allowed for now: each request creates a new run id, while deterministic placeholder content and source snapshot hash remain stable for unchanged packet/source metadata.
- Does not update `InformationPacket.processingStatus`; governed-intake lifecycle status transitions remain future work.
- Does not append ClaimLedgerItem, create ReviewEvent, call CanonicalWriteService, append WorkflowEvent, write CandidateProfile, mutate entity state, validate workflow transitions, expose API/controller/UI behavior, or expose output to Client.

## IntakeExtractionRunPort / JdbcIntakeExtractionRunPort

- `IntakeExtractionRunPort` is narrow: save an extraction run/output, find by organization-scoped run id, and list runs for an organization-scoped information packet.
- `JdbcIntakeExtractionRunPort` persists only to `intake.extraction_run`.
- `intake.extraction_run` links to `intake.information_packet` through an organization-scoped foreign key.
- `output_json` is JSONB for the intermediate extraction envelope only; it is not a canonical profile, ClaimLedger item, ReviewEvent, or client-facing projection.
- The adapter does not write `governance.claim_ledger_item`, `governance.review_event`, `workflow.workflow_event`, `recruiting.candidate`, `recruiting.candidate_profile`, `recruiting.source_item`, or `recruiting.information_packet`.

## IntakeClaimLedgerBridgeService

- Backend-owned Task 5C service boundary for bridging governed-intake extraction output envelopes into ClaimLedger append.
- Requires an organization-scoped `IntakeClaimLedgerBridgeRequest` with `organization_id`, `extraction_run_id`, actor provenance, and bridge policy.
- Loads extraction runs through `IntakeExtractionRunPort` by organization and extraction run id.
- Rejects missing runs, wrong-organization runs, `FAILED` runs, non-succeeded runs, and runs without an output envelope.
- Validates the extraction output envelope matches the run and validates the packet through `InformationPacketPersistencePort`.
- Uses `intake.extraction_run` and `intake.information_packet` for Task 5C governed-intake lineage. It does not read or write earlier V2 `recruiting.source_item` or `recruiting.information_packet` skeleton tables.
- Treats default deterministic placeholder output as non-bridge-eligible and appends no fake business claims.
- Only maps fields explicitly marked `CLAIM_CANDIDATE` and prefixed with `intake.bridge_eligible.*`.
- Blocks placeholder metadata such as `source_count`, `source_types`, `extraction_mode`, `real_ai_extraction_performed`, `claim_ledger_append_allowed`, `canonical_write_allowed`, and `needs_future_extraction` even if malformed input marks them as claim candidates.
- Maps bridge-eligible operational fixtures to internal-only ClaimLedger claims with `claim_value_text`, `verification_status=ai_extracted`, `assertion_strength=weak_signal`, `claim_type=inference`, and `canonical_write_allowed=false`.
- Stores governed-intake lineage in deterministic `source_span_ref` values. It leaves `source_item_id` null because the current V2 ClaimLedger column references `recruiting.source_item`, not `intake.source_item`.
- Uses `ClaimLedgerSourceReferenceLookupPort` and V6 `claim_ledger_org_source_span_idx` for narrow duplicate detection; repeated equivalent bridge calls return existing claim ids instead of appending duplicates.
- Appends ClaimLedger records only through `ClaimLedgerService`.
- Does not create ReviewEvent, call CanonicalWriteService, write CandidateProfile, write raw Candidate/Profile persistence, append WorkflowEvent, mutate entity state, implement a workflow engine, validate transition legality, expose API/controller/UI behavior, expose output to Client, wire AI models, implement Consent/Disclosure, or implement RBAC/ABAC.

## ClaimLedgerSourceReferenceLookupPort / JdbcClaimLedgerSourceReferenceLookupPort

- Narrow read-only lookup used only for Task 5C bridge idempotency by exact organization and `source_span_ref`.
- Reads only `governance.claim_ledger_item`.
- Does not provide generic ClaimLedger search, dashboard analytics, API exposure, canonical read models, candidate/company/job joins, or client-safe projection.

## IntakeReviewBridgeService

- Backend-owned Task 5D service boundary for bridging governed-intake-origin ClaimLedger claims into ReviewEvent append.
- Requires an organization-scoped `IntakeReviewBridgeRequest` with `organization_id`, `claim_ledger_item_id`, reviewer actor type/id, review decision, risk tier, bulk flag, reason, and review policy.
- Loads the claim through `ClaimLedgerItemReviewLookupPort` by exact organization and claim id.
- Rejects missing claims, wrong-organization claims, unsupported policies, and claims without Task 5C governed-intake `intake.*` lineage markers.
- Current policy is governed-intake-only because Task 5C source lineage is reliable in `source_span_ref`.
- Maps the claim target entity, target field path, claim id, risk tier, decision, bulk flag, reviewer id, and reason into `ReviewEventAppendCommand`.
- Appends only through `ReviewEventService`.
- Stores claim lineage in `ReviewEventAppendCommand.claimId` and a deterministic review bridge `source_span_ref` containing the reviewed `claim_ledger_item_id`.
- Uses `ReviewEventSourceReferenceLookupPort` for narrow duplicate detection by organization and deterministic `source_span_ref`.
- Repeated identical review bridge calls return the existing review event id. Materially different review evidence creates a separate ReviewEvent.
- T3/T4 review requests require a non-AI/non-system reviewer and a reason.
- Bulk approval is recorded as review evidence only; it does not create `candidate_confirmed` or `external_verified` semantics.
- Does not mutate ClaimLedger verification status, set `claim_ledger_item.review_event_id`, call `CanonicalWriteService`, write CandidateProfile, write raw Candidate/Profile persistence, append WorkflowEvent, mutate entity state, implement a workflow engine, validate transition legality, expose API/controller/UI behavior, expose output to Client, wire AI models, implement Consent/Disclosure, or implement RBAC/ABAC.
- Does not read or write earlier V2 `recruiting.source_item` or `recruiting.information_packet` skeleton tables.

## ClaimLedgerItemReviewLookupPort / JdbcClaimLedgerItemReviewLookupPort

- Narrow read-only lookup used only for Task 5D review bridge claim loading by exact organization and claim id.
- Reads only `governance.claim_ledger_item`.
- Does not provide generic ClaimLedger search, dashboard analytics, API exposure, canonical read models, candidate/company/job joins, business target entity lookup, or client-safe projection.

## ReviewEventSourceReferenceLookupPort / JdbcReviewEventSourceReferenceLookupPort

- Narrow read-only lookup used only for Task 5D review bridge idempotency by exact organization and deterministic review bridge `source_span_ref`.
- Reads only `governance.review_event`.
- Does not provide generic review history search, dashboard analytics, API exposure, candidate/company/job joins, business target entity lookup, or client-safe projection.

## IntakeCanonicalWriteBridgeService

- Backend-owned Task 5E service boundary for bridging governed-intake-origin ClaimLedger claims plus ReviewEvent evidence into the existing CanonicalWriteService boundary attempt.
- Requires an organization-scoped `IntakeCanonicalWriteBridgeRequest` with `organization_id`, `claim_ledger_item_id`, `review_event_id`, requesting actor, target entity reference, target field path, target verification status, risk tier, reason, occurred-at time, and the governed-intake-only bridge policy.
- Loads the claim through `ClaimLedgerItemCanonicalWriteLookupPort` by exact organization and claim id.
- Loads the review evidence through `ReviewEventCanonicalWriteLookupPort` by exact organization and review event id.
- Validates the ReviewEvent belongs to the same ClaimLedgerItem and that the ReviewEvent target entity and field path match the ClaimLedgerItem reviewed target.
- Current policy is governed-intake-only because Task 5C/5D source lineage is reliable in `source_span_ref`.
- Requires Task 5C ClaimLedger lineage markers: `information_packet` target entity, `intake.bridge_eligible.*` target field path, and `intake.extraction_run`, `intake.information_packet`, and `intake.source_item` source-span markers.
- Requires Task 5D ReviewEvent lineage markers: `intake.review_bridge` and the reviewed `claim_ledger_item_id` in `source_span_ref`.
- Maps claim type, assertion strength, verification status, client shareability, review decision, bulk flag, review reason, requested target verification status, target risk tier, target entity, target field path, actor, reason, idempotency key, correlation id, causation id, and occurred-at into `CanonicalWriteCommand`.
- Calls `CanonicalWriteService` only. It does not call lower-level persistence to write canonical records.
- Uses a deterministic CanonicalWrite idempotency key derived from claim id, review event id, target entity, target field path, and a hash-only proposed value reference.
- Repeated identical allowed bridge attempts return the existing WorkflowEvent audit through existing `WorkflowEventService` idempotency.
- Gate-blocked bridge attempts append no new audit row under the current `CanonicalWriteService` design; this is a documented limitation rather than a separate blocked-attempt ledger.
- Task 5F regression-covers both the default low-authority blocked path and an allowed fixture path. The allowed path appends only the existing `CanonicalWriteService` WorkflowEvent audit and still reports `canonicalPersistencePerformed=false`.
- Current Task 5C governed-intake claims are low-authority `inference`, `weak_signal`, `ai_extracted`, and `internal_only`; the existing `CanonicalWriteGate` blocks them rather than promoting them to fact.
- ReviewEvent is treated as review evidence only. It does not automatically make a claim a fact.
- Bulk review remains evidence only and cannot create `candidate_confirmed` or `external_verified` outcomes through the existing gate.
- Allowed boundary attempts, when a governed-intake-lineage fixture uses gate-allowable claim metadata, append only the existing `CanonicalWriteService` WorkflowEvent audit and still report `canonicalPersistencePerformed=false`.
- Does not mutate ClaimLedger verification status, set `claim_ledger_item.review_event_id`, mutate ReviewEvent, write CandidateProfile, write raw Candidate/Profile persistence, query business target entities, mutate entity state, implement a workflow engine, validate transition legality, expose API/controller/UI behavior, expose output to Client, wire AI models, implement Consent/Disclosure, or implement RBAC/ABAC.
- Does not read or write earlier V2 `recruiting.source_item` or `recruiting.information_packet` skeleton tables.

## ClaimLedgerItemCanonicalWriteLookupPort / JdbcClaimLedgerItemCanonicalWriteLookupPort

- Narrow read-only lookup used only for Task 5E canonical write bridge claim loading by exact organization and claim id.
- Reads only `governance.claim_ledger_item`.
- Returns the minimal claim snapshot needed to create a `CanonicalWriteCommand`: target entity, target field path, claim type, assertion strength, verification status, client shareability, `canonical_write_allowed`, optional claim value text, and source-span lineage.
- Does not provide generic ClaimLedger search, dashboard analytics, API exposure, canonical read models, candidate/company/job joins, business target entity lookup, or client-safe projection.

## ReviewEventCanonicalWriteLookupPort / JdbcReviewEventCanonicalWriteLookupPort

- Narrow read-only lookup used only for Task 5E canonical write bridge review loading by exact organization and review event id.
- Reads only `governance.review_event`.
- Returns the minimal review snapshot needed to create `CanonicalWriteReviewEvidence`: target entity, target field path, ClaimLedgerItem id, source-span lineage, decision, risk tier, bulk flag, reviewer id, and reason.
- Does not provide generic review history search, dashboard analytics, API exposure, candidate/company/job joins, business target entity lookup, or client-safe projection.

## CandidateProfile Domain Contracts

- Backend-owned Task 6A contract/vocabulary package for the CandidateProfile aggregate concept.
- Defines immutable domain records/value objects for `CandidateProfile`, `CandidateProfileId`, `CandidateId`, `CandidateProfileVersion`, `CandidateProfileField`, `CandidateProfileFieldPath`, `CandidateProfileFieldValue`, `CandidateProfileFieldLineage`, `CandidateProfileFieldSourceReference`, `CandidateProfileFieldConflict`, and `CandidateProfileFieldStaleness`.
- Defines stable canonical field paths such as `identity.full_name`, `identity.preferred_name`, `contact.email`, `contact.phone`, `location.current_location`, `location.preferred_locations`, `compensation.current_salary`, `compensation.expected_salary`, `availability.notice_period`, `availability.available_from`, `experience.current_company`, `experience.current_title`, `experience.years_of_experience`, `experience.work_history`, `skills.primary_skills`, `skills.secondary_skills`, `education.highest_degree`, `education.schools`, `intent.open_to_opportunities`, `intent.interest_level`, `consent.latest_profile_version`, and `metadata.notes`.
- Defines field status vocabulary: `AI_EXTRACTED`, `HUMAN_ACKNOWLEDGED`, `CONSULTANT_ATTESTED`, `CANDIDATE_CONFIRMED`, `EXTERNAL_VERIFIED`, `SYSTEM_INFERENCE`, `CONFLICTING`, `NEEDS_CONFIRMATION`, `STALE`, `UNVERIFIED`, and `LIKELY_CURRENT`.
- `AI_EXTRACTED` means AI extracted from source; it remains not fact and not verified client-safe statement.
- `HUMAN_ACKNOWLEDGED` means a consultant saw or temporarily accepted a field, possibly by bulk approval; it is not verified.
- `CONSULTANT_ATTESTED` is distinct from `CANDIDATE_CONFIRMED`; it represents consultant responsibility and must keep an actor reference.
- `CANDIDATE_CONFIRMED` is distinct from `EXTERNAL_VERIFIED`; it requires candidate actor/profile version semantics.
- `EXTERNAL_VERIFIED` requires external evidence lineage.
- `SYSTEM_INFERENCE` is forbidden as fact and remains an internal hint.
- `CONFLICTING` blocks client-visible verified fact statements and later canonical overwrite.
- `NEEDS_CONFIRMATION` is not transaction-ready and must block shortlist/consent/disclosure readiness in later tasks.
- `STALE`, `UNVERIFIED`, and `LIKELY_CURRENT` are representational status vocabulary only; later gates must decide whether and how they can be used.
- `CandidateProfileFieldStatusPolicy` is pure metadata only. It does not call `CanonicalWriteGate`, does not call `CanonicalWriteService`, does not write canonical state, and does not create client-safe projections.
- Bulk approve maps to `HUMAN_ACKNOWLEDGED` at most and cannot produce `CANDIDATE_CONFIRMED` or `EXTERNAL_VERIFIED`.
- Lineage references can name ClaimLedgerItem, ReviewEvent, SourceItem, InformationPacket, IntakeExtractionRun, WorkflowEvent, source spans, and external evidence. These references support auditability but are not proof by themselves.
- Conflict and staleness metadata only describe conflict/stale state. They do not implement conflict resolution, stale detection, profile mutation, workflow transitions, or fact promotion.
- Task 6B adds persistence as a separate backend-internal service/port/adapter layer; the domain contracts remain independent of JDBC, Spring Data, API, UI, CanonicalWrite, governed-intake bridge, and client-safe projection concerns.

## CandidateProfile Persistence

- Backend-internal Task 6B persistence skeleton for explicit CandidateProfile create/read/field-upsert operations.
- `CandidateProfileService` validates create/upsert requests and builds `CandidateProfile` / `CandidateProfileField` domain objects before delegating to the port.
- `CandidateProfilePersistencePort` is narrow: create profile, find by organization-scoped profile id, find latest profile by organization-scoped candidate id, upsert one field, and list fields for one organization-scoped profile.
- `JdbcCandidateProfilePersistencePort` uses plain JDBC/DataSource and reuses the existing V2 `recruiting.candidate_profile` table.
- `JdbcCandidateProfilePersistencePort` participates in Spring-managed transactions through transaction-aware JDBC connection handling.
- No new migration, table, index, client-facing view, API DTO table, or competing CandidateProfile table was added in Task 6B.
- `field_status_map` stores field path to status wire value.
- `metadata.candidate_profile_fields` stores field value JSON, lineage, conflict metadata, staleness metadata, review timestamps, actor/profile-version confirmation references, source claim id, source review event id, source workflow event id, and notes.
- `source_claim_ids` stores a summary of field source ClaimLedgerItem ids where a field carries them.
- Create requires a matching `recruiting.candidate` row in the same organization before inserting a profile row; the adapter does not insert or update raw `recruiting.candidate`.
- Profile id and candidate id lookups are organization-scoped.
- `HUMAN_ACKNOWLEDGED` and `SYSTEM_INFERENCE` can be persisted as non-verified statuses, but they remain not verified fact and are not client fact eligible.
- `CANDIDATE_CONFIRMED` still requires candidate actor/profile-version semantics; `EXTERNAL_VERIFIED` still requires external evidence lineage.
- Bulk approval remains capped below `CANDIDATE_CONFIRMED` and `EXTERNAL_VERIFIED`.
- This service does not evaluate ClaimLedgerItem or ReviewEvent, does not promote claims, does not mutate ClaimLedger verification status, does not mutate ReviewEvent, does not call `CanonicalWriteGate`, and does not call `CanonicalWriteService`.
- This service is not a REST/API/controller/DTO/UI surface and is not reachable from Client.
- This service does not expose raw Candidate/Profile to Client, does not implement ClientSafeCandidateCard, does not implement client-safe projection/redaction, does not implement RBAC/ABAC, and does not implement Consent/Disclosure.
- This service does not wire AI models, perform real AI extraction, parse CV/JD/WeChat/email/call-note content, implement workflow engine behavior, or validate transition legality.

## ClaimLedgerService

- Appends claims.
- Does not write canonical facts.
- Uses `governance.claim_ledger_item`.

## ReviewEventService

- Appends human review events.
- Records `bulk_flag`, `risk_tier`, `decision`, `reason`.
- Does not promote facts.
- Does not mutate ClaimLedger verification status.
- Uses `governance.review_event`.

## WorkflowEventService

- Appends audit/workflow events.
- Validates workflow action vocabulary and audit policy before append.
- Validates optional idempotency, correlation, and causation identifiers before append.
- Uses idempotency only to prevent accidental duplicate appends: duplicate equivalent payloads return the existing WorkflowEvent id, while duplicate different payloads are rejected as conflicts.
- Stores correlation id to group audit events for one business operation.
- Stores causation id to link an event to the prior event, request, or boundary that caused it; null remains valid for root events.
- Rejects unknown action codes and policy-unsafe audit requests.
- Does not validate transitions.
- Does not mutate entity state.
- Does not query or update the target entity.
- Does not expose broad workflow search/list/read-model behavior; append idempotency lookup remains idempotency-key scoped.
- Task 4C audit read-model behavior is separated into `WorkflowAuditQueryService` and `WorkflowAuditReadPort`.
- Uses `workflow.workflow_event`.

## WorkflowTransitionAuditService

- Backend-internal service boundary for recording requested workflow state-transition audit events.
- Validates `WorkflowTransitionAuditRequest` syntax and policy shape before append.
- Requires organization id, entity namespace/type/id, action code, actor type/id, AI involvement, source type, occurred-at timestamp, `before_state`, and `after_state`.
- Rejects equal `before_state` and `after_state`.
- Rejects unknown action codes.
- Rejects append-only audit actions that are not configured as `WorkflowActionPolicy.stateTransition`.
- Uses existing `WorkflowActionRegistry` policy validation for reason and T3/T4 human final actor requirements.
- Maps to the existing `WorkflowEventService.append` boundary so idempotency, correlation, and causation behavior remain unchanged.
- Does not validate legal `from_state -> to_state` paths.
- Does not implement a workflow engine or state machine.
- Does not query, join, update, or mutate Job, Candidate, Shortlist, Consent, Disclosure, Placement, Commission, ClaimLedger, ReviewEvent, or AITaskRun records.
- Does not expose API/controller/UI behavior.

## WorkflowAuditQueryService

- Backend-internal read-only service for appended `WorkflowEvent` audit records.
- Requires `organization_id` for every query.
- Validates limit, offset, and occurred-at range.
- Supports narrow audit filters only: event id, entity type/id, action code, actor type/id, correlation id, causation id, idempotency key, and occurred-at range.
- Returns deterministic ordering: `occurred_at DESC`, then `workflow_event_id DESC`.
- Does not append, update, delete, or mutate any state.
- Does not validate transition legality.
- Does not authorize users yet; RBAC/ABAC remains future work.
- Does not expose API/controller/UI behavior.
- Does not produce client-safe projections.

## WorkflowAuditReadPort / JdbcWorkflowAuditReadPort

- Read-only port and JDBC adapter for the `workflow.workflow_event` audit read model.
- Reads only from `workflow.workflow_event`.
- Does not replace `WorkflowEventPort`.
- Does not join Candidate, Company, Job, Consent, Disclosure, Placement, or Commission tables.
- Does not expose raw Candidate/Profile payloads, source document contents, metadata JSON, or business entity internals.
- Does not implement full-text search, arbitrary SQL filters, generic sorting, dashboard analytics, or broad repository behavior.
- Uses existing Task 4B columns and indexes for idempotency, correlation, causation, entity timeline, and event id lookup.

## CanonicalWriteGate

- Pure domain gate.
- Blocks unsafe `system_inference`, weak/implied intent, conflicting claims, forbidden/internal-only client visibility, unsafe T3/T4 paths.
- Bulk approve cannot become `candidate_confirmed` or `external_verified`.

## CanonicalWriteService

- Gate-first boundary.
- Requires claim snapshot and review evidence.
- Runs the existing boundary attempt inside `CanonicalWriteTransactionBoundary`.
- Does not bypass `CanonicalWriteGate`.
- Allowed boundary appends a `WorkflowEvent`.
- Allowed boundary propagates idempotency/correlation/causation identifiers when the command supplies them.
- Canonical persistence is explicitly deferred.
- Does not write CandidateProfile.
- Task 6B/6C do not change this behavior and do not call `CanonicalWriteService` from CandidateProfile persistence.

## Transaction Boundary

- Task 6C hardens `CanonicalWriteTransactionBoundary` with `SpringCanonicalWriteTransactionBoundary`.
- The implementation uses Spring `PlatformTransactionManager` and `TransactionTemplate`.
- Nested behavior follows Spring's default `PROPAGATION_REQUIRED`: join an existing transaction when present, otherwise create one around the canonical write callback.
- Successful callbacks commit and preserve their returned result.
- Runtime callback failures propagate and roll back participating JDBC writes.
- Checked callback failures are explicitly wrapped in `CanonicalWriteTransactionException` and roll back participating JDBC writes.
- The boundary contains no gate, ClaimLedger, ReviewEvent, WorkflowEvent, CandidateProfile, API, UI, AI, or workflow-engine business logic.
- Rollback behavior is tested against PostgreSQL/Testcontainers by appending a `WorkflowEvent` inside the boundary and throwing after the append.
- This transaction boundary still does not implement the real canonical write flow and does not write CandidateProfile.

## Forbidden Current Misreadings

- Do not treat `GovernedIntakeService` as AI extraction, ClaimLedger append, ReviewEvent creation, CanonicalWrite, CandidateProfile persistence, workflow engine, transition legality validation, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, or raw Candidate exposure.
- Do not treat `SourceItem` or `InformationPacket` as canonical facts.
- Do not treat `DeterministicIntakeExtractionService` as real AI extraction, semantic parsing, ClaimLedger append, ReviewEvent creation, CanonicalWrite, CandidateProfile persistence, workflow engine, transition legality validation, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, or raw Candidate exposure.
- Do not treat `IntakeExtractionOutputEnvelope` or `intake.extraction_run.output_json` as canonical facts, ClaimLedger, ReviewEvent, CandidateProfile, CanonicalWrite output, API DTO, UI state, or client-safe projection.
- Do not treat `IntakeClaimLedgerBridgeService` as ReviewEvent creation, CanonicalWrite, CandidateProfile persistence, raw Candidate/Profile persistence, workflow engine, transition legality validation, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, real AI extraction, semantic parser, or client exposure.
- Do not treat `IntakeReviewBridgeService` as CanonicalWrite, CandidateProfile persistence, ClaimLedger verification mutation, raw Candidate/Profile persistence, workflow engine, transition legality validation, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, real AI extraction, semantic parser, or client exposure.
- Do not treat `IntakeCanonicalWriteBridgeService` as CandidateProfile persistence, ClaimLedger verification mutation, ReviewEvent mutation, raw Candidate/Profile persistence, workflow engine, transition legality validation, business target entity lookup, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, real AI extraction, semantic parser, or client exposure.
- Do not treat Task 6A CandidateProfile contracts as CandidateProfile persistence, canonical writes, raw Candidate/Profile persistence, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, real AI extraction, workflow engine, transition legality validation, or transaction boundary hardening.
- Do not treat Task 6B CandidateProfile persistence as governed-intake automatic writes, `CanonicalWriteService` writes, `CanonicalWriteGate` bypass, claim-to-fact promotion, real canonical write flow, raw Candidate writes, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, real AI extraction, workflow engine, or transition legality validation.
- Do not treat Task 6C transaction hardening as governed-intake automatic writes, `CanonicalWriteService` CandidateProfile writes, `CanonicalWriteGate` bypass, claim-to-fact promotion, ReviewEvent mutation, ClaimLedger verification mutation, raw Candidate/Profile persistence, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, AI model wiring, real AI extraction, workflow engine, or transition legality validation.
- Do not treat Task 5C as cleanup/deprecation/migration of the earlier `recruiting.*` source/packet skeleton tables. For this bridge, `intake.*` is operational governed-intake lineage and `recruiting.*` remains deferred schema cleanup.
- Do not treat Task 5D as cleanup/deprecation/migration of the earlier `recruiting.*` source/packet skeleton tables. For this bridge, `intake.*` is operational governed-intake lineage and `recruiting.*` remains deferred schema cleanup.
- Do not treat Task 5E as cleanup/deprecation/migration of the earlier `recruiting.*` source/packet skeleton tables. For this bridge, `intake.*` is operational governed-intake lineage and `recruiting.*` remains deferred schema cleanup.
- Do not treat `CanonicalWriteService` as CandidateProfile persistence.
- Do not treat `WorkflowEventService` as workflow engine.
- Do not treat `WorkflowTransitionAuditService` as a workflow engine, state machine, SLA engine, automation engine, entity mutator, entity repository, API, or UI.
- Do not treat Task 4D transition audit validation as legal `from_state -> to_state` validation.
- Do not treat WorkflowEvent action policy validation as transition legality validation.
- Do not treat WorkflowEvent idempotency/correlation/causation guardrails as a workflow engine, SLA engine, automation engine, API, or UI.
- Do not treat Task 4C audit read model as API, UI, client-safe projection, RBAC/ABAC, dashboard analytics, or workflow engine.
- Do not treat `ReviewEventService` as verification promotion.
- Do not treat `ClaimLedgerService` as canonical fact storage.
- Do not treat the real transaction boundary as the real canonical write flow.
- Do not expose raw Candidate to Client.
- Do not treat Task 4C as ConsentRecord, DisclosureRecord, RBAC/ABAC, Client-safe projection, AI model wiring, or CandidateProfile canonical persistence.
