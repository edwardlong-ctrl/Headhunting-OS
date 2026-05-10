# Release Gates

Task 58 packages existing regression evidence into one repeatable release safety system. It does not create new product workflows and does not implement Task 60 final acceptance.

## Gate Chain

`rtk npm run release:gate` runs:

1. Backend regression: `npm run test:core-api`
2. Frontend unit regression: `npm --workspace @rto/web run test`
3. Frontend typecheck: `npm run typecheck:web`
4. Frontend build: `npm run build:web`
5. Migration validation: `npm run release:migrations`
6. Privacy/security negative: `npm run release:privacy-security`
7. AI eval regression: `npm run release:ai-eval`
8. Browser E2E: `npm run release:e2e:pilot`, unless explicitly skipped with `RTO_RELEASE_SKIP_BROWSER_E2E=1` and `RTO_RELEASE_E2E_EVIDENCE` pointing to a readable signed risk-acceptance artifact.

The release gate exits non-zero on the first failed required gate. It prints `RELEASE_READY` only after all required gates have passed or after browser E2E has a readable signed risk-acceptance artifact for an environment-dependent run. That artifact must name the owner, reason, expiration, rollback condition, and latest `release:e2e:pilot` evidence.

## Required Gates

### Backend regression

Runs the full Maven test suite. This protects backend-owned domain truth, WorkflowEvent guarantees, service-layer permissions, persistence contracts, and API boundary contracts.

### Frontend regression

Runs web tests, typecheck, and build. This protects portal route contracts, session isolation, UI build integrity, and v2.0/v2.1 portal preservation from accidental regressions.

### Migration validation

Runs filename/version checks and the existing Flyway/Testcontainers PostgreSQL migration integration coverage. PostgreSQL is the target source of truth, so release validation cannot rely only on compile-time checks or in-memory substitutes.

### Privacy/security negative

Runs focused negative suites for:

- Task 51 tenant boundaries.
- Five-portal access control.
- Raw Candidate leakage prevention.
- Client-safe projection.
- Disclosure/unlock access.
- Access audit/security baseline.
- Upload/security hardening.

### AI eval regression

Validates local eval case artifacts and prompt/schema registry coverage without calling live models. AI outputs claims, not facts; this gate only proves the release has deterministic eval artifacts and schema/prompt coverage.

### Browser E2E

Runs the deterministic Task 42 pilot browser path through temporary local ports. The script states expected startup signals before launch, starts an isolated PostgreSQL container when no datasource is provided, checks PostgreSQL and `/health` readiness instead of sleeping blindly, uses deterministic provider routes, and cleans up only the database container and API process it starts.

CI keeps this as a manual workflow-dispatch job because it boots services and rebuilds synthetic pilot data in an isolated test database.

## Hard Invariants

The release safety system must preserve:

- Backend owns truth.
- PostgreSQL is the target source of truth.
- AI outputs claims, not facts.
- Every key state transition must create WorkflowEvent.
- Client must never read raw Candidate objects before unlock/disclosure.
- Task 51 tenant boundaries must remain enforced.

## Non-Claims

Task 58 only creates the release safety system. It does not claim final product acceptance, public launch operation, managed infrastructure readiness, formal security certification, live customer migration completion, external BI/legal/accounting integration completion, or Task 60 closure.
