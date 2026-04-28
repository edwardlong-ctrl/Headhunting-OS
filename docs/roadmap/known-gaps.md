# Known Gaps

## Canonical Persistence Deferred

- `CanonicalWriteService` is a gate/audit boundary, not a profile writer.
- `canonicalPersistencePerformed=false` is intentional.
- `recruiting.candidate` and `recruiting.candidate_profile` exist in V2, but Task 3D intentionally did not implement CandidateProfile writes.
- Task 6A defines CandidateProfile domain contracts and field vocabulary.
- Task 6B implements a backend-internal CandidateProfile persistence skeleton, but it is not connected to governed intake or `CanonicalWriteService`.
- Task 6C hardens the canonical write transaction boundary, but `CanonicalWriteService` still does not write CandidateProfile.
- Task 6D or later must define the real canonical write flow from gated claims/review evidence into CandidateProfile.

## Transaction Boundary Hardened; Canonical Flow Still Deferred

- Task 6C replaces the no-op/skeleton transaction boundary with `SpringCanonicalWriteTransactionBoundary`.
- The boundary uses Spring `PlatformTransactionManager` and `TransactionTemplate` for real Spring/JDBC transaction coordination.
- Successful callback commit and failing callback rollback behavior are covered by focused unit and PostgreSQL/Testcontainers integration tests.
- Runtime callback failures propagate and roll back participating JDBC writes.
- Checked callback failures are explicitly wrapped in `CanonicalWriteTransactionException` and roll back participating JDBC writes.
- This is transaction coordination only. It is not the real canonical write flow and does not write CandidateProfile.
- `CanonicalWriteGate` remains mandatory before any canonical write flow can be added.

## CandidateProfile Persistence Skeleton Exists; Promotion Deferred

- Task 6A adds pure backend-owned CandidateProfile contract vocabulary.
- Task 6B adds a backend-internal `CandidateProfileService`, `CandidateProfilePersistencePort`, and `JdbcCandidateProfilePersistencePort`.
- Task 6B reuses the existing V2 `recruiting.candidate_profile` table and adds no new migration, table, index, client-facing view, API DTO table, or competing profile table.
- Task 6B stores field status wire values in `field_status_map`, field values/lineage/conflict/staleness documents in `metadata.candidate_profile_fields`, and source claim id summaries in `source_claim_ids`.
- Task 6B create/read/upsert operations are organization-scoped and require the candidate row to belong to the same organization before a profile row can be created.
- The contract covers `CandidateProfile`, `CandidateProfileId`, `CandidateId`, `CandidateProfileVersion`, `CandidateProfileField`, `CandidateProfileFieldPath`, `CandidateProfileFieldStatus`, source lineage references, conflict metadata, and staleness metadata.
- Field statuses now include `AI_EXTRACTED`, `HUMAN_ACKNOWLEDGED`, `CONSULTANT_ATTESTED`, `CANDIDATE_CONFIRMED`, `EXTERNAL_VERIFIED`, `SYSTEM_INFERENCE`, `CONFLICTING`, `NEEDS_CONFIRMATION`, `STALE`, `UNVERIFIED`, and `LIKELY_CURRENT`.
- Bulk approve remains capped at `HUMAN_ACKNOWLEDGED`; it must not produce `CANDIDATE_CONFIRMED` or `EXTERNAL_VERIFIED`.
- `SYSTEM_INFERENCE` remains forbidden as fact and internal hint only.
- `CONFLICTING` must block overwrite/client-visible verified fact statements in later tasks.
- `NEEDS_CONFIRMATION` must block shortlist/consent/disclosure readiness in later tasks.
- Source lineage references support auditability only; ClaimLedgerItem, ReviewEvent, SourceItem, InformationPacket, IntakeExtractionRun, and WorkflowEvent references are not proof by themselves.
- ClaimLedger/ReviewEvent/GovernedIntake remain upstream evidence/claims and still do not directly write CandidateProfile.
- `CanonicalWriteService` remains gate/audit only and still reports `canonicalPersistencePerformed=false`.
- Task 6C does not implement automatic ClaimLedgerItem-to-CandidateProfile promotion, does not mutate ClaimLedger verification status, does not mutate ReviewEvent, and does not treat ReviewEvent as fact promotion.
- No CandidateProfile REST/API/controller/DTO, UI, client-safe projection, redaction, RBAC/ABAC, Consent/Disclosure, AI model wiring, real AI extraction, governed-intake bridge write, or CanonicalWriteService write exists after Task 6C.

## Consent / Disclosure Not Implemented

- `ConsentRecord` and `DisclosureRecord` are expected by product spec but not yet implemented as behavior.
- Unlock/disclosure gate is not implemented.
- No identity disclosure behavior exists.
- Known alignment skip for consent/disclosure gap remains intentional until Task 12 or earlier dedicated workstream.

