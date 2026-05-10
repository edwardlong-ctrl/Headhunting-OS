# Task 58 Release Management and Regression Suite

Task 58 packages the current controlled-pilot and production-depth gates into a repeatable release process. It does not rebuild product features and does not implement Task 60 final product acceptance.

## Scope

Task 58 delivers:

- CI pipeline: `.github/workflows/release-regression.yml`.
- Migration validation: `scripts/release/validate-migrations.sh`.
- Backend regression suite: full Maven suite through `release:gate`.
- Frontend regression suite: web tests, typecheck, and build through `release:gate`.
- Browser E2E suite: `scripts/release/run-pilot-e2e.sh`.
- Privacy/security negative suite: `scripts/release/privacy-security-regression.sh`.
- AI eval regression suite: `scripts/release/ai-eval-regression.sh`.
- Release checklist and gate docs: `docs/release/release-checklist.md` and `docs/release/release-gates.md`.

## Automated

| Gate | Automation | Evidence |
| --- | --- | --- |
| Backend regression | `rtk npm run test:core-api` and `rtk mvn -f services/core-api/pom.xml test` | Full Maven test output. |
| Frontend regression | `rtk npm --workspace @rto/web run test`, `rtk npm run typecheck:web`, `rtk npm run build:web` | Vitest, TypeScript, and build output. |
| Migration validation | `rtk npm run release:migrations` | Flyway filename/version validation plus `TruthLayerPostgresMigrationIntegrationTest` against PostgreSQL Testcontainers. |
| Privacy/security negative | `rtk npm run release:privacy-security` | Tenant, access-control, client-safe, disclosure/unlock, audit, auth, upload, and pilot privacy negative tests. |
| AI eval regression | `rtk npm run release:ai-eval` | Local eval JSON, prompt, and schema coverage validation with no live model calls. |
| Browser E2E | `rtk npm run release:e2e:pilot` | Temporary PostgreSQL/API/web ports, PostgreSQL readiness, `/health` readiness, deterministic AI routes, Playwright pilot report, and owned process/container cleanup evidence. |
| Full gate | `rtk npm run release:gate` | Ordered release chain and `RELEASE_READY` only after required gates pass. |

## Manual or environment-dependent

Browser E2E boots services and rebuilds deterministic synthetic pilot data. It starts an isolated local PostgreSQL container when no datasource is supplied. It is automated locally and as a manual CI workflow-dispatch job, but normal CI does not run it by default.

If local browser E2E cannot run because Docker, PostgreSQL, or browser dependencies are unavailable, release signoff must include:

- signed risk acceptance,
- exact blocker,
- latest successful `release:e2e:pilot` evidence artifact,
- owner,
- expiration date,
- rollback condition.

`release:gate` fails closed when browser E2E is skipped without `RTO_RELEASE_E2E_EVIDENCE` pointing to a readable signed risk-acceptance artifact. That artifact must include owner, reason, expiration, rollback condition, and the latest `release:e2e:pilot` evidence.

## Deferred until Task 60

Task 58 does not implement Task 60. The following remain outside this task:

- final full-product acceptance gate,
- public launch operation signoff,
- managed cloud and public domain/HTTPS acceptance,
- formal security certification,
- live customer migration execution,
- external BI/legal/accounting completion,
- final go-live decision.

## CI Behavior

The CI workflow runs backend, frontend, migration, privacy/security, and AI eval gates without local-only secrets. The pilot browser E2E job is manual because it starts services and touches an isolated test PostgreSQL database.

## Release Rule

A release cannot be called ready unless tests, migrations, E2E, privacy, and eval gates pass, or an environment-dependent browser E2E gate has signed risk acceptance with evidence. Privacy leakage, tenant boundary failure, raw Candidate leakage, and disclosure/unlock bypass cannot be waived for release-ready status.
