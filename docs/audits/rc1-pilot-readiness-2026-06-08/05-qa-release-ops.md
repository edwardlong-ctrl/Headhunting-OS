# RC1 Pilot Readiness Audit - QA, Release, Runtime, Environment, And Operations

## Scope And Evidence Read

Scope: QA, release, CI, runtime, environment, and operations readiness for RC1 controlled-pilot readiness. This was a read-only audit except this component report. I did not run long product suites, mutate databases, start servers, or modify code/docs outside this file.

Current checkout evidence:

- Timestamp: `2026-06-08T01:24:59+08:00`.
- Branch: `main`.
- Commit: `e0401e17992c7e5da4580faf515832b7d81bb5a6`.
- Dirty state observed: `.gitignore` modified, `docs/release/RC1-pilot-readiness-plan.md` untracked, `docs/audits/` untracked.
- Environment probes run:
  - `rtk node --version`: `v24.13.1`.
  - `rtk npm --version`: `11.8.0`.
  - `rtk java -version`: OpenJDK `21.0.11`.
  - `rtk mvn -version`: Apache Maven `3.9.15`, Java `21.0.11`.
  - `rtk docker version`: Docker client `29.4.1`, but daemon connection failed at `unix:///Users/edwardlong/.docker/run/docker.sock`.
  - `rtk docker info`: daemon connection failed with the same socket error.

Required source of truth read first:

- `docs/release/RC1-pilot-readiness-plan.md`: RC1 requires current evidence from intake through placement, commission, and audit trace, and classifies Docker/Testcontainers/local PostgreSQL/port/browser startup blockers as `not-ready-blocked-environment` (`docs/release/RC1-pilot-readiness-plan.md:15-24`). The plan includes release, migration, backend, frontend, privacy/security, AI eval, browser E2E, golden-path runtime, placement/commission, and audit trace scope (`docs/release/RC1-pilot-readiness-plan.md:49-57`).
- `docs/specs/CURRENT_SPEC.md`: v2.1 is current, v2.0 UI is preserved, backend owns truth, PostgreSQL is the target source of truth, every key state transition creates `WorkflowEvent`, and clients must not read raw `Candidate` before unlock/disclosure (`docs/specs/CURRENT_SPEC.md:3-21`).
- `docs/specs/v2.1/product-spec-v2.1.md`: v2.1 requires AI-as-claims, backend/PostgreSQL truth ownership, workflow events, consent/disclosure protections, placement/commission, observability, and audit/governance (`docs/specs/v2.1/product-spec-v2.1.md:173-185`, `docs/specs/v2.1/product-spec-v2.1.md:887-895`, `docs/specs/v2.1/product-spec-v2.1.md:901-928`).

Additional evidence inspected:

- Root and web package scripts: `package.json`, `apps/web/package.json`.
- Release scripts: `scripts/release/release-gate.sh`, `scripts/release/validate-migrations.sh`, `scripts/release/run-pilot-e2e.sh`, `scripts/release/privacy-security-regression.sh`, `scripts/release/ai-eval-regression.sh`.
- Pilot data script: `scripts/pilot-data.sh`.
- CI: `.github/workflows/release-regression.yml`.
- Docker/runtime/env: `docker-compose.yml`, `infra/docker/compose.production-like.yml`, `infra/deployment/production-like.env.example`, staging/production application YAML, deployment validator code/tests.
- Ops runbooks: migration, rollback, backup/restore, DR/BCP, staging smoke, observability/incident runbook, onboarding/risk/go-live guides.
- Test structure: backend JUnit tests, Testcontainers tests, web Vitest files, Playwright specs.
- Existing QA/audit reports: `audit-reports/project-status-2026-06-07/05-quality-gates.md`, `audit-reports/project-status-2026-06-07/SYNTHESIS.md`, `audit-reports/evidence-gate-2026-06-03/SYNTHESIS.md`, `artifacts/task42-backup-restore-20260509/evidence.md`, and existing component report `docs/audits/rc1-pilot-readiness-2026-06-08/01-plan-structure.md`.

## Current Verification Surface Summary With Commands Found

Root scripts:

