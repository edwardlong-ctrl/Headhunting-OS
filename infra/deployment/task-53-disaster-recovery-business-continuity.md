# Task 53 Disaster Recovery and Business Continuity

Task 53 proves the provider-neutral DR/BCP baseline. It does not claim managed
cloud production readiness, multi-region failover, or external vendor SLAs.

Do not fake restore success. If a live restore cannot be completed, record the
blocked command, exact error, and rerun command before marking the drill passed.

## Scope

- Backup schedule.
- Restore drill.
- Migration rollback drill.
- Object storage recovery.
- AI provider outage playbook.
- Notification provider outage playbook.
- Incident severity levels.

Backend truth and audit invariants still apply during incidents:

- PostgreSQL remains the source of truth.
- WorkflowEvent, ClaimLedger, AITaskRun, ConsentRecord, DisclosureRecord, and
  ReviewEvent records must not be deleted or rewritten to hide a failed release.
- Recovery can restore a prior backup into a fresh environment, but it must not
  hand-edit domain or governance rows.

## Backup Schedule

Minimum pilot schedule:

| Asset | Schedule | Retention | Required proof |
| --- | --- | --- | --- |
| PostgreSQL logical backup | Daily before business hours and before each release | 14 daily, 8 weekly | `pg_dump --format=custom` file exists outside the app container |
| Document/object storage snapshot | Daily, same backup set as PostgreSQL | Same retention as matching DB dump | archive or bucket mirror exists with backup timestamp |
| Pre-migration backup | Immediately before any Flyway migration in staging or production | Until release is accepted plus one rollback window | DB dump and document/object snapshot are both restorable |
| Incident export | At incident open and close for Sev1/Sev2 | Incident retention policy | logs, image tags, schema version, and audit counts captured |

Backup sets must be named so the database dump and document/object snapshot can
be matched by timestamp. A database-only backup is not a complete recovery
artifact for this product because source documents are part of the evidence
chain.

## Restore Drill

Baseline runbook: `infra/deployment/backup-restore-runbook.md`.

Task 53 local drill date: 2026-05-10.

Source environment:

- Existing local Docker Postgres container: `rto-postgres`.
- Isolated source database: `recruiting_os_task53_source_20260510`.
- Isolated restore database: `recruiting_os_task53_restore_20260510`.
- Source document root: `/tmp/rto-task53-docstore-source-20260510`.
- Restored document root: `/tmp/rto-task53-docstore-restore-20260510`.
- Local evidence artifacts: `artifacts/task53-dr-bcp-20260510/`.

Commands executed:

```sh
rtk docker exec rto-postgres dropdb -U recruiting_os --if-exists recruiting_os_task53_source_20260510
rtk docker exec rto-postgres dropdb -U recruiting_os --if-exists recruiting_os_task53_restore_20260510
rtk docker exec rto-postgres createdb -U recruiting_os recruiting_os_task53_source_20260510
rtk env SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/recruiting_os_task53_source_20260510 SPRING_DATASOURCE_USERNAME=recruiting_os SPRING_DATASOURCE_PASSWORD=recruiting_os_local_password npm run pilot:data:rebuild
rtk docker exec rto-postgres psql -U recruiting_os -d recruiting_os_task53_source_20260510 -tAc "SELECT storage_ref FROM intake.source_item WHERE storage_ref IS NOT NULL ORDER BY storage_ref LIMIT 1"
rtk mkdir -p /tmp/rto-task53-docstore-source-20260510/pilotdata
rtk proxy sh -c 'printf "%s\n" "Task 53 DR restore document - created 2026-05-10 for isolated local drill." > /tmp/rto-task53-docstore-source-20260510/pilotdata/candidate-resume-01.txt'
rtk docker exec rto-postgres pg_dump postgresql://recruiting_os:recruiting_os_local_password@127.0.0.1:5432/recruiting_os_task53_source_20260510 --format=custom --file=/tmp/task53-rto-postgres-20260510.dump
rtk tar -C /tmp/rto-task53-docstore-source-20260510 -czf artifacts/task53-dr-bcp-20260510/rto-documents.tar.gz .
rtk docker cp rto-postgres:/tmp/task53-rto-postgres-20260510.dump artifacts/task53-dr-bcp-20260510/rto-postgres.dump
rtk docker exec rto-postgres createdb -U recruiting_os recruiting_os_task53_restore_20260510
rtk docker exec rto-postgres pg_restore --dbname postgresql://recruiting_os:recruiting_os_local_password@127.0.0.1:5432/recruiting_os_task53_restore_20260510 --clean --if-exists /tmp/task53-rto-postgres-20260510.dump
rtk mkdir -p /tmp/rto-task53-docstore-restore-20260510
rtk tar -C /tmp/rto-task53-docstore-restore-20260510 -xzf artifacts/task53-dr-bcp-20260510/rto-documents.tar.gz
rtk env SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/recruiting_os_task53_restore_20260510 SPRING_DATASOURCE_USERNAME=recruiting_os SPRING_DATASOURCE_PASSWORD=recruiting_os_local_password npm run pilot:data:validate
rtk proxy test -f /tmp/rto-task53-docstore-restore-20260510/pilotdata/candidate-resume-01.txt
rtk wc -c /tmp/rto-task53-docstore-restore-20260510/pilotdata/candidate-resume-01.txt
rtk docker exec rto-postgres psql -U recruiting_os -d recruiting_os_task53_restore_20260510 -tAc "SELECT storage_ref FROM intake.source_item WHERE storage_ref = 'pilotdata/candidate-resume-01.txt'"
```

