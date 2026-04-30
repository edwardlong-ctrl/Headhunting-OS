# Task 21: Real AI Task Runner v1

## Branch / Worktree Hygiene
- repo path: /Users/edwardlong/Documents/New project
- current main HEAD: d463426
- mode: worktree (do NOT modify main directly)
- commit allowed: yes (single focused commit in worktree after validation)
- merge: no (merge will be done from the main session after review)

## Operating Rules
Read `docs/roadmap/codex-task-operating-rules.md` before any implementation.
Read `docs/roadmap/current-engineering-snapshot.md` for current baseline.

---

## Confirmed Repo Facts (verified on main d463426)

### AITaskRun Metadata/Goverance (Task 10A/B/C — DONE)

1. **AITaskRunService** (`truthlayer/service/AITaskRunService.java`):
   - `append(AITaskRunAppendCommand)` — validates via `AITaskGovernancePolicy.decide()`, then persists.
   - `findById(UUID, AITaskRunId)` — readback only.
   - **No execution, no model calls, no queue, no retry.**

2. **AITaskGovernancePolicy** (`truthlayer/service/AITaskGovernancePolicy.java`):
   - Production-grade, deterministic, fail-closed. ~200 lines covering all write-back target × human review status × actor combinations.
   - Enforces: no AI self-approval, canonical targets require APPROVED human review + canonical gate, commercial/placement targets blocked, consent/disclosure future-gated.
   - **Must NOT be weakened or bypassed by Task 21.**

3. **AITaskRunPort / JdbcAITaskRunPort** (`truthlayer/port/`, `truthlayer/persistence/`):
   - Append-only: `append()` upserts `ai_task_definition` then inserts `ai_task_run`.
   - Readback: `findById()` JOINs definition to get task_key.
   - **No update method exists.** Task 21 will need to add status update methods.

4. **AITaskRunAppendCommand / AITaskRunRecord** (port layer):
   - Fields: organizationId, taskName, taskVersion, inputSchemaVersion, outputSchemaVersion, promptVersion, model (ModelRef), status (AITaskRunStatus), humanReviewStatus, writeBackTarget (WriteBackTarget), requestedBy (ActorRef), correlationId, causationId, targetEntity (EntityRef), sourceReferenceIds, startedAt, completedAt, failureReason.
   - **Missing from Java records** (but present in DB schema and JSON schema): `toolCalls`, `costUnits`, `traceRef`, `errorCode`. These must be added.

5. **AITaskRunStatus enum**: `CREATED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED` — DB check constraint matches exactly.

6. **ModelRef** (`truthlayer/port/ModelRef.java`): record with `provider`, `name`, `version`. Currently always `("metadata-only", "no-model-call", "v0")` in tests.

### Database (PostgreSQL, Flyway at V13 — DO NOT ADD MIGRATION)

7. **`governance.ai_task_run`** (V2 + V7):
   - Columns present in schema but NOT in Java: `tool_calls` (jsonb), `cost_units` (bigint), `trace_ref` (text), `error_code` (text).
   - `model_routing_policy` (jsonb) in `ai_task_definition` — never populated.
   - FK from `ai_task_run` to `ai_task_definition`.
   - FK from `claim_ledger_item` to `ai_task_run` (already exists).
   - FK from `workflow_event` to `ai_task_run` (already exists).

8. **Latest migration is V13** (Task 20). V14 exists? → CHECK. **Task 21 must NOT add a migration unless a new column is strictly required. Prefer using existing columns.**

### Governed Intake / Extraction

9. **DeterministicIntakeExtractionService** (`governedintake/service/`):
   - Only mode: `DETERMINISTIC_PLACEHOLDER`. Output envelope explicitly states `real_ai_extraction_performed: false`.
   - Provides the **contract shape** for what a real extraction would produce.

10. **IntakeExtractionMode**: single value `DETERMINISTIC_PLACEHOLDER`. Needs at least an `AI` mode added.

11. **Intake bridge chain exists**: extraction → `IntakeClaimLedgerBridgeService` → `IntakeReviewBridgeService` → `IntakeCanonicalWriteBridgeService`. Task 21's AI output feeds into this chain.

