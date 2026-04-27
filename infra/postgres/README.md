# PostgreSQL Baseline

PostgreSQL is the target source of truth for the production system.

## Local Startup

```bash
docker compose up -d postgres
```

The root Compose file starts only a local PostgreSQL container. It does not start application services.

## Initial Schema Namespaces

The first Flyway migration prepares schema namespaces only:

- `identity`
- `recruiting`
- `governance`
- `workflow`
- `privacy`
- `audit`

No business tables are created in Task 1.

## Migration Location

Core API migration files live under:

```text
services/core-api/src/main/resources/db/migration
```