## AITaskRun / AI Governance Not Implemented

- No real AI model wiring.
- No AITaskRun persistence implementation yet.
- No prompt/model/schema version tracking yet.
- No `write_back_target` enforcement beyond current boundary concepts.

## Governed Intake Minimal Slice Closed; Downstream Work Deferred

- Task 5A now provides backend-owned `SourceItem` and `InformationPacket` contracts, persistence ports/adapters, and `intake.source_item`, `intake.information_packet`, and `intake.information_packet_source_item` tables.
- Task 5B now provides backend-owned `IntakeExtractionRun` and `IntakeExtractionOutputEnvelope` contracts, a deterministic placeholder extractor, a narrow extraction-run persistence port/adapter, and `intake.extraction_run`.
- Task 5B extraction output is stored as JSONB in `intake.extraction_run.output_json`.
- Task 5B performs no real AI extraction, no LLM call, no OCR/STT/file conversion, and no semantic parsing.
- Task 5B extraction output is an intermediate envelope only. It is not canonical fact storage, ClaimLedger, ReviewEvent, CandidateProfile persistence, client-safe projection, or CanonicalWrite output.
- Task 5B explicitly sets output-envelope guardrail fields such as `real_ai_extraction_performed=false`, `claim_ledger_append_allowed=false`, and `canonical_write_allowed=false`.
- Task 5B leaves `InformationPacket.processingStatus` updates intentionally deferred to a future governed-intake lifecycle task.
- Task 5C now provides a backend-owned extraction-output to ClaimLedger bridge skeleton.
- Task 5C reads `intake.extraction_run` output envelopes and validates `intake.information_packet` lineage before appending through `ClaimLedgerService`.
- Task 5C default deterministic placeholder output creates no fake business claims and returns no ClaimLedger append because placeholder metadata is not bridge-eligible.
- Task 5C only appends explicitly marked operational `CLAIM_CANDIDATE` fixture fields under `intake.bridge_eligible.*`; these records are internal-only claims, not canonical candidate/company/job facts.
- Task 5C `ClaimLedgerItem` rows remain claims, not facts; ClaimLedger-to-canonical promotion is still blocked by future review/gate work.
- Task 5C adds V6 `claim_ledger_org_source_span_idx` for narrow source-span idempotency lookup and deterministic duplicate suppression.
- Task 5D now provides a backend-owned governed-intake ClaimLedger-to-ReviewEvent bridge skeleton.
- Task 5D reads only `governance.claim_ledger_item` records by organization-scoped claim id, requires Task 5C governed-intake `intake.*` lineage, and appends review evidence only through `ReviewEventService`.
- Task 5D ReviewEvent append is review evidence, not fact promotion. It does not mutate ClaimLedger verification status, does not call CanonicalWrite, and does not write CandidateProfile.
- Task 5D stores claim lineage in `governance.review_event.claim_ledger_item_id` and deterministic `source_span_ref` values. It added no new migration or table.
- Task 5D duplicate behavior is deterministic: identical bridge requests return the existing review event id; materially different review evidence is allowed as a new review event.
- Task 5E now provides a backend-owned governed-intake ClaimLedgerItem-plus-ReviewEvent to CanonicalWrite boundary integration skeleton.
- Task 5E reads only exact organization-scoped `ClaimLedgerItem` and `ReviewEvent` rows through narrow lookup ports, validates that the ReviewEvent belongs to the ClaimLedgerItem, requires Task 5C/5D governed-intake `intake.*` lineage, and calls only `CanonicalWriteService`.
- Task 5E keeps `CanonicalWriteGate` mandatory and does not bypass it. Current Task 5C low-authority governed-intake claims remain blocked by the existing gate rather than promoted to fact.
- Task 5E allowed boundary attempts, where gate-allowable governed-intake-lineage fixtures are used, append only the existing `WorkflowEvent` audit from `CanonicalWriteService` and still report `canonicalPersistencePerformed=false`.
- Task 5E does not mutate ClaimLedger verification status, does not mutate ReviewEvent, does not write CandidateProfile, does not write raw Candidate/Profile persistence, does not query business target entities, and does not implement API/UI exposure.
- Task 5E adds no new migration, table, index, or API-facing view. It relies on existing `governance.claim_ledger_item`, `governance.review_event`, and `workflow.workflow_event` audit/idempotency behavior.
- Task 5E duplicate behavior is deterministic for allowed boundary audits through existing WorkflowEvent idempotency. Gate-blocked attempts append no audit row under the current CanonicalWriteService design, so there is no DB-enforced blocked-attempt ledger yet.
- Task 5F now regression-covers the full safe minimal chain from `SourceItem` / `InformationPacket` through deterministic extraction output envelope, ClaimLedgerItem claim, ReviewEvent evidence, CanonicalWriteService boundary attempt, CanonicalWriteGate decision, and no canonical persistence.
- Task 5F verifies default placeholder output appends no business ClaimLedger claims, bridge-eligible fixtures append claims but not facts, ReviewEvent remains evidence rather than fact promotion, CanonicalWriteGate is mandatory, allowed boundary fixtures still report `canonicalPersistencePerformed=false`, and blocked canonical attempts still have no separate persisted audit ledger.
- These Task 5A `intake.*` governed-intake operational records coexist with earlier V2 skeleton schema artifacts: `recruiting.source_item` and `recruiting.information_packet`.
- `SourceItem` and `InformationPacket` are intake/provenance records, not canonical facts.
- Neither the Task 5A `intake.*` table family nor the earlier V2 `recruiting.*` source/packet table family is canonical fact storage, CandidateProfile persistence, ClaimLedger, or a canonical profile.
- For the Task 5C, Task 5D, and Task 5E bridges, `intake.*` is the operational governed-intake source. Earlier `recruiting.source_item` and `recruiting.information_packet` remain V2 skeleton artifacts and are not read or written by these bridges.
- Future cleanup, deprecation, or migration of the earlier `recruiting.*` source/packet skeleton remains a schema cleanup gap.
- No real AI extraction exists yet.
- Real canonical persistence from governed intake remains future Task 6D or later work.
- No default-placeholder business ClaimLedger append from intake exists.
- Governed intake CanonicalWrite boundary attempts exist only as a Task 5E gate/audit skeleton.
- No CandidateProfile persistence exists from intake; Task 6B backend-internal persistence and Task 6C transaction hardening do not change that.
- No API/UI exposure exists for governed intake.
- No Consent/Disclosure, RBAC/ABAC, Client-safe projection, redaction, unlock/disclosure, or client exposure exists for governed intake.
- Task 5 Governed Intake Minimal Slice is closed as a safe, regression-covered backend chain only. CandidateProfile persistence exists as a backend-internal Task 6B skeleton, while governed-intake write wiring and downstream privacy/access surfaces remain future work.

