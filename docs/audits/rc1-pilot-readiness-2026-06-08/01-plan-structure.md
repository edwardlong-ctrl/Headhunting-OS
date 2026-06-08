# RC1 Pilot Readiness Audit - Plan Structure And Execution Quality

## Scope And Evidence Read

Scope: plan structure and execution quality for RC1 pilot readiness. This audit did not execute product gates and did not inspect implementation code beyond the plan/spec documents named below.

Evidence read:

- `docs/release/RC1-pilot-readiness-plan.md`
- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/specs/v2.0/product-spec-v2.0.md` for UI preservation and baseline context
- `docs/release/release-checklist.md` because the RC1 plan lists it as a source of truth at `docs/release/RC1-pilot-readiness-plan.md:33`

Direct answers:

- RC1/pilot scope is mostly crisp enough to guide engineering. The plan names one success exit, a full golden path, explicit included/excluded scope, and a decision ladder.
- Phase ordering is mostly dependency-driven, but `RC1-08A passed-or-manual-gap-recorded -> RC1-08` creates an ambiguous path around the highest-risk commercial-closure gap.
- Acceptance criteria are strong at the final decision level, but owner, deliverable, evidence-row, rollback, and signoff metadata are not consistently concrete at each task level.
- The plan does distinguish local acceptance, RC1 pilot readiness, production go-live, and acquisition/commercial readiness, but it should add a short term matrix because the governing spec is production-first while RC1 is controlled-pilot scoped.
- Another engineer could execute the plan, but would need extra context for accountability, waiver authority, rollback conditions, and exact evidence artifact standards.

## Severity-Ranked Findings

### 1. Severity: P1 - Commercial-closure dependency has an ambiguous continue path

Observed evidence:

- The dependency graph allows `RC1-08A passed-or-manual-gap-recorded -> RC1-08` at `docs/release/RC1-pilot-readiness-plan.md:207`.
- The plan correctly states that current Task 42 browser flows stop before placement, commission, owner revenue, and accounting handoff, and that service/source evidence alone cannot satisfy RC1 at `docs/release/RC1-pilot-readiness-plan.md:1030`.
- `RC1-08A` says `passed` requires runtime/API plus persistence/audit evidence, while service/source evidence is only `partial` or `not-verified` at `docs/release/RC1-pilot-readiness-plan.md:1053-1081`.
- `RC1-09` says it may run after `RC1-08A` is `passed` or has a clearly recorded manual-evidence gap, but if `RC1-08A` is partial, `RC1-09` must close the placement/commission runtime plus persistence/audit gap before final readiness at `docs/release/RC1-pilot-readiness-plan.md:1092`.
- Final readiness still requires placement/commission and owner/admin audit evidence at `docs/release/RC1-pilot-readiness-plan.md:1294-1307`.

Inference:

The plan is logically strict at final signoff, but the mid-plan transition can let a worker proceed to the full release gate with a "manual gap recorded" state that is not itself a pass. That is not a product readiness bug, but it is an execution-quality risk: the next worker may treat a recorded gap as permission to keep moving instead of as a blocking branch that must be closed by manual/runtime evidence or converted into a `not-ready-insufficient-evidence` decision.

Likely cause:

`RC1-08A` was inserted to close a known coverage gap while preserving historical `RC1-08` numbering, so the dependency graph tries to accommodate both full release regression and commercial-closure evidence in one linear queue.

Recommended change:

Replace `passed-or-manual-gap-recorded` with explicit branch states:

- `RC1-08A passed -> RC1-08`
- `RC1-08A runtime-gap-recorded -> RC1-09-commercial-closure-runtime`
- `RC1-08A partial/not-verified -> stop or final decision cannot exceed not-ready-insufficient-evidence unless RC1-09 closes the gap`

Also define `manual-gap-recorded` as a non-pass state with required fields: missing runtime action, missing persistence/audit proof, owner, next command/manual step, and allowed final decision.

### 2. Severity: P1 - Per-task owners, approvers, rollback paths, and waiver authority are incomplete

Observed evidence:

- The release checklist has an owner column for each gate at `docs/release/release-checklist.md:7-17`.
- The release checklist requires signed risk acceptance to include owner, reason, expiration date, and rollback condition at `docs/release/release-checklist.md:21-27`.
- The RC1 plan's artifact responsibilities name documents and artifact directories, not accountable human or role owners, at `docs/release/RC1-pilot-readiness-plan.md:144-149`.
- The subagent return shape includes status, task, files changed, commands run, evidence rows, capability rows, blockers, and next recommended task, but not owner, approver, rollback condition, or signoff authority at `docs/release/RC1-pilot-readiness-plan.md:177-188`.
- Individual tasks list files and commands, for example `RC1-02` at `docs/release/RC1-pilot-readiness-plan.md:502-546` and `RC1-08A` at `docs/release/RC1-pilot-readiness-plan.md:1017-1081`, but do not name accountable owners or rollback paths.

Inference:

An engineer can run many commands from the plan, but cannot reliably tell who owns a gate, who can approve a waiver, what rollback condition applies, or which role must sign off a blocker classification. This matters most for privacy/security, AI governance, browser E2E waivers, disposable database approval, and final RC1 decision authority.

Likely cause:

The plan is optimized as a Codex task queue and evidence-gathering script, while the release checklist carries release-management metadata in a separate compact table.

Recommended change:

Add a "Gate Metadata Matrix" near the dependency graph with columns: task, owner role, approver/signoff role, deliverable, rollback/cleanup path, waiver allowed, waiver authority, exit gate, and next task. Reuse the release-checklist owners for regression gates and add explicit RC1 owners for commercial closure, manual golden path, and final decision.

### 3. Severity: P2 - Evidence capture is strict in principle but not standardized enough per task

Observed evidence:

- The plan states that no gate is passed unless the exact command exits `0` at `docs/release/RC1-pilot-readiness-plan.md:110`.
- The release checklist requires exact commands, timestamps, test reports or logs, browser startup signals, and signed risk acceptance metadata before signoff at `docs/release/release-checklist.md:21-27`.
- The initial `Gate Evidence` template has columns for gate, command, status, evidence, blocker, and next action, but not run timestamp, exit code, artifact path, evidence level, or operator at `docs/release/RC1-pilot-readiness-plan.md:325-342`.
- Some tasks ask for timestamps or artifact excerpts, such as Docker evidence at `docs/release/RC1-pilot-readiness-plan.md:536-538`, pilot data at `docs/release/RC1-pilot-readiness-plan.md:703-710`, and release gate evidence at `docs/release/RC1-pilot-readiness-plan.md:1003-1009`, but that shape is not uniform across all gates.
- The evidence strength model is useful and explicit at `docs/release/RC1-pilot-readiness-plan.md:123-142`, but the per-gate evidence table does not require the evidence level as a field.

Inference:

The plan is likely to produce evidence, but different workers may record different evidence granularity. That weakens reviewability and makes it easier for a "passed" row to lack the timestamp, exit code, artifact path, or E-level needed for a later readiness decision.

Likely cause:

The plan evolved from task-specific instructions, so stronger evidence details appear in some tasks but not in the shared template.

Recommended change:

Change `Gate Evidence` columns to: gate, owner, command, run at, exit code, status, evidence level, evidence artifact, blocker, rollback/cleanup, next action. Require every task's "Record result" step to fill the same fields.

### 4. Severity: P2 - Local, RC1 pilot, production, and commercial readiness boundaries are good but need a glossary-style guardrail

Observed evidence:

- Current spec says v2.1 is the source of truth, v2.0 UI is preserved, and the project is "production-first, not demo-first and not MVP-first" at `docs/specs/CURRENT_SPEC.md:3-11`.
- v2.1 says the full delivery plan is not MVP or demo and each phase must serve the final architecture at `docs/specs/v2.1/product-spec-v2.1.md:841-843`.
- The RC1 plan excludes public SaaS launch, managed cloud signoff, certification, customer go-live approval, production delivery, live external AI provider activation, large features/refactors, pricing, billing, sales packaging, and acquisition positioning at `docs/release/RC1-pilot-readiness-plan.md:60-69`.
- The RC1 decision ladder separates local gates, automated pilot flow, commercial closure, and operator-ready controlled pilot at `docs/release/RC1-pilot-readiness-plan.md:97-106`.
- The final decision options are limited to controlled-pilot readiness or specific not-ready states at `docs/release/RC1-pilot-readiness-plan.md:1314-1321`.

Inference:

The plan largely answers the readiness-boundary question correctly. The remaining risk is communication: a worker or reviewer could read the production-first spec language and overstate RC1 evidence as production go-live or commercial/acquisition readiness unless the RC1 plan repeats the term boundaries in one visible matrix.

Likely cause:

The plan already has exclusions and a decision ladder, but they are separated from the spec's production-first language and final memo template.

Recommended change:

Add a "Readiness Term Boundaries" table near the human summary:

- Local acceptance: local commands and deterministic evidence passed.
- RC1 controlled pilot readiness: full synthetic transaction through placement, commission, and audit has current evidence and no P0 operator blocker.
- Production go-live: out of scope; requires managed deployment, live providers, operational monitoring, customer go-live approval, and external compliance work.
- Commercial/acquisition readiness: out of scope; requires pricing, pilots/customers, sales packaging, deployment proof, buyer narrative, and commercial diligence.

### 5. Severity: P2 - Spec coverage is claimed, but there is no traceability matrix from v2.1 acceptance categories to RC1 gates

Observed evidence:

- The RC1 plan lists product invariants at `docs/release/RC1-pilot-readiness-plan.md:38-47`.
- The v2.1 acceptance criteria include five portals, AI intake, AI policy, matching, shortlist, consent/disclosure, workflow, industry packs, audit/governance, and v2.1 governance additions at `docs/specs/v2.1/product-spec-v2.1.md:901-925`.
- The plan self-review says spec coverage includes v2.1 source of truth, backend truth ownership, AI claim boundary, PostgreSQL, WorkflowEvent, client disclosure boundary, release checklist gates, pilot E2E, manual golden path, and capability mapping at `docs/release/RC1-pilot-readiness-plan.md:1452-1457`.

Inference:

The plan covers the right areas, but the coverage claim is not mechanically reviewable. Another engineer cannot quickly tell which v2.1 acceptance categories are required for RC1, which are sampled through gates, and which are explicitly out of RC1 scope.

Likely cause:

The plan uses a golden-path queue rather than a spec-to-gate traceability table.

Recommended change:

Add a compact traceability matrix: v2.1 acceptance category, RC1 relevance, RC1 task/gate, minimum evidence level, out-of-scope note if applicable, and final evidence row. This will make the plan executable without asking the original planner which spec clauses matter for RC1.

## Missing Evidence / Not Verified

- Product gate commands were not run in this audit. No current pass/fail claim is made for backend, frontend, migrations, privacy/security, AI eval, Playwright, or release gate behavior.
- Implementation code was not audited for whether plan commands, file paths, or test names are current.
- The RC1 plan file is currently untracked in the working tree; this audit treats it as the document under review and does not infer readiness from git tracking state.
- I did not verify whether `RC1-pilot-readiness-evidence.md`, `RC1-capability-map.md`, or `docs/release/rc1-artifacts/` already exist outside the plan template.
- I did not verify current Docker, Testcontainers, local PostgreSQL, ports, or browser availability.

## Concrete Patch Suggestions For The RC1 Plan

### Patch Suggestion 1 - Replace the ambiguous dependency branch

```markdown
RC1-07 passed -> RC1-08A
RC1-08A passed -> RC1-08
RC1-08A runtime-gap-recorded -> RC1-09-commercial-closure-runtime
RC1-08A partial-or-not-verified -> stop, or proceed only to a final `not-ready-insufficient-evidence` decision unless RC1-09 closes the runtime plus persistence/audit gap
RC1-08 passed -> RC1-09
RC1-09 complete -> RC1-10 -> RC1-11
```

Add:

```markdown
`manual-gap-recorded` is not a pass. It must include the missing runtime action, missing persistence/audit proof, owner, next manual step, and the highest allowed final decision. It cannot support `ready-for-controlled-pilot`.
```

### Patch Suggestion 2 - Add a gate metadata matrix

```markdown
## Gate Metadata Matrix

