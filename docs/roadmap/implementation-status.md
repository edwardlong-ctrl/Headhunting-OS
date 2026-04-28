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
- Task 5D current worktree: adds a backend-owned `IntakeReviewBridgeService` skeleton that reads governed-intake-origin `ClaimLedgerItem` rows by organization-scoped claim id and appends human review evidence only through `ReviewEventService`.
- Task 5E current worktree: adds a backend-owned `IntakeCanonicalWriteBridgeService` skeleton that reads governed-intake-origin `ClaimLedgerItem` plus matching `ReviewEvent` evidence by organization scope and submits only a boundary attempt through `CanonicalWriteService`.
- Task 5F current worktree: adds end-to-end PostgreSQL/Testcontainers regression coverage and documentation closure for the governed-intake minimal slice from `intake.source_item` / `intake.information_packet` through deterministic placeholder extraction, ClaimLedger claim append, ReviewEvent evidence append, CanonicalWriteService boundary attempt, mandatory CanonicalWriteGate decision, and no canonical persistence.
- Task 6A current worktree: adds backend-owned `candidateprofile` domain contracts for the CandidateProfile aggregate concept, field path vocabulary, field status vocabulary, source lineage references, conflict metadata, staleness metadata, profile version semantics, and pure status policy helpers only.
- Task 6B current worktree: adds backend-internal CandidateProfile persistence skeleton with a narrow `CandidateProfilePersistencePort`, `CandidateProfileService`, `JdbcCandidateProfilePersistencePort`, explicit create/read/field-upsert methods, and reuse of the existing `recruiting.candidate_profile` table.
- Task 6C current worktree: hardens `CanonicalWriteTransactionBoundary` from a no-op skeleton into a Spring `PlatformTransactionManager` / `TransactionTemplate` boundary with real JDBC transaction coordination and rollback coverage, while keeping `CanonicalWriteService` gate/audit-only.

## Current Test State

