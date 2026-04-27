# Implementation Status

## Current Git Main Milestones

- `beb71d4` Add product specs as source of truth: established `docs/specs/CURRENT_SPEC.md`, v2.1 as current product source of truth, and preserved v2.0 as UI / portal baseline.
- `f1b2e0a` Initialize production kernel skeleton: created the first production-kernel repo skeleton for the production-first Recruiting Transaction OS.
- `45f979a` Document contracts-first truth layer design: added the Task 2A contracts-first truth-layer design documents.
- `1aca18b` Add truth layer contract skeleton: introduced the initial truth-layer Java/domain contract skeleton.
- `09571ee` Verify truth layer migrations against PostgreSQL: verified Flyway/PostgreSQL truth-layer migration behavior.
- `22bdf98` Add truth layer canonical write gate skeleton: introduced the first CanonicalWriteGate boundary.
- `fc18e0e` Add truth layer alignment tests: checked alignment across domain contracts, documentation, and migration shape.
- `4d90e2c` Add truth layer negative policy tests: added negative policy coverage for unsafe truth-layer behavior.
- `8a8b670` Add truth layer persistence port contracts: added minimal append-oriented persistence port contracts.
- `1ffc5d5` Implement claim ledger append persistence: implemented append persistence for ClaimLedger records.
- `53d6469` Implement review event append persistence: implemented append persistence for ReviewEvent records.
- `eac26cd` Implement workflow event append persistence: implemented append persistence for WorkflowEvent records.
- `9f6e097` Add canonical write transaction boundary: added the CanonicalWriteTransactionBoundary skeleton.
- `e55069c` Harden truth layer service boundaries: hardened service boundaries and regression coverage through Task 3E.
- Task 4A: added stable workflow action/entity/risk/actor/AI involvement vocabulary, a workflow audit policy registry, and append-boundary policy validation for `WorkflowEventService`.
- Task 4B: added `WorkflowEvent` idempotency, correlation, and causation guardrails at the audit append boundary.
- Task 4C: added a backend-internal, read-only `WorkflowEvent` audit query/read model skeleton.
- Task 4D current worktree: adds a backend-internal `WorkflowTransitionAuditService` / `WorkflowTransitionAuditRequest` skeleton for recording requested workflow state-transition audit events with `before_state` and `after_state`.
- Task 5A current worktree: adds backend-owned `SourceItem` and `InformationPacket` governed-intake contracts, narrow create/attach/read service and persistence ports, JDBC adapters, and a V4 `intake` schema with packet/source link table.
- Task 5B current worktree: adds backend-owned governed-intake extraction run/output contracts, a deterministic no-real-AI placeholder extractor, a narrow extraction-run persistence port/JDBC adapter, and a V5 `intake.extraction_run` JSONB output-envelope table.
- Task 5C current worktree: adds a backend-owned `IntakeClaimLedgerBridgeService` skeleton that reads `intake.extraction_run` output envelopes, validates governed-intake lineage through `intake.information_packet`, and appends only explicitly bridge-eligible operational claim candidates through `ClaimLedgerService`.

## Current Test State

