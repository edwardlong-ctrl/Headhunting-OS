# Known Gaps

## Canonical Persistence Deferred

- `CanonicalWriteService` is a gate/audit boundary, not a profile writer.
- `canonicalPersistencePerformed=false` is intentional.
- `recruiting.candidate` and `recruiting.candidate_profile` exist in V2, but Task 3D intentionally did not implement CandidateProfile writes.
- Task 6A now defines CandidateProfile domain contracts and field vocabulary, but still does not implement CandidateProfile persistence.
- `CanonicalWriteService` still does not write CandidateProfile, and Task 6A does not call `CanonicalWriteService`.
- Task 6B or later must implement CandidateProfile persistence only after the transaction boundary hardening plan is clear.

## Transaction Boundary Skeleton

- `CanonicalWriteTransactionBoundary` is currently a skeleton/abstraction.
- It does not provide real JDBC rollback coordination.
- Do not rely on it for multi-table atomicity yet.
- This remains true after Task 6A; no canonical profile write should rely on this boundary until it is hardened.

## CandidateProfile Contract Exists; Writes Deferred

- Task 6A adds pure backend-owned CandidateProfile contract vocabulary only.
- The contract covers `CandidateProfile`, `CandidateProfileId`, `CandidateId`, `CandidateProfileVersion`, `CandidateProfileField`, `CandidateProfileFieldPath`, `CandidateProfileFieldStatus`, source lineage references, conflict metadata, and staleness metadata.
- Field statuses now include `AI_EXTRACTED`, `HUMAN_ACKNOWLEDGED`, `CONSULTANT_ATTESTED`, `CANDIDATE_CONFIRMED`, `EXTERNAL_VERIFIED`, `SYSTEM_INFERENCE`, `CONFLICTING`, `NEEDS_CONFIRMATION`, `STALE`, `UNVERIFIED`, and `LIKELY_CURRENT`.
- Bulk approve remains capped at `HUMAN_ACKNOWLEDGED`; it must not produce `CANDIDATE_CONFIRMED` or `EXTERNAL_VERIFIED`.
- `SYSTEM_INFERENCE` remains forbidden as fact and internal hint only.
- `CONFLICTING` must block overwrite/client-visible verified fact statements in later tasks.
- `NEEDS_CONFIRMATION` must block shortlist/consent/disclosure readiness in later tasks.
- Source lineage references support auditability only; ClaimLedgerItem, ReviewEvent, SourceItem, InformationPacket, IntakeExtractionRun, and WorkflowEvent references are not proof by themselves.
- ClaimLedger/ReviewEvent/GovernedIntake remain upstream evidence/claims and do not directly write CandidateProfile.
- No CandidateProfile repository, JDBC adapter, REST/API/controller/DTO, UI, client-safe projection, redaction, RBAC/ABAC, Consent/Disclosure, AI model wiring, or real AI extraction exists after Task 6A.

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
- Real canonical persistence from governed intake remains future Task 6+ work.
- No default-placeholder business ClaimLedger append from intake exists.
- Governed intake CanonicalWrite boundary attempts exist only as a Task 5E gate/audit skeleton.
- No CandidateProfile persistence exists from intake; Task 6A contracts do not change that.
- No API/UI exposure exists for governed intake.
- No Consent/Disclosure, RBAC/ABAC, Client-safe projection, redaction, unlock/disclosure, or client exposure exists for governed intake.
- Task 5 Governed Intake Minimal Slice is closed as a safe, regression-covered backend chain only. CandidateProfile persistence begins in Task 6+ and downstream privacy/access surfaces remain future work.

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
- No CandidateProfile canonical persistence exists.
