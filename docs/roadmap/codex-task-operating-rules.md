# Codex Task Operating Rules

## Purpose

This file is the short, stable operating rule layer for Codex tasks.

Future task prompts should reference this file instead of repeating the same long project rules.

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

## Required Validation Commands

Run from the repository root:

```sh
git diff --check
docker info
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

## Standard Final Report Format

Every future task final report should include:

- Worktree
- Base commit
- Final commit
- Changed files
- Implementation summary
- Validation
- Boundary confirmation
- Known gaps remaining
- Merge recommendation

## Merge Rule

- Codex must not merge without GPT/user review.
- After Codex reports back, GPT performs merge review.
- Only after approval should a separate review / fast-forward merge / validate / cleanup prompt be generated.
