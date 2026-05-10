# Release Checklist

Task 58 defines the repeatable release safety process for the current controlled-pilot product baseline. A release cannot be called ready unless every required gate below has passing evidence, or an environment-dependent gate has signed risk acceptance recorded before release signoff.

This checklist does not certify Task 60 final product acceptance, public launch readiness, managed cloud operation, SOC 2/ISO status, or customer go-live approval.

| Gate | Owner | Command | Expected evidence | Blocker condition | Waiver / risk acceptance |
| --- | --- | --- | --- | --- | --- |
| Backend regression | Engineering | `rtk npm run test:core-api` | Maven exits 0 with the full backend regression suite. | Any compile failure, test failure, or skipped required Testcontainers dependency. | No waiver for release-ready status. Fix or mark release blocked. |
| Frontend regression | Engineering | `rtk npm --workspace @rto/web run test` | Vitest exits 0 for the web workspace. | Any frontend test failure. | No waiver for release-ready status. |
| Frontend typecheck | Engineering | `rtk npm run typecheck:web` | TypeScript exits 0 with no type errors. | Any type error. | No waiver for release-ready status. |
| Frontend build | Engineering | `rtk npm run build:web` | Vite build exits 0 and produces the web build artifact. | Build failure or missing artifact. | No waiver for release-ready status. |
| Migration validation | Engineering / Deployment owner | `rtk npm run release:migrations` | Flyway migration filenames are contiguous from V1; `TruthLayerPostgresMigrationIntegrationTest` applies migrations to PostgreSQL with Testcontainers. | Duplicate migration version, gap, invalid filename, Docker/Testcontainers unavailable, or migration apply failure. | A release can only proceed with signed risk acceptance when the target release contains no migration changes and an equivalent fresh PostgreSQL migration report is attached. |
| Privacy/security negative | Security owner | `rtk npm run release:privacy-security` | Tenant boundary, client-safe projection, disclosure/unlock, access audit, auth/rate-limit, upload hardening, and pilot privacy regressions exit 0. | Any negative test fails or no tests are selected. | No waiver for privacy leakage, raw Candidate leakage, tenant-boundary failure, or disclosure/unlock bypass. |
| AI eval regression | AI governance owner | `rtk npm run release:ai-eval` | Local eval case JSON, prompt files, and input/output schemas validate without live model calls. | Empty eval suite, missing prompt/schema, invalid JSON, missing required assertions, or filename/task-key mismatch. | Live model evals may be separately waived, but this artifact/schema gate may not be skipped. |
| Browser E2E | Release owner | `rtk npm run release:e2e:pilot` | Pilot browser E2E passes on temporary PostgreSQL/API/web ports with deterministic AI provider routes; logs show PostgreSQL readiness, `/health` readiness, and cleanup of the owned API process and database container. | API/web/database startup failure, pilot seed validation failure, Playwright failure, or missing cleanup evidence. | For CI or blocked local environments, attach signed risk acceptance plus the latest successful `release:e2e:pilot` evidence artifact. `release:gate` requires `RTO_RELEASE_E2E_EVIDENCE` to point to a readable file containing owner, reason, expiration, rollback condition, and `release:e2e:pilot` evidence if `RTO_RELEASE_SKIP_BROWSER_E2E=1`. |
| Full local release gate | Release owner | `rtk npm run release:gate` | Runs backend, frontend, migration, privacy/security, AI eval, and browser E2E gates in order, then prints `RELEASE_READY`. | Any upstream gate fails; browser E2E skipped without a readable signed risk-acceptance artifact. | The command must not be used to claim readiness when it exits non-zero. |

## Signoff Rule

Before release signoff, record:

- Git commit under evaluation.
- Exact commands and timestamps.
- Test reports or terminal logs for each gate.
- Browser E2E ports and startup signals when run locally.
- Any signed risk acceptance, owner, reason, expiration date, and rollback condition.

Waivers cannot override hard product invariants:

- Backend owns truth.
- PostgreSQL is the target source of truth.
- AI outputs claims, not facts.
- Every key state transition must create WorkflowEvent.
- Client must never read raw Candidate objects before unlock/disclosure.
- Task 51 tenant boundaries must remain enforced.