## Client-safe Projection Not Implemented

- Raw Candidate must never be exposed to Client.
- `ClientSafeCandidateCard` does not exist yet.
- Redaction L0-L4 does not exist yet.
- Re-identification risk scorer does not exist yet.

## Identity / RBAC / ABAC Not Implemented

- Five portal route shells exist, but backend role/permission enforcement is not production-ready.
- Field-level access control remains future work.

## Workflow Engine Not Implemented

- WorkflowEvent append exists.
- Workflow action vocabulary and audit policy exist after Task 4A.
- WorkflowEvent idempotency, correlation, and causation guardrails exist after Task 4B.
- WorkflowEvent audit query/read model skeleton exists after Task 4C.
- Workflow transition audit skeleton exists after Task 4D.
- Task 4D records transition audit events with `before_state` and `after_state` through the existing `WorkflowEvent` append boundary.
- Task 4D is backend-internal only.
- Workflow action audit still exists only at append-boundary validation level.
- No state machine.
- No transition legality validation.
- No entity-state lookup or mutation is performed by WorkflowEvent policy validation.
- No entity-state lookup or mutation is performed by `WorkflowTransitionAuditService`.
- No SLA/automation workflow engine.
- Full workflow engine remains future work.
- Task 5 governed-intake minimal slice exists, but the broader Governed Intake workflow engine remains future work.

## Workflow Read Model Remaining Gaps

- Task 4C adds a backend-internal, read-only audit query/read model for `WorkflowEvent`.
- It is not an API/controller layer.
- It is not UI integration.
- It is not a client-safe projection.
- It does not expose raw Candidate/Profile payloads or business entity internals.
- It does not join Candidate, Company, Job, Consent, Disclosure, Placement, or Commission tables.
- It is not dashboard analytics, full reporting, full-text search, generic repository search, or arbitrary SQL filtering.
- Correlation and causation identifiers are queryable for backend audit lineage, but no user-facing timeline/query API exists yet.

## API Boundary Not Implemented

- No REST/API DTO layer for truth layer.
- No client-safe response contract.
- No controller boundary tests.

## UI / AI / Access Boundaries Not Implemented

- No UI integration exists for WorkflowEvent audit guardrails.
- No real AI model wiring exists for workflow actions.
- No API/controller/UI integration exists for governed intake.
- No API/controller/UI integration exists for CandidateProfile.
- No Consent/Disclosure behavior exists.
- No RBAC/ABAC implementation exists.
- No Client-safe projection or redaction behavior exists.
- No governed-intake or CanonicalWriteService-driven CandidateProfile canonical write flow exists.
- Blocked canonical attempts still have no separate persisted audit ledger.
