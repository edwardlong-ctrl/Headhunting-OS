# Task 42 Backup / Restore Evidence - 2026-05-09

## Scope

This evidence captures a Task 39-style backup / restore drill for the Task 42
pilot E2E runtime after the S01-S08 browser flow suite had been executed.

## Backup Artifacts

- PostgreSQL custom dump: `artifacts/task42-backup-restore-20260509/rto-postgres.dump`
- Clean-seed PostgreSQL custom dump: `artifacts/task42-backup-restore-20260509/rto-postgres-clean-seed.dump`
- Document storage archive: `artifacts/task42-backup-restore-20260509/rto-documents.tar.gz`

Observed artifact sizes:

- `rto-postgres.dump`: 358.2K
- `rto-postgres-clean-seed.dump`: 340.1K
- `rto-documents.tar.gz`: 9.4K

## Commands Executed

```sh
rtk docker exec rto-postgres pg_dump postgresql://recruiting_os:recruiting_os_local_password@127.0.0.1:5432/recruiting_os --format=custom --file=/tmp/task42-rto-postgres-20260509.dump
rtk docker cp rto-postgres:/tmp/task42-rto-postgres-20260509.dump artifacts/task42-backup-restore-20260509/rto-postgres.dump
rtk tar -C /tmp/rto-task42-docstore-8093 -czf artifacts/task42-backup-restore-20260509/rto-documents.tar.gz .
rtk docker exec rto-postgres createdb -U recruiting_os recruiting_os_task42_restore_20260509
rtk docker exec rto-postgres pg_restore --dbname postgresql://recruiting_os:recruiting_os_local_password@127.0.0.1:5432/recruiting_os_task42_restore_20260509 --clean --if-exists /tmp/task42-rto-postgres-20260509.dump
rtk tar -C /tmp/rto-task42-docstore-restore-20260509 -xzf artifacts/task42-backup-restore-20260509/rto-documents.tar.gz
rtk env SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/recruiting_os SPRING_DATASOURCE_USERNAME=recruiting_os SPRING_DATASOURCE_PASSWORD=recruiting_os_local_password npm run pilot:data:rebuild
rtk docker exec rto-postgres pg_dump postgresql://recruiting_os:recruiting_os_local_password@127.0.0.1:5432/recruiting_os --format=custom --file=/tmp/task42-clean-rto-postgres-20260509.dump
rtk docker cp rto-postgres:/tmp/task42-clean-rto-postgres-20260509.dump artifacts/task42-backup-restore-20260509/rto-postgres-clean-seed.dump
rtk docker exec rto-postgres dropdb -U recruiting_os --if-exists recruiting_os_task42_clean_restore_20260509
rtk docker exec rto-postgres createdb -U recruiting_os recruiting_os_task42_clean_restore_20260509
rtk docker exec rto-postgres pg_restore --dbname postgresql://recruiting_os:recruiting_os_local_password@127.0.0.1:5432/recruiting_os_task42_clean_restore_20260509 --clean --if-exists /tmp/task42-clean-rto-postgres-20260509.dump
rtk env SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/recruiting_os_task42_clean_restore_20260509 SPRING_DATASOURCE_USERNAME=recruiting_os SPRING_DATASOURCE_PASSWORD=recruiting_os_local_password npm run pilot:data:validate
```

## Restore Checks

- Restore database: `recruiting_os_task42_restore_20260509`
- Restore API port: `8094`
- Restore API health check: `GET /health -> 200`, body `{"status":"UP","service":"core-api"}`
- Restored governance review events: `1`
- Restored workflow events: `28`
- Restored intake source items: `84`
- Restored source document file:
  `/tmp/rto-task42-docstore-restore-20260509/00000000-0000-0000-0000-000000380001/2d3bf317-c2b3-44e3-ac0b-c25ee957d8ad/sha256:8d187c08b/s01-pilot-candidate.txt`
- Restored source document size: `225B`
- Clean restore database: `recruiting_os_task42_clean_restore_20260509`
- Clean restore `npm run pilot:data:validate`: exit code `0`
- Clean restore counts: `candidates=75`, `jobs=8`, `sourceDocuments=83`

## Important Caveat

`npm run pilot:data:validate` was executed against the post-E2E restored database and
returned exit code `2`. This is expected for this snapshot because the backup
was taken after the S01-S08 E2E suite mutated the pilot seed baseline:

- candidate count became `77` instead of clean seed `75`
- seeded shortlists became `1`
- disclosure records became `2`
- canonical write attempts became `1`

This means the restore drill proves backup production, restore execution,
restored app startup, audit preservation, and restored document availability for
the post-E2E business state. The separate clean-seed restore drill above proves
that a clean Task 38 pilot seed backup can be restored and still satisfy the
seed validator.
