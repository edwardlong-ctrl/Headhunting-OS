# Task 39 Backup and Restore Runbook

This baseline documents the minimum backup/restore proof needed before a real pilot. The exact managed database and object storage services can change later.

## Required Backup Inputs

- `POSTGRES_BACKUP_URL`: PostgreSQL CLI connection URL, for example `postgresql://rto_app@postgres:5432/recruiting_os`. Do not pass the Spring JDBC `SPRING_DATASOURCE_URL` value to `pg_dump` or `pg_restore`.
- `PGPASSWORD`: database password from the target secret store, if the URL does not include a password.
- `RTO_DOCUMENT_STORAGE_BACKUP_PATH`: filesystem path or mounted object-storage export path for the document storage root or bucket.

## Backup

Run a logical PostgreSQL backup from a trusted operator machine or managed job:

```sh
pg_dump "$POSTGRES_BACKUP_URL" --format=custom --file=rto-postgres.dump
```

Document storage backup must be captured in the same backup set. For the Task 39 local-production equivalent, archive the mounted document storage directory:

```sh
tar -C "$RTO_DOCUMENT_STORAGE_BACKUP_PATH" -czf rto-documents.tar.gz .
```

For MinIO or future external object storage, export the configured bucket with provider tooling and keep the bucket snapshot/version aligned with `rto-postgres.dump`.

Store the database dump and document/object-storage backup in an access-controlled location outside the application container.

## Restore

1. Create a fresh empty PostgreSQL database.
2. Restore the database dump:

```sh
pg_restore --dbname "$POSTGRES_BACKUP_URL" --clean --if-exists rto-postgres.dump
```

3. Document storage restore must run before smoke validation. For the Task 39 local-production equivalent, restore the document archive into the configured document storage path:

```sh
mkdir -p "$RTO_DOCUMENT_STORAGE_BACKUP_PATH"
tar -C "$RTO_DOCUMENT_STORAGE_BACKUP_PATH" -xzf rto-documents.tar.gz
```

For MinIO or future external object storage, restore the bucket snapshot/version that belongs to the database dump.

4. Start `core-api` with `CORE_API_FLYWAY_ENABLED=true`.
5. Confirm `/health`.
6. Run `npm run pilot:data:validate` for pilot-seeded environments.

## Proof Required Before Pilot

- One successful PostgreSQL backup file is produced.
- One document/object-storage backup is produced.
- One restore into a separate database completes.
- One document/object-storage restore completes.
- The restored app can start.
- The restored app preserves audit records and does not require manual schema edits.
- A restored source document referenced by an intake `storageRef` can be retrieved or parsed.
