# PostgreSQL Baseline

PostgreSQL is the target source of truth for the production system.

## Local Startup

```bash
docker compose up -d postgres
```

The root Compose file starts only a local PostgreSQL container. It does not start application services.

## Migration Location

Core API migration files live under:

```text
services/core-api/src/main/resources/db/migration
```

## Deployment Migration Path

Task 39 keeps PostgreSQL provider-neutral. For staging and production-like environments, set:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `CORE_API_FLYWAY_ENABLED=true`

Then start `core-api` with the `staging` or `production` profile. The profile validator fails fast unless the deployment inputs are explicit.

The full empty-database process is documented in:

```text
infra/deployment/migration-runbook.md
```