Restore result:

- PostgreSQL dump created: `artifacts/task53-dr-bcp-20260510/rto-postgres.dump`, 343.0K observed.
- Document archive created: `artifacts/task53-dr-bcp-20260510/rto-documents.tar.gz`, 587B observed.
- Restored database: `recruiting_os_task53_restore_20260510`.
- Restored document: `/tmp/rto-task53-docstore-restore-20260510/pilotdata/candidate-resume-01.txt`.
- Restored document size: 75B.
- Restored source row still references `pilotdata/candidate-resume-01.txt`.
- `npm run pilot:data:validate` returned exit code 0 against the restored database.
- Restored validation counts: `candidates=75`, `jobs=8`, `sourceDocuments=83`, `accounts=5`.
- Restored validation checks: privacy checks passed, source documents present, current profiles linked, no shortcut shortlists, no shortcut disclosure records, and no seeded canonical write attempts.

Restored API boot check:

```sh
rtk env CORE_API_PORT=8095 SPRING_PROFILES_ACTIVE=staging CORE_API_FLYWAY_ENABLED=true SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/recruiting_os_task53_restore_20260510 SPRING_DATASOURCE_USERNAME=recruiting_os SPRING_DATASOURCE_PASSWORD=recruiting_os_local_password RTO_AUTH_JWT_SECRET=task53_restore_drill_jwt_secret_32_bytes_minimum RTO_DOCUMENT_STORAGE_ROOT_DIR=/tmp/rto-task53-docstore-restore-20260510 RTO_DOCUMENT_STORAGE_VIRUS_SCAN_MODE=noop RTO_AI_DEEPSEEK_API_KEY=task53-not-used RTO_FRONTEND_ORIGIN=https://task53.local.test RTO_PUBLIC_BASE_URL=https://task53.local.test RTO_DEPLOYMENT_DATABASE_MANAGED=false RTO_OBJECT_STORAGE_PROVIDER=local-filesystem RTO_OBJECT_STORAGE_LOCAL_ROOT_DIR=/tmp/rto-task53-docstore-restore-20260510 mvn -f services/core-api/pom.xml spring-boot:run
rtk curl -s -i http://127.0.0.1:8095/health
```

Result:

- Startup signal observed: `Started CoreApiApplication`.
- Flyway observed restored schema at version 32 and reported no migration necessary.
- Health check returned HTTP 200 with `{"service":"core-api","status":"UP"}`.
- Process was stopped after the health check. Maven reported exit 143 because the verified server process was terminated intentionally.

Historical pilot restore evidence remains relevant for non-clean business-state
restore proof:

- `artifacts/task42-backup-restore-20260509/evidence.md`
- That prior evidence restored a post-E2E business-state database and document
  file, started the API, and observed restored audit/event counts including
  WorkflowEvent rows.

## Migration Rollback Drill

Baseline runbook: `infra/deployment/rollback-runbook.md`.

Task 53 local rollback drill did not reverse an already-applied Flyway migration.
That is intentional: applied Flyway migrations are immutable and the safe local
rollback action is to restore a pre-migration backup into a separate database or
roll back the app image while preserving the database audit trail.

Command executed:

