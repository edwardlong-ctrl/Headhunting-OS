# AI Headhunting Transaction OS

Production-first recruitment/headhunting platform. NOT a demo or MVP.

## Tech Stack

- **Frontend**: Vite + React + TypeScript (`apps/web`), workspace `@rto/web`
- **Backend**: Java 21 Spring Boot modular monolith (`services/core-api`)
- **Database**: PostgreSQL (Flyway migrations at `services/core-api/src/main/resources/db/migration/`)
- **Infra**: Docker Compose for local PostgreSQL

## Repository Layout

| Directory | Purpose |
|-----------|---------|
| `apps/web` | Frontend route shell (5 portals: owner, consultant, client, candidate, admin) |
| `services/core-api` | Spring Boot backend — domain services, API boundary, persistence |
| `services/workers-go` | Reserved for future Go workers (no code yet) |
| `packages/contracts` | Reserved shared schema/API contracts |
| `packages/design-system` | Reserved UI primitives |
| `packages/test-fixtures` | Reserved test fixtures (never runtime truth) |
| `docs/architecture` | Architecture docs, data model, module boundaries |
| `docs/specs` | Product specs (v2.1 current, v2.0 preserved) |
| `docs/roadmap` | Engineering snapshot, implementation status, task rules |
| `docs/prompts` | Task prompts and prompt writing guide |
| `infra` | Docker, PostgreSQL, observability notes |

## Common Commands

```bash
# Frontend
npm install
npm run dev:web          # Start Vite dev server
npm run build:web        # Production build
npm run typecheck:web    # TypeScript check

# Backend (Java 21 + Maven)
mvn -f services/core-api/pom.xml test         # Run tests
mvn -f services/core-api/pom.xml clean verify  # Full build + tests

# Database
docker compose up -d postgres   # Start local PostgreSQL

# Validation (before commit)
git diff --check
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

## MUST NOT

- Merge to main. Merge is always done via separate PR review.
- Fast-forward main.
- Push with `--force`.
- Weaken existing safety gates or tests to make a task pass.
- Rewrite already-merged Flyway migrations. Use follow-up migrations.
- Rewrite product specs unless explicitly asked.
- Bypass `CanonicalWriteGate`.
- Expose raw Candidate/CandidateProfile data to clients before unlock/disclosure.
- Import broad scope creep beyond the current task.

## Task Workflow

Read `docs/roadmap/codex-task-operating-rules.md` before any implementation.

- Work in a task worktree or branch, never directly on main.
- Negative tests first for boundary-sensitive work (privacy, auth, org-scope, canonical write).
- Validation must pass before commit. If it fails, report clearly and do NOT merge.
- Produce one focused commit per task.
- Final report must separate: code completed / tests passed / docs updated / full suite run / known gaps.

## Architecture Invariants

- Backend owns truth. PostgreSQL is the canonical source of truth.
- AI outputs are claims, not facts. Claim Ledger → human review → CanonicalWriteGate → canonical fact.
- Every key state transition must create a WorkflowEvent.
- `Bulk approve` cannot produce `CANDIDATE_CONFIRMED` or `EXTERNAL_VERIFIED`.
- Consultant is one unified portal.

## Key Docs

- Current spec: `docs/specs/CURRENT_SPEC.md` → `docs/specs/v2.1/product-spec-v2.1.md`
- Operating rules: `docs/roadmap/codex-task-operating-rules.md`
- Prompt writing guide: `docs/prompts/PROMPT_WRITING_GUIDE.md`
- Engineering snapshot: `docs/roadmap/current-engineering-snapshot.md`
- Implementation status: `docs/roadmap/implementation-status.md`
- Known gaps: `docs/roadmap/known-gaps.md`
