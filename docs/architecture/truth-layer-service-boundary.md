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
- Does not bypass `CanonicalWriteGate`.
- Allowed boundary appends a `WorkflowEvent`.
- Allowed boundary propagates idempotency/correlation/causation identifiers when the command supplies them.
- Canonical persistence is explicitly deferred.
- Does not write CandidateProfile.

## Transaction Boundary

- Current `CanonicalWriteTransactionBoundary` is skeleton/no JDBC rollback coordination.
- Future work must implement real transaction coordination before canonical multi-table writes.

## Forbidden Current Misreadings

- Do not treat `GovernedIntakeService` as AI extraction, ClaimLedger append, ReviewEvent creation, CanonicalWrite, CandidateProfile persistence, workflow engine, transition legality validation, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, or raw Candidate exposure.
- Do not treat `SourceItem` or `InformationPacket` as canonical facts.
- Do not treat `DeterministicIntakeExtractionService` as real AI extraction, semantic parsing, ClaimLedger append, ReviewEvent creation, CanonicalWrite, CandidateProfile persistence, workflow engine, transition legality validation, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, or raw Candidate exposure.
- Do not treat `IntakeExtractionOutputEnvelope` or `intake.extraction_run.output_json` as canonical facts, ClaimLedger, ReviewEvent, CandidateProfile, CanonicalWrite output, API DTO, UI state, or client-safe projection.
- Do not treat `IntakeClaimLedgerBridgeService` as ReviewEvent creation, CanonicalWrite, CandidateProfile persistence, raw Candidate/Profile persistence, workflow engine, transition legality validation, API, UI, client-safe projection, Consent/Disclosure, RBAC/ABAC, real AI extraction, semantic parser, or client exposure.
- Do not treat Task 5C as cleanup/deprecation/migration of the earlier `recruiting.*` source/packet skeleton tables. For this bridge, `intake.*` is operational governed-intake lineage and `recruiting.*` remains deferred schema cleanup.
- Do not treat `CanonicalWriteService` as CandidateProfile persistence.
- Do not treat `WorkflowEventService` as workflow engine.
- Do not treat `WorkflowTransitionAuditService` as a workflow engine, state machine, SLA engine, automation engine, entity mutator, entity repository, API, or UI.
- Do not treat Task 4D transition audit validation as legal `from_state -> to_state` validation.
- Do not treat WorkflowEvent action policy validation as transition legality validation.
- Do not treat WorkflowEvent idempotency/correlation/causation guardrails as a workflow engine, SLA engine, automation engine, API, or UI.
- Do not treat Task 4C audit read model as API, UI, client-safe projection, RBAC/ABAC, dashboard analytics, or workflow engine.
- Do not treat `ReviewEventService` as verification promotion.
- Do not treat `ClaimLedgerService` as canonical fact storage.
- Do not treat transaction boundary skeleton as real rollback.
- Do not expose raw Candidate to Client.
- Do not treat Task 4C as ConsentRecord, DisclosureRecord, RBAC/ABAC, Client-safe projection, AI model wiring, or CandidateProfile canonical persistence.
