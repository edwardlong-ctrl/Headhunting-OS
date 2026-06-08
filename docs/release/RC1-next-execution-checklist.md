# RC1 Next Execution Checklist

## Current State

- `docs/release/RC1-pilot-readiness-plan.md` is the execution plan.
- Product gates have not been run in this RC1 execution.
- Fresh local check at `2026-06-08 02:40:49 CST` confirms Docker CLI is installed but Docker server is unavailable at `unix:///Users/edwardlong/.docker/run/docker.sock`.
- Therefore Testcontainers, PostgreSQL migration apply, isolated pilot data, browser E2E, and `release:gate` cannot produce pass evidence yet.
- Treat the current maximum decision as `not-ready-blocked-environment` until Docker client and server are reachable.

## Stop Rules

- Do not run Testcontainers-dependent, isolated PostgreSQL, browser E2E, or `release:gate` checks while Docker server is unavailable.
- Do not use real candidate, client, customer, or production data.
- Do not run `pilot:data:rebuild` against `localhost:5432/recruiting_os` unless the user confirms that database is disposable for RC1.
- Do not claim `ready-for-controlled-pilot` from screenshots, source mapping, service tests, or pre-placement E2E alone.
- Canonical write, consent, unlock/disclosure, placement, commission, owner commercial proof, and admin governance proof require runtime/API plus persistence or WorkflowEvent/audit evidence tied to the same transaction IDs.

## Execution Order

1. Repair or start Docker Desktop.

Run:

```bash
rtk docker version
rtk docker info
```

Expected: both commands show Docker client and server information and exit `0`.

2. Start RC1 evidence artifacts with Task RC1-00.

Run only the RC1-00 steps in `docs/release/RC1-pilot-readiness-plan.md`.

Expected: create:

- `docs/release/RC1-pilot-readiness-evidence.md`
- `docs/release/RC1-capability-map.md`
- `docs/release/rc1-artifacts/`

3. Freeze the checkout with Task RC1-01.

Run:

```bash
rtk git rev-parse --abbrev-ref HEAD
rtk git rev-parse HEAD
rtk git status --short
```

Expected: evidence log records branch, commit, and dirty-worktree state.

4. Re-run Docker gate as Task RC1-02.

Run:

```bash
rtk docker version
rtk docker info
```

Expected: evidence log records `Docker/Testcontainers` as `passed`, or `blocked-environment` with an exact failure excerpt.

5. Run migration validation as Task RC1-03 only after Docker passes.

Run:

```bash
rtk npm run release:migrations
```

Expected: contiguous Flyway filename validation plus Testcontainers-backed PostgreSQL migration apply evidence.

6. Validate isolated synthetic pilot data as Task RC1-03A.

Use the isolated PostgreSQL container path from the plan, not the default local database.

Expected: `pilot:data:rebuild` and `pilot:data:validate` pass against isolated synthetic data, and `RC1-03A-synthetic-data-attestation.md` is created.

7. Run local regression gates in order.

Run:

```bash
rtk npm --workspace @rto/web run test
rtk npm run typecheck:web
rtk npm run build:web
rtk npm run test:core-api
rtk npm run build:core-api
rtk npm run release:privacy-security
rtk npm run release:ai-eval
```

Expected: each command exits `0`; any failure gets its exact excerpt, blocker type, owner, rollback/cleanup, and next action in the evidence log.

8. Run pilot browser E2E only after data and regressions pass.

Run:

```bash
rtk npm run release:e2e:pilot
```

Expected: PostgreSQL readiness, API `/health`, Playwright pass output, and cleanup evidence.

9. Close the known RC1 gaps before full readiness.

Required gates:

- RC1-06B browser/API privacy negative proof.
- RC1-08A placement, commission, owner revenue/accounting, and admin governance/audit proof.
- RC1-08 full release gate with `RELEASE_READY`.
- RC1-08B operations readiness evidence.
- RC1-09 manual golden path operator walkthrough.

10. Synthesize and decide.

Run RC1-10 and RC1-11 from the plan.

Expected: every spec traceability row is reconciled, every `implemented` capability has current evidence, and the decision is exactly one of:

- `ready-for-controlled-pilot`
- `not-ready-blocked-environment`
- `not-ready-blocked-product`
- `not-ready-blocked-dependency`
- `not-ready-insufficient-evidence`