```sh
rtk docker exec rto-postgres psql -U recruiting_os -d recruiting_os_task53_restore_20260510 -tAc "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1"
```

Result:

- Restored schema version: `32|t`.
- App startup against the restored database validated all 32 migrations and
  reported no migration necessary.
- Rollback invariant preserved: no production schema was hand-edited, and no
  WorkflowEvent, ClaimLedger, AITaskRun, DisclosureRecord, ConsentRecord, or
  ReviewEvent rows were deleted.

Operational migration rollback path:

1. Open Sev2 or Sev1 incident depending on user impact.
2. Stop new traffic to the failed release.
3. Capture current image tags, schema version, logs, request IDs, and failed
   migration error.
4. If migration did not apply, restart previous app image against the unchanged
   database and run smoke checks.
5. If migration applied and corrupted the environment, restore the pre-migration
   backup into a fresh database and point the previous app image at that restored
   database after incident lead approval.
6. If data was written after the backup point, preserve the failed database for
   audit and reconciliation. Do not delete audit rows to make the rollback look
   clean.
7. Fix forward with a reviewed migration. Do not edit an applied migration file.

## Object Storage Recovery

Document archive restore was proven by extracting
`artifacts/task53-dr-bcp-20260510/rto-documents.tar.gz` into
`/tmp/rto-task53-docstore-restore-20260510` and verifying the restored file.

MinIO object-storage drill:

Initial attempt:

```sh
rtk docker run --rm --network container:rto-minio --entrypoint sh -v /tmp/rto-task53-docstore-restore-20260510:/restore:ro minio/mc -c "mc alias set local http://127.0.0.1:9000 minioadmin minioadmin >/dev/null && mc mb --ignore-existing local/rto-task53-dr-bcp && mc cp /restore/pilotdata/candidate-resume-01.txt local/rto-task53-dr-bcp/pilotdata/candidate-resume-01.txt && mc stat local/rto-task53-dr-bcp/pilotdata/candidate-resume-01.txt"
```

Blocked result:

- Docker could not pull `minio/mc:latest` due to the unauthenticated Docker Hub
  pull-rate limit.

Completed local workaround using the existing `rto-minio` container:

```sh
rtk docker cp /tmp/rto-task53-docstore-restore-20260510/pilotdata/candidate-resume-01.txt rto-minio:/tmp/task53-candidate-resume-01.txt
rtk docker exec rto-minio mc alias set local http://127.0.0.1:9000 minioadmin minioadmin
rtk docker exec rto-minio mc mb --ignore-existing local/rto-task53-dr-bcp
rtk docker exec rto-minio mc cp /tmp/task53-candidate-resume-01.txt local/rto-task53-dr-bcp/pilotdata/candidate-resume-01.txt
rtk docker exec rto-minio mc stat local/rto-task53-dr-bcp/pilotdata/candidate-resume-01.txt
rtk docker exec rto-minio mc cp local/rto-task53-dr-bcp/pilotdata/candidate-resume-01.txt /tmp/task53-candidate-resume-01-recovered.txt
rtk docker cp rto-minio:/tmp/task53-candidate-resume-01-recovered.txt artifacts/task53-dr-bcp-20260510/minio-recovered/candidate-resume-01.txt
rtk wc -c artifacts/task53-dr-bcp-20260510/minio-recovered/candidate-resume-01.txt
```

Result:

- Bucket created: `local/rto-task53-dr-bcp`.
- Object uploaded: `pilotdata/candidate-resume-01.txt`.
- `mc stat` returned size `75 B` and content type `text/plain`.
- Recovered object copied back to local artifact path.
- Recovered object size: 75B.

## AI Provider Outage Playbook

Current baseline:

- DeepSeek is the real provider baseline for executable AI tasks.
- Deterministic pilot provider exists for local pilot paths.
- AI outputs remain claims, not facts.
- AI task failures must be persisted as failed AITaskRun records with safe
  failure reasons.

Playbook:

1. Classify impact. If governed intake, matching, or interview feedback
   structuring is blocked for live users, open Sev2. If candidate/client
   disclosure or commercial commitments are at risk, escalate to Sev1.
2. Disable or pause affected AI-triggered workflow actions at the operator
   level. Do not allow AI to bypass CanonicalWriteGate, human review, consent,
   disclosure, or client-safe projection boundaries.