- Full Maven backend reached 119 tests, 0 failures/errors, 1 existing skip after Task 4A.
- Full Maven backend reached 131 tests, 0 failures/errors, 1 existing skip after Task 4B.
- Task 4B added focused unit and PostgreSQL/Testcontainers coverage for idempotency/correlation/causation behavior.
- Task 4C adds focused unit and PostgreSQL/Testcontainers coverage for read-only audit query behavior and boundaries.
- Task 4D adds focused unit and PostgreSQL/Testcontainers coverage for transition audit request validation, transition-action classification, idempotency/correlation/causation propagation, persistence, read-model visibility, and organization isolation.
- Task 5A adds focused unit and PostgreSQL/Testcontainers coverage for SourceItem/InformationPacket validation, duplicate attach rejection, organization-scoped lookup/list behavior, V4 migration application, source/packet/link persistence, and non-canonical boundary assertions.
- Task 5B adds focused unit and PostgreSQL/Testcontainers coverage for deterministic extraction validation, no-source failed run behavior, output-envelope flags, duplicate extraction determinism, organization-scoped persistence/readback, V5 migration application, and the absence of ClaimLedger, ReviewEvent, WorkflowEvent, CanonicalWrite, CandidateProfile, and old `recruiting.*` writes.
- Task 5C adds focused unit and PostgreSQL/Testcontainers coverage for bridge request validation, missing/wrong-organization/failed/missing-output extraction rejection, default placeholder no-claim behavior, explicit operational bridge-eligible fixture append, duplicate source-reference replay, organization isolation, V6 migration/index application, and absence of ReviewEvent, WorkflowEvent, CanonicalWrite, CandidateProfile, raw Candidate/Profile, and old `recruiting.*` use.
- Docker/Testcontainers PostgreSQL is part of required validation.
- `docker info` must pass before full Maven validation.
- Maven command:

```sh
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

## Current Truth Layer Capabilities

- `ClaimLedgerService` appends to `governance.claim_ledger_item`.
- `ReviewEventService` appends to `governance.review_event`.
- `WorkflowEventService` appends to `workflow.workflow_event`.
- `WorkflowEventService` validates known workflow action vocabulary and audit policy before append.
- `WorkflowEventService` validates idempotency, correlation, and causation identifiers before append.
- `WorkflowEventService` returns the existing event for duplicate equivalent idempotency-key appends and rejects duplicate different payloads as idempotency conflicts.
- `WorkflowActionRegistry` defines one policy per stable action code, including allowed entity types, risk tier, before/after-state requirements, reason requirements, and AI-only finalization limits.
- `WorkflowAuditQueryService` provides a backend-internal, read-only audit read-model boundary for `workflow.workflow_event` records.
- `WorkflowAuditReadPort` supports narrow audit filters by organization, event id, entity, action, actor, correlation, causation, idempotency key, and occurred-at range.
- `JdbcWorkflowAuditReadPort` reads only from `workflow.workflow_event`; it does not join Candidate, Company, Job, Consent, Disclosure, Placement, or Commission tables.
- `WorkflowTransitionAuditService` provides a backend-internal transition audit boundary that records requested state-transition audit events through `WorkflowEventService`.
- `WorkflowTransitionAuditService` requires `before_state` and `after_state`, rejects equal states, rejects unknown action codes, and rejects action policies that are not configured as state transitions.
- `WorkflowTransitionAuditService` preserves existing `WorkflowEvent` idempotency, correlation, and causation behavior by mapping to the existing append command.
- `CanonicalWriteService` uses `CanonicalWriteGate` and appends audit `WorkflowEvent` for allowed boundary attempts, propagating idempotency/correlation/causation identifiers when supplied.
- `GovernedIntakeService` creates and reads governed-intake `SourceItem` and `InformationPacket` records and attaches source items to packets through narrow backend-owned ports.
- `SourceItem` stores provenance/raw-source metadata, refs, hashes, actor provenance, received/created timestamps, metadata JSON, and source status in `intake.source_item`.
- `InformationPacket` stores packet grouping intent, intended entity type/id, creator provenance, processing status, notes, timestamps, and metadata JSON in `intake.information_packet`.
- `intake.information_packet_source_item` records packet/source grouping and rejects duplicate source attachment for the same organization and packet.
- Governed-intake lookup and list operations are organization-scoped.
- `DeterministicIntakeExtractionService` creates governed-intake extraction runs for an `InformationPacket` and its attached `SourceItem` metadata.
- The Task 5B extractor mode is only `DETERMINISTIC_PLACEHOLDER`; it performs no real AI extraction, no LLM call, no OCR/STT/file conversion, and no semantic parsing of CV/JD/WeChat/email/call-note content.
- Successful extraction output is an intermediate `IntakeExtractionOutputEnvelope` only. It records packet type, intended entity type, source ids/safe source snapshots, deterministic placeholder fields, findings, and no errors for currently supported placeholder operation.
- The output envelope explicitly records `real_ai_extraction_performed=false`, `semantic_parsing_performed=false`, `claim_ledger_append_allowed=false`, `canonical_write_allowed=false`, and `needs_future_extraction=true`.
- `IntakeExtractionRunPort` persists extraction runs/output to `intake.extraction_run`; `output_json` is JSONB and is not normalized into canonical fact tables.
- Extraction run lookup/list operations are organization-scoped and linked to `intake.information_packet`.
- If an `InformationPacket` has no attached `SourceItem`, Task 5B persists a deterministic `FAILED` extraction run with no output envelope and the failure reason `information packet has no attached source items`.
- Duplicate extraction is allowed in Task 5B: each call creates a new extraction run id, while the source snapshot hash and deterministic placeholder output remain stable for the same packet/source metadata.
- Task 5B does not update `InformationPacket.processingStatus`; packet status transitions remain future governed-intake work.
- `IntakeClaimLedgerBridgeService` provides the Task 5C governed-intake extraction-output to ClaimLedger bridge skeleton.
- The bridge reads `intake.extraction_run` by organization-scoped extraction run id, validates a `SUCCEEDED` run with an output envelope, validates the packet through `intake.information_packet`, and appends only through `ClaimLedgerService`.
- Default deterministic placeholder extraction output is intentionally not bridge-eligible and creates no fake business claims.
- Bridge-eligible fixture fields must be explicitly marked `CLAIM_CANDIDATE`, use the `intake.bridge_eligible.*` operational field prefix, have intake source-item lineage, and are mapped to internal-only `ClaimLedgerItem` records with `claim_value_text`, `verification_status=ai_extracted`, `assertion_strength=weak_signal`, `claim_type=inference`, and `canonical_write_allowed=false`.
- Task 5C stores governed-intake lineage in deterministic `source_span_ref` values and uses V6 `claim_ledger_org_source_span_idx` for narrow idempotency lookup. It does not populate `source_item_id` from `intake.source_item` because the existing V2 column still references `recruiting.source_item`.
- Repeated bridge calls for the same extraction-run/field/source reference return the existing claim id and do not append duplicate claims.
- For Task 5C, `intake.*` is the operational governed-intake source for ClaimLedger linkage; earlier `recruiting.source_item` and `recruiting.information_packet` remain V2 skeleton artifacts and are not read or written by this bridge.
- Canonical persistence is explicitly deferred.
- `CanonicalWriteTransactionBoundary` is skeleton/no JDBC rollback coordination.
- No endpoint/API/UI/AI wiring exists for this flow yet.

## Current Non-capabilities

- No CandidateProfile canonical persistence.
- No raw Candidate/Profile persistence.
- No real AI extraction from SourceItem or InformationPacket.
- No semantic extraction from SourceItem or InformationPacket.
- No business-fact ClaimLedger append from default governed-intake placeholder output.
- No ReviewEvent creation from governed intake.
- No CanonicalWrite call from governed intake.
- No CandidateProfile persistence from governed intake.
- No workflow engine.
- No SLA/automation workflow engine.
- No transition validation.
- No transition legality validation; WorkflowEvent policy validation is audit request validation only.
- No legal `from_state -> to_state` validation in the Task 4D transition audit skeleton.
- No target entity lookup or state mutation in `WorkflowTransitionAuditService`.
- No API layer.
- No UI integration.
- No AI model integration.
- No Consent/Disclosure implementation.
- No Client-safe projection.
- No RBAC/ABAC implementation.
- No dashboard analytics or generic repository search.
- Task 5 Governed Intake Minimal Slice remains incomplete until later subtasks add ReviewEvent creation, CanonicalWrite boundary integration, CandidateProfile persistence, downstream privacy/access surfaces, API/UI wiring, and real AI extraction.
