# Codex Task Operating Rules

## Purpose

This file is the short, stable operating rule layer for Codex tasks — AI behavior constraints that must be followed during implementation.

Future task prompts should reference this file instead of repeating the same long project rules.

For prompt authoring rules (how to write a task prompt), see `docs/prompts/PROMPT_WRITING_GUIDE.md`.

## Product and Architecture Invariants

- v2.1 is the current product source of truth.
- v2.0 UI / portal definitions must be preserved.
- Consultant is one unified portal.
- This project is production-first, not demo-first and not MVP-first.
- Backend owns truth.
- PostgreSQL is the target source of truth.
- AI outputs claims, not facts.
- Raw input is not fact.
- Extraction output is not fact.
- ClaimLedgerItem is claim, not fact.
- ReviewEvent is review evidence, not fact promotion.
- Claim Ledger comes before canonical fact.
- Risk-tiered human review comes before canonical write.
- CanonicalWriteGate must not be bypassed.
- Client must never read raw Candidate or raw CandidateProfile before unlock/disclosure.
- WorkflowEventService / WorkflowTransitionAuditService are audit boundaries, not workflow engines.
- Bulk approve cannot produce CANDIDATE_CONFIRMED or EXTERNAL_VERIFIED.

## Task Sizing Strategy

- Do not mechanically split every major task into A/B/C/D/E/F.
- Use risk-based task sizing.
- Low-risk tasks may combine contract + policy + unit tests + docs.
- Medium-risk tasks should usually be 2-3 subtasks.
- High-risk tasks must remain small-step.
- High-risk areas include RBAC/ABAC, Consent/Disclosure/Unlock, client-facing API boundary, identity disclosure, commercial/placement/commission logic, canonical fact writes, and anything that exposes candidate data to clients.

## Codex Execution Rules

- Work only in the current task worktree.
- Do not merge to main.
- Do not fast-forward main.
- Do not clean up worktrees unless explicitly asked.
- If the worktree is detached HEAD, committing on detached HEAD is acceptable.
- Produce one focused commit only after validation passes.
- If validation fails, report failure clearly and do not merge.
- Never weaken existing safety gates or tests to make a task pass.
- Do not rewrite product specs unless the task explicitly asks.
- Do not introduce broad scope creep.

## Testing Rules

- Negative tests must come first for boundary-sensitive tasks (privacy, org-scope isolation, auth, disclosure, canonical write, idempotency).
- Write the failing negative test before the implementation that makes it pass.
- Cross-org negative tests are required whenever a table has organization-scoped data.

## Schema and Migration Rules

- Tables with both `organization_id` and `parent_id` columns MUST have a composite FK that includes both, OR the task must explicitly document why not and include cross-org negative tests.
- Already-merged Flyway migrations must never be rewritten. If a schema change is needed after merge, create a new follow-up migration.

## Review and Finding Rules

- Every review finding must be tagged with one of: `confirmed-current` (verified on current main), `fixed-current` (resolved on current branch), `stale-old-worktree` (path no longer exists on current main), `needs-reverification` (not yet rechecked).
- Findings that reference paths in old worktrees are stale candidates. They must be re-located against the current main branch before being cited.

## Completion Reporting Rules

- Completion labels must use precise tiers: `baseline exists`, `pilot-ready`, `production-ready`, `full product complete`. Never inflate.
- A task is not "complete" just because code compiles. Tests must pass, docs must be updated, and known gaps must be explicitly listed.

## Required Validation Commands

Run from the repository root:

```sh
git diff --check
docker info
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

## Standard Final Report Format

Every future task final report must separate these five items:

1. **Code completed** — files new/modified count, worktree, base/final commit
2. **Tests passed** — exact count, 0 failures required
3. **Docs updated** — which files were changed
4. **Full suite run** — yes or no (and which scope if partial)
5. **Known hardening gaps** — what was intentionally left for later

Plus: boundary confirmation and merge recommendation.

Completion labels must be honest about scope (e.g. "Company CRUD baseline", not "full product API").

## Merge Rule

- Codex must not merge without GPT/user review.
- After Codex reports back, GPT performs merge review.
- Only after approval should a separate review / fast-forward merge / validate / cleanup prompt be generated.
