# Task 39 Backup and Restore Runbook

This baseline documents the minimum backup/restore proof needed before a real pilot. The exact managed database service can change later.

## Backup

Run a logical backup from a trusted operator machine or managed job:

```sh
pg_dump "$SPRING_DATASOURCE_URL" --format=custom --file=rto-backup.dump
```

Store the dump in an access-controlled location outside the application container.

## Restore

1. Create a fresh empty PostgreSQL database.
2. Restore the dump:

```sh
pg_restore --dbname "$SPRING_DATASOURCE_URL" --clean --if-exists rto-backup.dump
```

3. Start `core-api` with `CORE_API_FLYWAY_ENABLED=true`.
4. Confirm `/health`.
5. Run `npm run pilot:data:validate` for pilot-seeded environments.

## Proof Required Before Pilot

- One successful backup file is produced.
- One restore into a separate database completes.
- The restored app can start.
- The restored app preserves audit records and does not require manual schema edits.
