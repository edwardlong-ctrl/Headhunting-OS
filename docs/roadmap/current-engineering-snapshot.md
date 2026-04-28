# Current Engineering Snapshot

This file contains mutable short-term engineering state. Update it after future successful main merges.

## Current Main Baseline

- main HEAD: `ebbdfad5c6f68ef74d0aea155fc2c7cdaefbc1fc`
- latest commit: Add client-safe projection privacy contracts
- validation: 336 tests, 0 failures, 0 errors, 1 skipped
- main status: clean

## Completed Major Tasks

- Task 0: Source of Truth ✅
- Task 1: Production Skeleton ✅
- Task 2: Truth Layer Foundation ✅
- Task 3: Truth Layer Persistence & Domain Services ✅
- Task 3F: Roadmap / status / known gaps materialized ✅
- Task 4: WorkflowEvent / Audit Foundation ✅
- Task 5: Governed Intake Minimal Slice ✅
- Task 6: Candidate Canonical Profile minimal slice ✅
- Task 7A: Client-safe projection contract / policy / vocabulary ✅

## Current Truth/Kernel Capabilities

- ClaimLedger append persistence exists.
- ReviewEvent append persistence exists.
- WorkflowEvent append/audit foundation exists.
- CanonicalWriteGate exists and must be used before canonical writes.
- CanonicalWriteService boundary exists.
- CandidateProfile minimal canonical field write exists through the gated transaction path.
- CandidateProfile lineage/stale/conflict metadata persistence exists.
- `ClientSafeCandidateCard` now exists as a backend-only anonymous contract.
- `ClientVisibleCandidateFieldPolicy` now denies forbidden and unknown client-visible candidate fields.
- `RedactionLevel` now defines L0-L4 vocabulary, with L4 separated from anonymous client-safe card exposure.

## Current Known Gaps

- Task 7 Client-safe Projection & Privacy Boundary is in progress; only Task 7A is complete.
- No re-identification risk scorer yet.
- No client-safe projection read model yet.
- No projection service yet.
- No API/controller/UI yet.
- No RBAC/ABAC yet.
- No Consent/Disclosure/Unlock implementation yet.
- No real redaction pipeline or automatic text rewriting yet.
- No real AI extraction/model wiring yet.
- No workflow engine or transition legality validation yet.
- No stale detection engine.
- No conflict resolution workflow.
- No full CandidateProfile engine.
- Blocked canonical attempts still have no separate persisted audit ledger.
- recruiting.* source/packet cleanup/deprecation remains deferred.

## Current Leftover Cleanup Note

Task 6F worktree was not deleted because it contains an untracked Vim swap file:

`/Users/edwardlong/.codex/worktrees/fc08/New project/services/core-api/src/test/java/com/recruitingtransactionos/coreapi/candidateprofile/.CandidateProfileContractTest.java.swp`

This worktree has already been merged to main, but cleanup was safely skipped. Do not force-delete it. Close the relevant Vim/editor/session and confirm recovery is unnecessary before deleting the swap file/worktree.

## Next Recommended Task

Task 7B: Client-safe projection service/read-model skeleton without API/UI exposure

## Future Prompt Strategy

Future Codex prompts should normally include only:

- task name
- current main HEAD
- instruction to read `docs/roadmap/codex-task-operating-rules.md`
- instruction to read `docs/roadmap/current-engineering-snapshot.md`
- local task-relevant files to inspect
- goal
- forbidden scope
- validation commands if not already referenced
- final report requirements if special additions are needed