### Contracts

12. **`packages/contracts/schemas/ai-task-run.schema.json`**: includes `tool_calls`, `cost_units`, `trace_ref`, `error_code`. Ahead of Java code.

### Tests

13. **Current test count**: 652 tests, 0 failures, 1 existing skip (from Task 18C baseline). **Task 21 must NOT reduce this count — only increase it.**
14. **Regression closure tests exist**: `AITaskGovernanceRegressionClosureTest`, `AITaskRunGovernanceContractTest`, `AITaskRunPostgresPersistenceIntegrationTest`, `AITaskWriteBackPolicyTest`. All must continue to pass.

---

## Mandated Outcomes (from roadmap Task 21)

1. **AI Provider Abstraction** — interface for model invocation. Must support at minimum one real provider (Anthropic-compatible endpoint, which DeepSeek uses). Deterministic/fake provider for tests. Provider selection driven by `ModelRef`.

2. **Model Routing** — map `ModelRef` (provider + name + version) to actual API endpoint + credentials. Config-driven, not hardcoded. Read from environment or config file, NEVER committed in source.

3. **Prompt Registry** — store prompt templates with versioning. Load from classpath resources (e.g. `prompts/` directory). Each prompt has a name, version, and template text. Support variable substitution for input parameters. Support schema version tags on the prompt file so the system can validate prompt version × input schema version compatibility.

4. **Input/Output Schema Validation** — validate that task input matches the declared `inputSchemaVersion`, and that AI output matches `outputSchemaVersion`. Schemas stored as JSON Schema files in `packages/contracts/schemas/`. Schema mismatch = task fails before model call.

5. **Task Execution Lifecycle** — implement the full status machine:
   - `CREATED` → `RUNNING` → `SUCCEEDED` or `FAILED`
   - `CREATED` → `CANCELLED` (before running)
   - Sync execution for v1 (no async queue — caller blocks). The task runner executes, updates status, and returns result.
   - Each status transition must produce a `WorkflowEvent` (via existing `WorkflowEventService` / `WorkflowTransitionAuditService`).

6. **Retry and Failure Policy** — failed tasks can be retried with a `correlationId` linking attempts. Max retries configurable. Failure reason must be safe (single-line, no stack traces, max 512 chars as per existing contract). Distinguish transient failures (retryable) from permanent failures (not retryable).

7. **Cost and Latency Logging** — populate `cost_units` and `trace_ref` on the `AITaskRunRecord`. Latency = `completedAt - startedAt`. Add these fields to the Java records (`AITaskRunAppendCommand`, `AITaskRunRecord`).

8. **Tool-call Metadata Logging** — if the AI model uses tools during execution, log tool calls (name + input/output summary) to `tool_calls` jsonb column. Add this field to Java records.

9. **Replay Support** — given a completed `AITaskRun`, be able to re-execute the same prompt with the same input and model, producing a new `AITaskRun` linked by `causationId`. Replay does NOT modify the original task run.

10. **Deterministic Test Provider** — a `FakeAiProvider` / `DeterministicAiProvider` that returns pre-configured responses for testing. Tests must NOT call real APIs.

11. **Write-back Target Enforcement** — after a successful AI execution, the output must route through the existing governance chain:
    - `CLAIM_LEDGER_PROPOSAL` → `IntakeClaimLedgerBridgeService`
    - `REVIEW_QUEUE`/`HUMAN_REVIEW_REQUIRED` → create review task
    - `CANONICAL_CANDIDATE_PROFILE` → requires APPROVED review + `CanonicalWriteGate` (Task 21 must NOT bypass this)
    - `NONE`/`NO_WRITE_BACK` → metadata-only, no further action
    The existing `AITaskGovernancePolicy` handles the "allowed/denied" decision. Task 21 adds the **execution** of the allowed path.

12. **Add UPDATE to AITaskRunPort** — currently append-only. Need `update()` method (or specific `updateStatus()`, `updateResult()`) to reflect execution outcomes.

13. **Add `error_code` field** to Java records (already in DB and JSON schema).