| Surface | Command | Evidence |
| --- | --- | --- |
| Web build | `npm run build:web` | Root script delegates to web build (`package.json:12-15`); web build runs `tsc --noEmit && vite build` (`apps/web/package.json:6-12`). |
| Web typecheck | `npm run typecheck:web` | Root script delegates to web typecheck (`package.json:30`); web typecheck runs `tsc --noEmit --pretty false` (`apps/web/package.json:12`). |
| Web unit regression | `npm --workspace @rto/web run test` | Web script runs `vitest run` (`apps/web/package.json:10`). |
| Backend regression | `npm run test:core-api` | Root script runs `mvn -f services/core-api/pom.xml test` (`package.json:29`). |
| Backend build | `npm run build:core-api` | Root script runs `mvn -f services/core-api/pom.xml clean verify` (`package.json:13`). |
| Pilot data | `npm run pilot:data:{rebuild,validate,export,reset,import}` | Root scripts call `scripts/pilot-data.sh` (`package.json:17-21`); the script executes `PilotDataCliApplication` through Maven exec (`scripts/pilot-data.sh:7-14`). |
| Release gates | `release:migrations`, `release:privacy-security`, `release:ai-eval`, `release:e2e:pilot`, `release:gate` | Root scripts are defined at `package.json:22-26`. |

Release gate behavior:

- `scripts/release/release-gate.sh` runs backend regression, web tests, web typecheck, web build, migration validation, privacy/security, AI eval, and browser E2E unless browser E2E is skipped with signed evidence (`scripts/release/release-gate.sh:15-63`). It prints `RELEASE_READY` only after passing the ordered chain (`scripts/release/release-gate.sh:65-69`).
- `scripts/release/validate-migrations.sh` checks Flyway filename versions/order and then runs `TruthLayerPostgresMigrationIntegrationTest` through Maven/Testcontainers (`scripts/release/validate-migrations.sh:7-48`).
- `scripts/release/privacy-security-regression.sh` selects tenant, client-safe, disclosure/unlock, audit, auth/rate-limit, upload, and pilot privacy regression tests (`scripts/release/privacy-security-regression.sh:7-31`).
- `scripts/release/ai-eval-regression.sh` validates local eval JSON, prompt files, and input/output schemas without live model calls (`scripts/release/ai-eval-regression.sh:7-73`).
- `scripts/release/run-pilot-e2e.sh` starts an isolated PostgreSQL container when no datasource is provided, blocks non-local JDBC URLs unless explicitly allowed, rebuilds and validates deterministic pilot data, starts the API with deterministic AI routes, waits for `/health`, then runs Playwright (`scripts/release/run-pilot-e2e.sh:41-55`, `scripts/release/run-pilot-e2e.sh:86-140`, `scripts/release/run-pilot-e2e.sh:142-173`).

CI:

- `.github/workflows/release-regression.yml` runs backend tests on PR/push (`.github/workflows/release-regression.yml:19-34`), frontend tests/typecheck/build (`.github/workflows/release-regression.yml:35-47`), and migration/privacy/AI eval gates (`.github/workflows/release-regression.yml:48-64`).
- Browser pilot E2E is a manual `workflow_dispatch` job gated by `run_pilot_e2e == true` (`.github/workflows/release-regression.yml:65-98`). Normal PR/push CI does not run the browser E2E job or the top-level `release:gate`.

Test inventory observed with bounded file-count commands:

- Core API test files: 163.
- Testcontainers/PostgreSQL integration test files: 38.
- Web test files: 9.
- Playwright spec files: 2.
- The Playwright business-flow suite is explicitly S01-S08 and ends at interview feedback/follow-up review (`tests/e2e/pilot-business-flows.spec.ts:99-397`).

Runtime, environment, and operations surfaces:

- Local Compose provides PostgreSQL and MinIO with health checks (`docker-compose.yml:1-34`).
- Production-like Compose builds `core-api` and `web`, uses PostgreSQL, MinIO, `minio-init`, health-gated app startup, and app ports (`infra/docker/compose.production-like.yml:1-79`).
- `production-like.env.example` documents required datasource, Flyway, JWT, document storage, AI provider, base URL/origin, database managed flag, and object-storage variables with `CHANGE_ME` placeholders (`infra/deployment/production-like.env.example:1-32`).
- Staging/production profiles bind datasource, Flyway, JWT, document storage, AI, deployment URL, database, and object-storage settings from env vars (`services/core-api/src/main/resources/application-staging.yml:1-52`, `services/core-api/src/main/resources/application-production.yml:1-52`).
- Startup validation is active only for `staging` and `production` profiles (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/deployment/DeploymentEnvironmentConfiguration.java:12-31`), and validates PostgreSQL URL, datasource credentials, Flyway, JWT length, document storage, production virus-scan mode, AI key/models, HTTPS frontend/public URLs, managed production database, and object storage (`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/deployment/DeploymentEnvironmentValidator.java:12-58`).
- Backup/restore runbook requires PostgreSQL dump plus document/object-storage backup, restore into a separate DB, app start, `/health`, pilot data validation, audit preservation, and restored document availability (`infra/deployment/backup-restore-runbook.md:51-59`).
- DR/BCP runbook covers backup schedule, restore drill, migration rollback, object storage recovery, AI provider outage, notification outage, and incident severities, while explicitly leaving managed cloud backup/failover and production provider failover unexecuted (`infra/deployment/task-53-disaster-recovery-business-continuity.md:27-36`, `infra/deployment/task-53-disaster-recovery-business-continuity.md:222-291`, `infra/deployment/task-53-disaster-recovery-business-continuity.md:316-324`).
- Observability runbook covers request correlation, structured logs, admin observability APIs, incident lookup steps, and forbidden-log handling; it explicitly does not add Prometheus, OpenTelemetry, external log collectors, or vendor dashboards (`infra/observability/README.md:1-21`, `infra/observability/README.md:71-117`).

## Severity-Ranked Findings

### 1. Severity: P0 - Current Docker daemon unavailability blocks RC1 environment, Testcontainers, migration-application, isolated pilot-data, and E2E gates

Observed evidence:

- Current command result: `rtk docker version` showed Docker client `29.4.1`, then failed with `Cannot connect to the Docker daemon at unix:///Users/edwardlong/.docker/run/docker.sock. Is the docker daemon running?`
- Current command result: `rtk docker info` failed with the same daemon socket error.
- RC1 plan says Docker/Testcontainers/local PostgreSQL/ports/browser startup blockers produce `not-ready-blocked-environment` (`docs/release/RC1-pilot-readiness-plan.md:21-24`).
- RC1 plan's L1 requires Docker/Testcontainers and isolated local data source availability (`docs/release/RC1-pilot-readiness-plan.md:97-104`).
- RC1 plan says Docker/Testcontainers failures are environment blockers until logs prove product regression (`docs/release/RC1-pilot-readiness-plan.md:112-114`) and stops Testcontainers-dependent work when Docker is unavailable (`docs/release/RC1-pilot-readiness-plan.md:212-215`, `docs/release/RC1-pilot-readiness-plan.md:522-530`).
- Migration validation invokes Testcontainers-backed PostgreSQL migration coverage (`scripts/release/validate-migrations.sh:47-48`).
- Pilot E2E starts Docker PostgreSQL when no datasource is provided (`scripts/release/run-pilot-e2e.sh:86-113`).
- Prior current-status QA on 2026-06-07 saw the same class of blocker: migration filename validation reached V34, then Testcontainers failed with no valid Docker environment (`audit-reports/project-status-2026-06-07/05-quality-gates.md:87-126`).

Inference:

The current machine cannot provide L1 RC1 environment readiness. This does not prove a product regression; it prevents PostgreSQL migration application, Testcontainers integration proof, isolated pilot data rebuild, local browser E2E, and `release:gate` from producing pass evidence.

Likely cause:

Docker Desktop or the Docker daemon is not running or the active Docker context socket is unavailable.

Recommended change:

Start or repair Docker Desktop, then rerun in order:

```sh
rtk docker version
rtk docker info
rtk npm run release:migrations
```

Do not run or report pass/fail for Testcontainers-dependent, isolated PostgreSQL, browser E2E, or `release:gate` checks until Docker client and server are reachable.

### 2. Severity: P1 - RC1 names the right regression gates, but operations readiness is not first-class in the final hard-pass checklist

Observed evidence:

- RC1 final hard-pass criteria require migrations, synthetic pilot data, backend tests/build, frontend tests/typecheck/build, privacy/security, AI eval, pilot E2E, `release:gate`, golden path, placement/commission evidence, owner/admin audit trace, privacy/AI invariants, manual operator walkthrough, current evidence docs, and no waiver-only required gates (`docs/release/RC1-pilot-readiness-plan.md:1292-1312`).
- Those criteria do not explicitly require a current environment validation run, staging/production-like smoke check, backup/restore drill, rollback rehearsal, observability/incident dry run, dependency scan, or live-provider/manual-provider status decision.
- Separate docs do cover these concerns:
  - Env variables/secrets placeholders: `infra/deployment/production-like.env.example:1-32`.
  - Fail-fast deployment validator: `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/deployment/DeploymentEnvironmentValidator.java:12-58`.
  - Backup/restore proof requirements: `infra/deployment/backup-restore-runbook.md:51-59`.
  - Rollback rules: `infra/deployment/rollback-runbook.md:5-19`.
  - Incident/observability runbook: `infra/observability/README.md:71-117`.
  - DR/BCP gaps: `infra/deployment/task-53-disaster-recovery-business-continuity.md:316-324`.
  - Onboarding/risk docs require integration status, no red risks, support path, rollback/reset decision, and first-week monitoring (`docs/onboarding/customer-onboarding-checklist.md:89-110`, `docs/onboarding/risk-review-guide.md:146-184`, `docs/onboarding/go-live-checklist.md:72-113`).

Inference:

The repo has meaningful ops runbooks and validators, but the RC1 plan's hard checklist can still reach a final decision without forcing a current, dated ops evidence row for environment config, secrets presence, external provider posture, observability, backup, rollback, and incident response. For a controlled pilot, those should be current RC1 evidence or explicitly scoped as a signed nonclaim.

Likely cause:

`release:gate` and Task 58 focus on regression safety; Task 39/40/52/53/59 carry operations evidence in separate documents. RC1 has not yet stitched those operations artifacts into one release checklist.

Recommended change:

Add an `Operations Readiness` gate group to RC1 with explicit evidence rows:

- Deployment config validation command/test result.
- Secret/provider presence check that records names only, not values.
- External provider status: deterministic-only, live-provider-configured, manual channel approved, or out of scope.
- Backup/restore evidence for the exact RC1 pilot dataset or explicit not-current classification.
- Rollback runbook check with image/schema/version rollback target.
- Observability/incident dry-run evidence with request id, audit search, and escalation path.
- First-week monitoring owner and cadence.

### 3. Severity: P1 - Normal CI does not run the full RC1 release gate or browser E2E

Observed evidence:

- PR/push CI runs backend tests (`.github/workflows/release-regression.yml:20-34`), frontend tests/typecheck/build (`.github/workflows/release-regression.yml:35-47`), and release sub-gates for migrations, privacy/security, and AI eval (`.github/workflows/release-regression.yml:48-64`).
- Browser pilot E2E is manual-only: `if: github.event_name == 'workflow_dispatch' && inputs.run_pilot_e2e == 'true'` (`.github/workflows/release-regression.yml:65-67`).
- The workflow does not run `npm run release:gate`; the top-level gate exists as a local script (`scripts/release/release-gate.sh:51-67`).
- Release docs state browser E2E is manual in CI because it boots services and rebuilds synthetic pilot data (`docs/release/release-gates.md:50-55`, `docs/roadmap/task-58-release-management-regression-suite.md:57-63`).

Inference:

A green PR/push CI run is not equivalent to RC1 release readiness. It can miss the ordered `release:gate` chain and the browser E2E startup/cleanup behavior unless the manual dispatch job or local `release:gate` artifact is attached.

Likely cause:

The browser E2E gate is intentionally expensive and stateful, so CI keeps it manual.

Recommended change:

For RC1, require one of:

- A current successful manual `pilot-browser-e2e` CI run attached to the RC1 evidence log, plus local/CI evidence for the remaining gates.
- A current local `rtk npm run release:gate` artifact that includes `RELEASE_READY`, PostgreSQL readiness, `/health` readiness, Playwright pass output, and cleanup evidence.

Do not treat the default PR/push `release-regression` workflow as sufficient for RC1 signoff.

### 4. Severity: P1 - Commercial closure is correctly identified as required, but current automated E2E does not cover placement/commission runtime behavior

Observed evidence:

- RC1 plan says Task 42 browser flows prove S01-S08 through interview feedback, but not placement, commission, owner revenue, or accounting handoff runtime behavior (`docs/release/RC1-pilot-readiness-plan.md:1017-1030`).
- RC1 plan says `passed` commercial closure requires current runtime/API plus persistence/audit evidence; service tests or source evidence are only `partial` (`docs/release/RC1-pilot-readiness-plan.md:1053-1081`).
- Playwright suite covers S01 candidate intake through S08 client feedback and consultant follow-up review (`tests/e2e/pilot-business-flows.spec.ts:115-397`). There is no S09 placement or S10 commission step in that spec.
- Service/API test evidence exists for placement and commission behavior:
  - Placement invoice readiness requires fee agreement and creates pending commission (`services/core-api/src/test/java/com/recruitingtransactionos/coreapi/placement/service/PlacementWorkflowServiceTest.java:73-167`).
  - Commission workflow validates organization scope, paid status amount, and fee calculation inputs (`services/core-api/src/test/java/com/recruitingtransactionos/coreapi/commission/service/CommissionWorkflowServiceTest.java:41-170`).
  - Consultant placement query aggregates commission amounts and handles unknown fees (`services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantPlacementQueryServiceTest.java:40-167`).
  - Reporting export preserves read-only accounting handoff semantics (`services/core-api/src/test/java/com/recruitingtransactionos/coreapi/reportingexport/ReportingExportServicePolicyTest.java:178-200`).

Inference:

The release plan answers the commercial-closure gap honestly, but the current automation surface cannot by itself support `ready-for-controlled-pilot`. Focused backend tests can support a partial/service-level claim only; RC1 still needs runtime/API plus persisted row or WorkflowEvent/audit proof for placement, commission, and owner/admin audit trace.

Likely cause:

Task 42 browser E2E was a pre-placement pilot flow; Task 48/57 added commercial/backend/export depth later without adding a corresponding browser or runtime RC1 closure flow.

Recommended change:

Add either:

- An automated S09-S11 browser/API scenario covering placement recorded, invoice/commission state, owner revenue/accounting export, and admin/owner audit trace, or
- A manual RC1-09 runtime script with API calls and SQL/audit queries that records placement id, commission id, owner revenue/export evidence, and WorkflowEvent/audit rows.

### 5. Severity: P2 - RC1 evidence artifacts have not been created, so no current RC1 run exists yet

Observed evidence:

- `docs/release/RC1-pilot-readiness-evidence.md` is missing.
- `docs/release/RC1-capability-map.md` is missing.
- `docs/release/rc1-artifacts/` is missing.
- RC1 plan defines those as artifact responsibilities (`docs/release/RC1-pilot-readiness-plan.md:144-149`) and starts with an artifact creation task (`docs/release/RC1-pilot-readiness-plan.md:230-455`).
- Existing Task 42/Task 53/Task 60 evidence is historical and the RC1 plan explicitly says historical evidence can guide inspection but cannot replace current RC1 command/runtime/persistence/browser/audit evidence (`docs/release/RC1-pilot-readiness-plan.md:24`).

Inference:

The repo has a plan and historical evidence, but this checkout does not yet contain the RC1 evidence log/capability map that would support a current RC1 readiness decision.

Likely cause:

The RC1 implementation plan was added before execution artifacts were generated.

Recommended change:

Run RC1-00/RC1-01 after resolving any report-directory coordination, then execute gates into the evidence log. Until that exists, the highest supportable claim is "RC1 execution is planned/prepared", not "RC1 pilot ready".

### 6. Severity: P2 - Dependency/security scan refresh is outside `release:gate`

Observed evidence:

- `release:gate` runs backend, frontend, migration, privacy/security, AI eval, and browser E2E (`scripts/release/release-gate.sh:51-63`).
- Task 52 security baseline separately requires `rtk npm audit --omit=dev` and `rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check` (`docs/security/task-52-production-security-compliance-baseline.md:152-159`, `docs/security/task-52-production-security-compliance-baseline.md:201-212`).
- Task 52 warns that dependency-check cannot be claimed passed without NVD key/prewarmed cache or explicit risk acceptance (`docs/security/task-52-production-security-compliance-baseline.md:241-254`).

Inference:

For controlled pilot, privacy/security negative tests are necessary but not a full dependency/security refresh. A pilot release checklist should either run these scans or explicitly record why they are not required for this RC1 decision.

Likely cause:

Release regression gates optimize for deterministic code behavior; dependency scanning is maintained in the security baseline.

Recommended change:

Add a pre-pilot security evidence row:

```sh
rtk npm audit --omit=dev
rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check
```

If cache/API-key prerequisites are unavailable, record owner, review date, and bounded risk acceptance rather than leaving the scan implicit.

## Direct Answers To Audit Questions

1. Does the RC1 plan name the right verification commands and pass/fail gates?

Yes for regression and transaction-readiness gates. It names Docker, migration validation, pilot data, frontend test/type/build, backend test/build, privacy/security, AI eval, pilot E2E, full `release:gate`, commercial closure, manual golden path, and final decision gates (`docs/release/RC1-pilot-readiness-plan.md:325-342`, `docs/release/RC1-pilot-readiness-plan.md:727-1016`, `docs/release/RC1-pilot-readiness-plan.md:1017-1168`, `docs/release/RC1-pilot-readiness-plan.md:1292-1312`). Gap: operations runbooks and security/dependency scan refresh are not first-class final hard-pass gates.

2. Does it separate static verification, unit/integration tests, E2E tests, migrations, seed data, and manual pilot rehearsal?

Mostly yes. Migrations are RC1-03, seed data is RC1-03A, frontend gates are RC1-04, backend gates are RC1-05, privacy/security plus AI eval are RC1-06, browser E2E is RC1-07, full release gate is RC1-08, commercial closure is RC1-08A, and manual golden path is RC1-09 (`docs/release/RC1-pilot-readiness-plan.md:548-1168`). The plan also warns not to mutate default local DB without confirmation and prefers an isolated PostgreSQL container for pilot data (`docs/release/RC1-pilot-readiness-plan.md:626-690`).

3. Does it cover environment prerequisites, secrets, external providers, observability, backups, rollback, and incident response enough for a pilot?

Partially. The repo has strong supporting docs and code for environment validation, secret placeholders, deterministic AI routing, observability, backup/restore, rollback, DR/BCP, risk review, onboarding, and go-live. The RC1 plan itself covers Docker/Testcontainers and isolated pilot data well, but it does not require current RC1 evidence rows for environment config validation, secrets presence, external provider/manual channel status, observability incident dry run, backup/restore, rollback, or dependency/security scan refresh. For pilot readiness, those should be added to the RC1 release checklist.

4. Are known blockers like Docker/Testcontainers captured accurately?

Yes. The RC1 plan accurately classifies Docker/Testcontainers failures as environment blockers unless logs prove product regression (`docs/release/RC1-pilot-readiness-plan.md:112-114`). Current local probes confirm Docker daemon unavailability, so Docker/Testcontainers are currently `blocked-environment`, not a product regression.

5. What should be the RC1 release checklist?

See the proposed checklist below.

## Missing Evidence / Not Verified

- I did not run `npm run test:core-api`, `npm run build:core-api`, web tests, web typecheck, web build, `release:migrations`, `release:privacy-security`, `release:ai-eval`, `release:e2e:pilot`, or `release:gate`.
- PostgreSQL migration application is not currently verified because Docker daemon is unavailable.
- No browser E2E or Playwright result was produced in this audit.
- No runtime API/server behavior, persisted DB rows, WorkflowEvent rows, screenshots, logs, or manual operator walkthrough were collected.
- No secrets, external provider keys, SMTP/SMS/calendar/OCR/ATS providers, object storage credentials, or live model calls were inspected or verified.
- No backup/restore, rollback, incident, observability, dependency scan, production-like compose, or staging smoke command was rerun for this checkout.
- Remote GitHub Actions status was not checked; only local workflow files were inspected.
- `docs/release/RC1-pilot-readiness-evidence.md`, `docs/release/RC1-capability-map.md`, and `docs/release/rc1-artifacts/` do not exist in this checkout.

## Proposed RC1 Release Checklist With Command Gates

All commands should record timestamp, branch, commit, exit code, evidence artifact path, and blocker classification. Do not print secret values.

| Phase | Gate | Command / evidence | Pass condition | Current audit status |
| --- | --- | --- | --- | --- |
| 0 | Checkout freeze | `rtk git rev-parse --abbrev-ref HEAD`, `rtk git rev-parse HEAD`, `rtk git status --short` | Branch/commit/dirty state captured; no conflicting dirty files. | Captured for this audit; not written to RC1 evidence log. |
| 1 | Toolchain | `rtk node --version`, `rtk npm --version`, `rtk java -version`, `rtk mvn -version` | Node/npm satisfy `package.json` engines; Java 21 and Maven available. | Passed bounded probe. |
| 2 | Docker/Testcontainers | `rtk docker version`, `rtk docker info` | Docker client and server reachable. | Blocked-environment: daemon unavailable. |
| 3 | Static hygiene | `rtk git diff --check` | Exits 0. | Not run. |
| 4 | Web unit regression | `rtk npm --workspace @rto/web run test` | Vitest exits 0. | Not run. |
| 5 | Web typecheck | `rtk npm run typecheck:web` | TypeScript exits 0. | Not run. |
| 6 | Web build | `rtk npm run build:web` | Vite build exits 0 and artifact exists. | Not run. |
| 7 | Backend regression | `rtk npm run test:core-api` | Maven test exits 0; Docker/Testcontainers failures classified separately. | Not run. |
| 8 | Backend build | `rtk npm run build:core-api` | Maven `clean verify` exits 0. | Not run. |
| 9 | Migration validation | `rtk npm run release:migrations` | Flyway filenames contiguous and Testcontainers PostgreSQL apply/validation passes. | Not run; currently blocked by Docker if attempted. |
| 10 | Pilot data contract | Use RC1 isolated PostgreSQL container pattern, then `npm run pilot:data:rebuild` and `npm run pilot:data:validate` against that datasource. | Rebuild and validate exit 0 without mutating shared DB. | Not run; currently blocked by Docker. |
| 11 | Privacy/security negative | `rtk npm run release:privacy-security` | Selected negative suites exit 0. | Not run. |
| 12 | AI eval artifacts | `rtk npm run release:ai-eval` | Eval JSON, prompts, and schemas validate without live calls. | Not run. |
| 13 | Security/dependency refresh | `rtk npm audit --omit=dev`; `rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check` | Exit 0 or bounded risk acceptance with owner/review date. | Not run. |
| 14 | Deployment config validation | `rtk mvn -f services/core-api/pom.xml -Dtest=DeploymentArtifactsContractTest,DeploymentEnvironmentValidatorTest,DeploymentEnvironmentConfigurationTest test` | Env docs and fail-fast validator tests pass. | Not run. |
| 15 | Ops evidence | Backup/restore runbook rerun or current artifact; rollback target; incident/observability dry run; external provider/manual-channel status. | Current RC1 evidence exists, or explicit scoped nonclaim/risk acceptance exists. | Not run. |
| 16 | Pilot browser E2E | `rtk npm run release:e2e:pilot` | Isolated Postgres readiness, `/health`, Playwright pass, API/container cleanup. | Not run; currently blocked by Docker. |
| 17 | Commercial closure | If not automated, run focused partial command: `rtk mvn -f services/core-api/pom.xml -Dtest=PlacementWorkflowServiceTest,ConsultantPlacementQueryServiceTest,CommissionWorkflowServiceTest,ReportingExportServicePolicyTest test`; then collect runtime/API plus DB/audit evidence manually. | Placement and commission have current runtime/API plus persistence or WorkflowEvent/audit evidence. Service tests alone are partial. | Not run. |
| 18 | Full release gate | `rtk npm run release:gate` | Exits 0 and prints `RELEASE_READY`; no browser E2E waiver for RC1 readiness. | Not run; currently blocked by Docker. |
| 19 | Manual golden path | Operate all golden-path steps through five portals; record screenshots/API/log/DB/WorkflowEvent evidence and transaction IDs; run post-walkthrough `rtk npm run pilot:data:validate`. | No P0 operator confusion; placement, commission, and audit trace have runtime plus persistence/audit evidence. | Not run. |
| 20 | Final RC1 decision | Update RC1 evidence log and capability map. | Decision is exactly one of the RC1 plan statuses and matches evidence. | Not available; RC1 artifacts missing. |

## Top 5 Improvement Actions For This Scope

1. Start or repair Docker and rerun the Docker/Testcontainers gate before attempting migration, isolated pilot-data, E2E, or full release-gate proof.
2. Add operations readiness gates to the RC1 final checklist: deployment config validation, secrets/provider status, observability incident dry run, backup/restore, rollback, and first-week monitoring.
3. Require a current manual CI E2E dispatch artifact or local `release:gate` artifact for RC1; do not treat default PR/push CI as RC1-ready evidence.
4. Add runtime/API plus persistence/audit evidence for placement, commission, owner revenue/accounting handoff, and admin/owner audit trace; service tests alone should remain `partial`.
5. Create and maintain `RC1-pilot-readiness-evidence.md`, `RC1-capability-map.md`, and `rc1-artifacts/` before any readiness decision; historical Task 42/58/60 evidence should be copied only as context, not as current pass proof.