- Full Maven backend reached 119 tests, 0 failures/errors, 1 existing skip after Task 4A.
- Full Maven backend reached 131 tests, 0 failures/errors, 1 existing skip after Task 4B.
- Task 4B added focused unit and PostgreSQL/Testcontainers coverage for idempotency/correlation/causation behavior.
- Task 4C adds focused unit and PostgreSQL/Testcontainers coverage for read-only audit query behavior and boundaries.
- Task 4D adds focused unit and PostgreSQL/Testcontainers coverage for transition audit request validation, transition-action classification, idempotency/correlation/causation propagation, persistence, read-model visibility, and organization isolation.
- Task 5A adds focused unit and PostgreSQL/Testcontainers coverage for SourceItem/InformationPacket validation, duplicate attach rejection, organization-scoped lookup/list behavior, V4 migration application, source/packet/link persistence, and non-canonical boundary assertions.
- Task 5B adds focused unit and PostgreSQL/Testcontainers coverage for deterministic extraction validation, no-source failed run behavior, output-envelope flags, duplicate extraction determinism, organization-scoped persistence/readback, V5 migration application, and the absence of ClaimLedger, ReviewEvent, WorkflowEvent, CanonicalWrite, CandidateProfile, and old `recruiting.*` writes.
- Task 5C adds focused unit and PostgreSQL/Testcontainers coverage for bridge request validation, missing/wrong-organization/failed/missing-output extraction rejection, default placeholder no-claim behavior, explicit operational bridge-eligible fixture append, duplicate source-reference replay, organization isolation, V6 migration/index application, and absence of ReviewEvent, WorkflowEvent, CanonicalWrite, CandidateProfile, raw Candidate/Profile, and old `recruiting.*` use.
- Task 5D adds focused unit and PostgreSQL/Testcontainers coverage for review bridge request validation, missing/wrong-organization claim blocking, governed-intake-only claim policy, ReviewEvent append through `ReviewEventService`, claim lineage persistence, deterministic duplicate review handling, materially different review evidence, T3/T4 human reviewer enforcement, ClaimLedger immutability, and absence of CanonicalWrite, CandidateProfile, raw Candidate/Profile, workflow, API/UI, and old `recruiting.*` use.
- Task 5E adds focused unit and PostgreSQL/Testcontainers coverage for canonical-write bridge request validation, missing/wrong-organization ClaimLedger and ReviewEvent rejection, ReviewEvent-to-ClaimLedger lineage validation, governed-intake-only policy, CanonicalWriteService invocation, mandatory CanonicalWriteGate behavior, gate-blocked governed-intake claims, allowed boundary audit with `canonicalPersistencePerformed=false`, WorkflowEvent idempotency for repeated allowed attempts, ClaimLedger and ReviewEvent immutability, no CandidateProfile/raw Candidate/Profile writes, no business target entity queries, no API/UI, and no old `recruiting.*` source/packet use.
- Task 5F adds a focused end-to-end PostgreSQL/Testcontainers regression proving the safe chain: SourceItem / InformationPacket in `intake.*`, deterministic placeholder output envelope, default placeholder no-claim behavior, bridge-eligible operational fixture to ClaimLedgerItem claim, ReviewEvent evidence-not-promotion, CanonicalWriteService boundary attempt, mandatory CanonicalWriteGate block/allow behavior, `canonicalPersistencePerformed=false`, wrong-organization isolation, ClaimLedger/ReviewEvent immutability, no CandidateProfile/raw Candidate/Profile persistence, no blocked-attempt audit ledger, and no old `recruiting.*` source/packet use.
- Task 6A adds focused unit coverage for CandidateProfile required identifiers, profile version, field path/status/value/lineage validation, stable canonical field paths, required v2.1 status vocabulary, bulk approval capped at `HUMAN_ACKNOWLEDGED`, status policy fact/readiness blockers, source lineage references to ClaimLedgerItem / ReviewEvent / SourceItem / InformationPacket / IntakeExtractionRun / WorkflowEvent / source spans, conflict/staleness metadata, and absence of CandidateProfile persistence/API/UI/canonical-write calls.
- Task 6B adds focused unit and PostgreSQL/Testcontainers coverage for CandidateProfile create request validation, field upsert validation, bulk-approval limits, HUMAN_ACKNOWLEDGED and SYSTEM_INFERENCE non-verified persistence, candidate/external verification lineage requirements, Flyway reuse of the existing V2 `recruiting.candidate_profile` table, organization-scoped profile/candidate lookup, field path/value/status/lineage/conflict/staleness readback, and absence of ClaimLedger/ReviewEvent/CanonicalWrite/governed-intake/API/UI wiring.
- Task 6C adds focused unit coverage for successful transaction callback commit/result preservation, runtime rollback propagation, checked-exception rollback wrapping, and no business logic inside the transaction boundary. It also adds PostgreSQL/Testcontainers coverage proving WorkflowEvent append commit, WorkflowEvent append rollback, CanonicalWriteService allowed audit behavior with `canonicalPersistencePerformed=false`, blocked-path behavior, no CandidateProfile write from CanonicalWriteService, and independent CandidateProfileService persistence.
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
- `CanonicalWriteService` now runs its existing gate/audit attempt inside `CanonicalWriteTransactionBoundary`.
- `SpringCanonicalWriteTransactionBoundary` uses Spring `PlatformTransactionManager` and `TransactionTemplate` with default `PROPAGATION_REQUIRED` semantics; successful callbacks commit and runtime or checked callback failures roll back participating JDBC writes.
- `JdbcWorkflowEventPort` and `JdbcCandidateProfilePersistencePort` now use Spring transaction-aware JDBC connection handling so future canonical write orchestration can participate in the boundary.
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
- `IntakeReviewBridgeService` provides the Task 5D governed-intake ClaimLedger-to-ReviewEvent bridge skeleton.
- The review bridge reads `governance.claim_ledger_item` by organization-scoped claim id through `ClaimLedgerItemReviewLookupPort`, requires Task 5C governed-intake `intake.*` lineage markers, and rejects non-governed-intake claims.
- The review bridge appends review evidence only through `ReviewEventService`; `ReviewEvent` remains review evidence, not fact promotion.
- The review bridge stores claim lineage in `governance.review_event.claim_ledger_item_id` and a deterministic `source_span_ref` containing the reviewed `claim_ledger_item_id`.
- Repeated identical review bridge calls return the existing review event id; materially different review evidence creates a new review event.
- T3/T4 review bridge requests require a non-AI/non-system reviewer and a reason.
- ReviewEvent append does not mutate ClaimLedger verification status and does not call CanonicalWrite.
- For Task 5D, `intake.*` lineage remains the governed-intake source lineage. Earlier `recruiting.source_item` and `recruiting.information_packet` remain V2 skeleton artifacts and are not read or written by this bridge.
- `IntakeCanonicalWriteBridgeService` provides the Task 5E governed-intake ClaimLedgerItem-plus-ReviewEvent to CanonicalWrite boundary integration skeleton.
- The canonical-write bridge reads `governance.claim_ledger_item` and `governance.review_event` by exact organization-scoped ids through narrow lookup ports, validates that the ReviewEvent belongs to the same ClaimLedgerItem, and requires Task 5C/5D governed-intake `intake.*` lineage markers.
- The canonical-write bridge maps claim metadata and review evidence into `CanonicalWriteCommand` and calls `CanonicalWriteService` only; it does not write CandidateProfile, raw Candidate/Profile, ClaimLedger updates, ReviewEvent updates, workflow transitions, or business target entity state.
- `CanonicalWriteGate` remains mandatory and is not bypassed. Current Task 5C governed-intake claims remain low-authority `inference` / `ai_extracted` / `weak_signal` / `internal_only` claims and are blocked by the existing gate rather than promoted to fact.
- Allowed boundary attempts, where a test fixture uses governed-intake lineage with gate-allowable claim metadata, append only the existing `CanonicalWriteService` WorkflowEvent audit and still report `canonicalPersistencePerformed=false`.
- Repeated identical allowed bridge attempts use deterministic CanonicalWriteService/WorkflowEvent idempotency; blocked gate attempts remain deterministic in result but do not append an audit row because the existing `CanonicalWriteService` only appends on allowed boundary audits.
- For Task 5E, `intake.*` lineage remains the governed-intake source lineage. Earlier `recruiting.source_item` and `recruiting.information_packet` remain V2 skeleton artifacts and are not read or written by this bridge.
- Task 5F regression-covers the complete Task 5 safe chain:
  `SourceItem` / `InformationPacket`
  -> deterministic extraction output envelope
  -> `ClaimLedgerItem` claim
  -> `ReviewEvent` evidence
  -> `CanonicalWriteService` boundary attempt
  -> `CanonicalWriteGate` decision
  -> no canonical persistence.
