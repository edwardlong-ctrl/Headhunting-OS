# Prompt Writing Guide

For anyone writing a Codex task prompt. These rules control prompt structure, scope, and quality — they are for the **prompt author**, not the AI agent.

AI behavior rules live in `docs/roadmap/codex-task-operating-rules.md`.

---

## 1. Repo-Truth Preflight

Extract confirmed facts from actual repo files before writing the prompt. Do NOT infer baseline from roadmap documents alone.

- Read the actual source files this task touches.
- Note what methods/classes/tables exist vs. don't exist.
- Record the current test count and HEAD commit.
- Mark every fact with "Confirmed by Repo" so the AI knows it was verified.

## 2. Three-Layer Prompt Structure

Every task prompt must have exactly three sections:

1. **Confirmed Repo Facts** — what the codebase actually contains right now (verified by reading files)
2. **Mandated Outcomes** — what MUST be delivered (numbered, concrete, testable)
3. **Open Design Decisions** — questions the AI must propose answers for before coding

Optional fourth section: **Forbidden Scope** — what the AI must NOT touch.

## 3. Assumption Audit

Label every factual claim with its confidence level:

| Label | Meaning |
|-------|---------|
| `confirmed` | Verified by reading an actual file on current main |
| `inferred` | Reasonable deduction but not directly verified |
| `open` | Needs a design decision before proceeding |
| `stale` | May have been true once; re-verify before relying on it |

## 4. Design Checkpoint Before Code

For schema/migration tasks: the prompt must require a **delta proposal** (what tables/columns change) before any code is written. The AI stops, presents the proposal, and waits for approval.

For non-schema tasks: if there are Open Design Decisions, the AI must propose answers before implementing.

## 5. Don't Write Dead Schema

State the **outcome** the task must achieve. Do NOT prescribe column-by-column implementation. Let the implementer compare against the current schema and design the delta themselves.

Bad: "Add a VARCHAR(255) column called `status_reason` to the `jobs` table."
Good: "Job records must track why they entered their current status."

## 6. Docs Closeout

Every prompt must include a Docs Closeout section listing which docs files to update after implementation:

- `docs/roadmap/current-engineering-snapshot.md` — HEAD, test count, completed task, gaps
- `docs/roadmap/implementation-status.md` — task entry
- `docs/roadmap/known-gaps.md` — scope limitations from this task

## 7. Task Sizing

Large tasks split into A/B/C/D subtasks. Each subtask must be independently reviewable and mergeable.

Splitting thresholds:
- **Schema change + code change** → separate subtasks
- **> 20 files touched** → split
- **> 2 bounded domains** → split
- **High-risk area** (auth, disclosure, canonical write, client-facing API, commission) → keep small

## 8. Final Report: Five-Item Separation

The prompt's Final Report Requirements must require these five items reported separately:

1. **Code completed** — files new/modified count
2. **Tests passed** — exact count, 0 failures
3. **Docs updated** — which files
4. **Full suite run** — yes or no (and which scope if partial)
5. **Known hardening gaps** — what was intentionally left for later

Plus a **completion label** that is honest about scope (e.g. "Company CRUD baseline", not "full product API").

## 9. Branch / Worktree Hygiene

Every prompt must state at the top:

- Repo path
- Current main HEAD commit
- Whether worktree is required
- Whether direct modification of main is allowed
- Whether commit is allowed (and how many)
- Whether merge is allowed

## 10. Validation Matrix by Scope

Different task scopes require different validation. The prompt must specify which applies:

| Scope | Validation |
|-------|-----------|
| docs-only | `git diff --check` |
| backend-only | `git diff --check` + `mvn test` (full suite) |
| frontend-only | `git diff --check` + `npm run typecheck:web` + `npm run build:web` |
| frontend + API | full backend suite + typecheck + build |
| migration | `git diff --check` + `mvn test` + manual review of migration SQL |

Full Maven suite is `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test`.

## 11. Prompt Size Control

If a prompt exceeds any of these thresholds, split it before writing:

- 1 migration + > 20 files
- > 50 files total
- > 2 bounded domains

Split into separate prompts, each independently executable.
