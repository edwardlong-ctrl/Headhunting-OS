# Task 39 Rollback Runbook

This runbook keeps rollback provider-neutral. It assumes the deployment platform can start a previous image and point it at the same PostgreSQL database.

## Rollback Steps

1. Stop new traffic to the unhealthy release.
2. Capture logs, active image tags, `SPRING_PROFILES_ACTIVE`, and the current Flyway schema version.
3. Restore the previous image for `core-api` and the previous image for `web`.
4. Restart the previous image with the same secret set and database connection.
5. Run `/health`.
6. Run the staging smoke test if the environment is not production.
7. Keep the failed image tag and logs for incident review.

## Database Rule

- do not hand-edit production schema.
- Do not delete WorkflowEvent, ReviewEvent, ClaimLedgerItem, ConsentRecord, DisclosureRecord, or AITaskRun records to hide a failed release.
- If a migration must be reversed, create a new forward migration after review. Already-applied Flyway migrations are immutable.
