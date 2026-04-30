# AI Headhunting Transaction OS

AI Headhunting Transaction OS, also referred to in the codebase as Recruiting Transaction OS (`RTO`), is a monorepo for a governed recruiting platform with a React web app, a Spring Boot core API, shared contracts, and infrastructure notes.

## Current Status

- `apps/web` provides the Vite + React portal shell.
- `services/core-api` provides the main Java 21 Spring Boot backend with:
  - JWT-based authentication and persisted identity sessions
  - consultant and client-safe API boundaries
  - governed intake and document upload flows
  - Flyway-managed PostgreSQL schema migrations
  - regression and integration coverage with Testcontainers
- `packages/*` holds shared contracts, design-system scaffolding, and fixtures.

## Source Of Truth

Read these before making product, architecture, UI, AI, workflow, data model, or governance changes:

1. `docs/specs/CURRENT_SPEC.md`
2. `docs/specs/v2.1/product-spec-v2.1.md`
3. `docs/specs/v2.0/product-spec-v2.0.md`

`v2.1` is the current product source of truth. `v2.0` remains the preserved UI and portal baseline context.

## Repository Layout

- `apps/web` - Vite React TypeScript frontend shell
- `services/core-api` - Spring Boot core API
- `services/workers-go` - placeholder for future Go workers
- `packages/contracts` - shared schema and contract assets
- `packages/design-system` - design-system package boundary
- `packages/test-fixtures` - shared non-production fixtures
- `docs` - product specs, roadmap, architecture, testing, and security guidance
- `infra` - docker, postgres, and observability notes

## Requirements

- Node.js `20.19+`
- npm `10+`
- Java `21`
- Maven
- Docker, for PostgreSQL and integration-test dependencies

## Quick Start

Install workspace dependencies:

```bash
npm install
```

Run frontend checks:

```bash
npm run typecheck:web
npm run build:web
```

Start the frontend locally:

```bash
npm run dev:web
```

Run backend tests:

```bash
mvn -f services/core-api/pom.xml test
```

Run the intended full verification gate:

```bash
npm run ci:intended
```

## Local Services

Start local PostgreSQL and MinIO helpers:

```bash
docker compose up -d
```

Backend auth and document upload features expect runtime configuration such as:

- `RTO_AUTH_JWT_SECRET`
- `RTO_DOCUMENT_STORAGE_ROOT_DIR`
- optional Flyway and port overrides in `services/core-api/src/main/resources/application.yml`

## Testing

Main validation commands:

```bash
npm run typecheck:web
npm run build:web
mvn -f services/core-api/pom.xml test
mvn -f services/core-api/pom.xml clean verify
```

The intended CI baseline is documented in `docs/testing/testing-strategy.md`.

## License

This repository is licensed under the MIT License. See `LICENSE`.