| Task | Owner | Approver / Signoff | Deliverable | Rollback / Cleanup | Waiver Allowed | Exit Gate | Next Task |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RC1-02 Docker/Testcontainers | Engineering / Release owner | Release owner | Evidence row plus blocker artifact if failed | None; repair local Docker and rerun | No readiness waiver | Docker client/server reachable | RC1-03 |
| RC1-06 Privacy/Security and AI Eval | Security owner / AI governance owner | Release owner | Evidence rows plus selected-test/eval summary | Revert only the minimal fix task if introduced later | No privacy leakage waiver | Both commands exit 0 | RC1-07 |
| RC1-08A Commercial Closure | Release owner / Product owner | Product owner | Runtime/API plus persistence/audit proof for placement and commission | Reset synthetic data or record intentional transaction mutations | No pilot-readiness waiver | Passed or explicit not-ready branch | RC1-08 or RC1-09 branch |
```

### Patch Suggestion 3 - Standardize evidence rows

```markdown
| Gate | Owner | Command | Run At | Exit Code | Status | Evidence Level | Evidence Artifact | Blocker | Rollback / Cleanup | Next Action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
```

Instruction:

```markdown
Every task's "Record result" step must fill all fields. A `passed` status without run time, exit code, evidence level, and artifact or exact evidence excerpt is invalid.
```

### Patch Suggestion 4 - Add readiness term boundaries

```markdown
## Readiness Term Boundaries