---

## Open Design Decisions (propose before implementing)

1. **AI Provider library** — Which Java library for AI model calls?
   - Option A: Spring AI (`org.springframework.ai`) — idiomatic for Spring Boot, provider abstraction built-in, supports Anthropic/OpenAI.
   - Option B: Direct HTTP client (`RestClient`/`WebClient`) to Anthropic-compatible endpoint — lighter, no extra dependency, but more boilerplate.
   - Recommendation: Spring AI for provider abstraction, or direct `WebClient` if dependency minimization is preferred.

2. **Prompt template format** — How to store and load prompts?
   - Option A: Markdown files with YAML frontmatter (like SKILL.md format) — name, version, input/output schema versions in frontmatter, body = prompt template.
   - Option B: JSON files — machine-readable but less human-friendly.
   - Recommendation: Markdown + YAML frontmatter, matching the existing skill/prompt conventions in this repo.

3. **Sync vs Async execution for v1** — Should the task runner block the caller?
   - Option A: Sync — simpler, no queue infrastructure, caller waits for completion.
   - Option B: Async with in-memory queue — non-blocking, but adds complexity.
   - Recommendation: Sync for v1. Async can come in a follow-up task.

4. **Model credentials** — How to provide API keys to the task runner?
   - MUST NOT be committed to source. Read from environment variables (`ANTHROPIC_API_KEY` style) or a config file excluded from git.
   - Recommendation: Environment variables, matching the existing `ANTHROPIC_AUTH_TOKEN` pattern.

5. **Scope of extraction mode** — Should Task 21 add `AI` extraction mode to `IntakeExtractionMode`?
   - The roadmap defers "Real AI Intake" to Tasks 21-24. Task 21 is the runner infrastructure. Task 22 is document intelligence specifically.
   - Recommendation: Add the mode enum value but defer the full extraction integration to Task 22.

6. **Which provider to implement first?**
   - The project uses DeepSeek V4 Pro via Anthropic-compatible endpoint. This should be the primary real provider.
   - Recommendation: One real provider (Anthropic-compatible) + one fake provider for tests.

---

## Forbidden Scope

- No new Flyway migration unless strictly required (prefer using existing `tool_calls`, `cost_units`, `trace_ref`, `error_code` columns).
- MUST NOT bypass `AITaskGovernancePolicy` — it remains the single decision point for write-back.
- MUST NOT bypass `CanonicalWriteGate` — canonical writes still go through the gate.
- No AI direct canonical writes.
- No prompt hardcoding in controllers or service classes — prompts come from the registry.
- No model credentials in source code or committed config.
- No AI self-approval for human-review-required targets (existing policy check; must be preserved).
- No frontend changes.
- No Client/Candidate/Owner/Admin endpoints.
- No real auth/session — header-based temporary access context remains the only mechanism.
- Do NOT touch files in `consentdisclosure/` or `identityaccess/` packages.
- Do NOT add async queue infrastructure (RabbitMQ, SQS, etc.) — v1 is sync.
- Do NOT add new model providers beyond Anthropic-compatible (the one DeepSeek uses).

---

## Validation

```sh
git diff --check
docker info
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

Full Maven test suite must pass. Report exact test count. **Must not reduce from 652.**

---

## Docs Closeout

Update after implementation:
- `docs/roadmap/current-engineering-snapshot.md` — update HEAD, test count, completed task entry, known gaps
- `docs/roadmap/implementation-status.md` — add Task 21 entry
- `docs/roadmap/known-gaps.md` — add Task 21 scope limitations (no async queue, no full extraction integration, etc.)

---

## Final Report Requirements

Must distinguish:
1. **Code completed** — files new/modified count
2. **Tests passed** — exact count, 0 failures required
3. **Docs updated** — which files
4. **Full suite run** — yes or no
5. **Known hardening gaps** — e.g., sync-only (no async queue), one provider only, no extraction integration, no retry backoff strategy, no circuit breaker; tool-call metadata is best-effort logging, not structured contract

Completion label: "AI Task Runner v1 baseline" — NOT "full AI execution platform".
