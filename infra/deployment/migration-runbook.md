# Task 39 Migration Runbook

This is the provider-neutral baseline for moving an empty database into a staging or production-like environment. It does not create cloud resources and does not replace product specs.

## Empty Database Path

1. Create an empty PostgreSQL database for the target environment.
2. Set `SPRING_PROFILES_ACTIVE=staging` or `production`.
3. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` from the target secret store.
4. Set `CORE_API_FLYWAY_ENABLED=true`.
5. Start the `core-api` image once and let Flyway apply the migrations from `services/core-api/src/main/resources/db/migration`.
6. Confirm `/health` returns `UP`.
7. Import pilot seed data only for staging or controlled pilot environments:

```sh
npm run pilot:data:import
```

8. Validate the seeded baseline:

```sh
npm run pilot:data:validate
```

## Rules

- Do not hand-edit production schema.
- Do not skip Flyway validation.
- Do not load real personal data into the Task 39 production-like baseline.
- Keep Task 38 pilot seed data separate from any future real customer import.