- Task 5F confirms default placeholder output appends no business ClaimLedger claims, bridge-eligible fixtures append claims but not facts, ReviewEvent remains evidence and does not promote facts, low-authority governed-intake claims are gate-blocked, allowed fixtures audit only with `canonicalPersistencePerformed=false`, blocked attempts still have no separate audit ledger, and `intake.*` remains the governed-intake operational lineage while earlier `recruiting.*` source/packet skeleton cleanup remains deferred.
- Task 6A defines the first `CandidateProfile` canonical contract vocabulary as pure backend domain objects only.
- Task 6A CandidateProfile fields use stable `CandidateProfileFieldPath` values such as `identity.full_name`, `contact.email`, `location.current_location`, `compensation.expected_salary`, `experience.work_history`, `skills.primary_skills`, `intent.interest_level`, and `consent.latest_profile_version`.
- Task 6A CandidateProfile field status vocabulary includes `AI_EXTRACTED`, `HUMAN_ACKNOWLEDGED`, `CONSULTANT_ATTESTED`, `CANDIDATE_CONFIRMED`, `EXTERNAL_VERIFIED`, `SYSTEM_INFERENCE`, `CONFLICTING`, `NEEDS_CONFIRMATION`, `STALE`, `UNVERIFIED`, and `LIKELY_CURRENT`.
- `CandidateProfileFieldStatusPolicy` is pure metadata only. It marks bulk approval as `HUMAN_ACKNOWLEDGED`, treats only `CANDIDATE_CONFIRMED` and `EXTERNAL_VERIFIED` as verified fact eligible, blocks `SYSTEM_INFERENCE` and `CONFLICTING` from client-visible fact statements, and treats `NEEDS_CONFIRMATION` as not transaction-ready.
- Task 6A lineage contracts can carry ids or refs for ClaimLedgerItem, ReviewEvent, SourceItem, InformationPacket, IntakeExtractionRun, WorkflowEvent, source spans, and external evidence. These references support auditability but are not proof by themselves and do not query or join upstream tables.
- Task 6A conflict and staleness metadata are representational only. They do not resolve conflicts, detect stale fields, mutate profiles, or trigger workflow.
- Task 6B adds explicit backend-internal CandidateProfile persistence calls only through `CandidateProfileService` and `CandidateProfilePersistencePort`.
- `JdbcCandidateProfilePersistencePort` reuses `recruiting.candidate_profile`; it stores field status wire values in `field_status_map`, field values/lineage/conflict/staleness documents in `metadata.candidate_profile_fields`, and source claim id summaries in `source_claim_ids`.
- CandidateProfile persistence lookups are organization-scoped by profile id or candidate id, and create requires the candidate row to belong to the same organization.
- Task 6B does not add a migration, new table, API/controller/DTO/UI, client-safe projection, RBAC/ABAC, Consent/Disclosure, AI wiring, governed-intake bridge write, or CanonicalWriteService write.
- Canonical write flow from ClaimLedger/ReviewEvent/GovernedIntake to CandidateProfile remains explicitly deferred.
- Task 6C adds real transaction coordination only; it does not add the allowed write to CandidateProfile.
- No endpoint/API/UI/AI wiring exists for this flow yet.