3. Keep intake, upload, manual review, shortlist drafting, consent, disclosure,
   and workflow transitions available where they do not require the failed
   provider.
4. Route demo/pilot-only checks to the deterministic provider only when the
   environment is explicitly pilot or test. Do not silently substitute
   deterministic output in production.
5. Record failed AITaskRun ids, provider error codes, model route, prompt
   version, schema version, request time, and correlation id.
6. After provider recovery, replay only tasks that are idempotent and still
   policy-allowed. Re-run schema validation before any human review surface.
7. Incident notes must state whether any AI output was shown to users, whether
   any write-back was blocked, and whether any user-visible follow-up is needed.

## Notification Provider Outage Playbook

Current baseline:

- Task 34 has notification persistence, schedules, delivery attempts, and no-op
  email/SMS provider abstractions.
- Real external email/SMS/WeChat delivery is still not production-wired.
- Candidate and client actions that affect truth still require backend service
  paths and workflow audit.

Playbook:

1. Classify impact. Missed reminders with manual workaround are usually Sev3.
   Consent, disclosure, interview, offer, or client clarification notifications
   that block live transactions are Sev2. A privacy-impacting misdelivery is
   Sev1.
2. Stop external sends for the affected channel if delivery integrity is
   uncertain.
3. Keep in-app notification records and schedules append-only. Do not delete
   failed attempts.
4. Use consultant/manual outreach for time-critical consent, disclosure,
   interview, and offer actions. Record the manual outreach as a workflow or
   follow-up note where the product path supports it.
5. Preserve delivery attempt ids, recipient role, channel, provider error,
   request id, and next retry time.
6. Retry only after provider health is verified and duplicate-send risk is
   assessed. For consent/disclosure/commercial notices, require operator review
   before resend.
7. Close the incident only after affected notifications are either delivered,
   manually handled, or explicitly canceled with a recorded reason.

## Incident Severity Levels

| Severity | Definition | Examples | Initial response target | Required actions |
| --- | --- | --- | --- | --- |
| Sev1 | Active privacy, data integrity, security, or transaction-critical outage | raw candidate exposure, broken disclosure gate, corrupted restore, failed production migration with writes | 15 minutes | incident lead, stop unsafe traffic, preserve evidence, executive update |
| Sev2 | Major workflow or operational capability unavailable with workaround | AI provider down for live intake, notification outage blocking consent, restore needed for staging/pilot | 1 hour | owner, runbook execution, status updates, post-incident notes |
| Sev3 | Degraded non-critical workflow with manual workaround | delayed reminders, slow AI tasks, partial object-store export failure with source intact | 1 business day | ticket, workaround, fix owner |
| Sev4 | Low-risk operational issue or documentation drift | stale runbook command, missing evidence link, non-user-facing warning | next planning cycle | backlog item or docs fix |

## Rerun Commands

Minimum rerun gate for this task:

```sh
rtk git diff --check
rtk docker info
rtk mvn -f services/core-api/pom.xml -Dtest=DeploymentArtifactsContractTest,DeploymentEnvironmentValidatorTest,DeploymentEnvironmentConfigurationTest test
rtk mvn -f services/core-api/pom.xml test
```

Restore rerun:

```sh
rtk docker exec rto-postgres dropdb -U recruiting_os --if-exists recruiting_os_task53_restore_20260510
rtk docker exec rto-postgres createdb -U recruiting_os recruiting_os_task53_restore_20260510
rtk docker exec rto-postgres pg_restore --dbname postgresql://recruiting_os:recruiting_os_local_password@127.0.0.1:5432/recruiting_os_task53_restore_20260510 --clean --if-exists /tmp/task53-rto-postgres-20260510.dump
rtk tar -C /tmp/rto-task53-docstore-restore-20260510 -xzf artifacts/task53-dr-bcp-20260510/rto-documents.tar.gz
rtk env SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/recruiting_os_task53_restore_20260510 SPRING_DATASOURCE_USERNAME=recruiting_os SPRING_DATASOURCE_PASSWORD=recruiting_os_local_password npm run pilot:data:validate
```

## Remaining Gaps

- No managed cloud database backup policy has been executed.
- No managed object storage versioning or cross-region replication has been
  configured.
- No real production email/SMS/WeChat provider has been failed over.
- No production AI multi-provider failover has been executed.
- No public production incident communications process has been tested.
- Local drill uses synthetic pilot data and local credentials only.