| Term | What It Means In This Plan | What It Does Not Mean |
| --- | --- | --- |
| Local acceptance | Required local commands and deterministic synthetic evidence pass on the evaluated checkout. | Production deployment or customer go-live. |
| RC1 controlled pilot readiness | One synthetic transaction is proven from intake through placement, commission, and audit trace, with no P0 operator blocker. | Public SaaS launch, managed cloud signoff, SOC 2/ISO, live provider activation, or customer go-live approval. |
| Production go-live readiness | Out of RC1 scope; requires deployment, live-provider, monitoring, support, compliance, rollback, and customer approval evidence. | A claim supported by local gates alone. |
| Commercial/acquisition readiness | Out of RC1 scope; requires pricing, packaging, pilot/customer traction, deployment proof, ROI evidence, and diligence materials. | A claim supported by code/spec depth alone. |
```

### Patch Suggestion 5 - Add v2.1 traceability

```markdown
## v2.1 Acceptance Traceability For RC1

| v2.1 Acceptance Category | RC1 Relevance | RC1 Gate / Task | Minimum Evidence | Out Of Scope Note |
| --- | --- | --- | --- | --- |
| Five portals | Required where touched by the golden path | RC1-04, RC1-07, RC1-09 | E4/E5 for operated surfaces | Full portal completeness outside golden path is not proven by RC1. |
| Consent & Disclosure | P0 invariant | RC1-06, RC1-07, RC1-09, RC1-10 | E1 plus E3/E4 and audit/persistence evidence | No waiver for raw Candidate leakage or unlock bypass. |
| Placement/Commission | Required for RC1 success | RC1-08A, RC1-09 | E3 plus persistence/audit evidence | Service tests alone are partial. |
| Industry Packs | Sample only if used by the pilot transaction | RC1-07, RC1-10 | Evidence level matching actual use | Full 8-pack production calibration is not RC1 scope. |
```

## Top 5 Improvement Actions

1. Replace `passed-or-manual-gap-recorded` with explicit pass, runtime-gap, and not-ready branches.
2. Add a gate metadata matrix with owner, approver, deliverable, rollback/cleanup, waiver, exit gate, and next task.
3. Standardize evidence rows across every task with run time, exit code, evidence level, artifact path, and operator/owner.
4. Add a readiness-boundary glossary separating local acceptance, RC1 controlled pilot readiness, production go-live, and commercial/acquisition readiness.
5. Add a v2.1 acceptance traceability matrix so another engineer can see which spec clauses are required, sampled, or out of scope for RC1.