## Current Non-capabilities

- No automatic CandidateProfile canonical persistence from governed intake or `CanonicalWriteService`.
- CandidateProfile table writes to `recruiting.candidate_profile` exist only through backend-internal explicit `CandidateProfileService` / `JdbcCandidateProfilePersistencePort` calls.
- No raw Candidate writes to `recruiting.candidate`.
- No generic CandidateProfile repository/search/list surface beyond the narrow Task 6B port methods.
- No CandidateProfile API/controller/DTO/UI.
- No CandidateProfile client-safe projection or redaction.
- No raw Candidate/Profile exposure to Client.
- No real AI extraction from SourceItem or InformationPacket.
- No semantic extraction from SourceItem or InformationPacket.
- No business-fact ClaimLedger append from default governed-intake placeholder output.
- Governed intake can now submit a ClaimLedgerItem plus ReviewEvent evidence to the CanonicalWriteService boundary through Task 5E, but this remains gate/audit only and does not persist canonical profile data.
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
- Task 5 Governed Intake Minimal Slice is closed as a regression-covered safe chain only. CandidateProfile persistence is now backend-internal only; downstream privacy/access surfaces, governed-intake write wiring, API/UI wiring, real AI extraction, Consent/Disclosure, RBAC/ABAC, client-safe projection, and recruiting.* source/packet cleanup remain future work.
- Task 6D or later must define the real canonical write flow from gated claims/review evidence into CandidateProfile; Task 6C only adds the transaction coordination that future work can use.
