# AI Headhunting Transaction OS

Production-first repository skeleton for the AI Headhunting Transaction OS / Recruiting Transaction OS.

## Source of Truth

Read these before any product, architecture, UI, AI, workflow, data model, or governance change:

1. `docs/specs/CURRENT_SPEC.md`
2. `docs/specs/v2.1/product-spec-v2.1.md`
3. `docs/specs/v2.0/product-spec-v2.0.md` for UI preservation and v2.0 baseline context

v2.1 is the current product source of truth. v2.0 remains the preserved UI and portal baseline.

## Repository Layout

- `apps/web` - Vite React TypeScript route shell for the five portals.
- `services/core-api` - Java 21 Spring Boot core API skeleton. Health endpoint only.
- `services/workers-go` - reserved for future Go auxiliary workers. No worker code yet.
- `packages/contracts` - reserved shared contracts boundary.
- `packages/design-system` - reserved design system boundary.
- `packages/test-fixtures` - reserved non-production fixture boundary.
- `docs/architecture` - production architecture and module boundary documents.
- `docs/decisions` - architecture decision records.
- `docs/security` - security and privacy guidance.
- `docs/testing` - testing and intended CI command guidance.
- `infra/docker` - Docker-related infrastructure notes.
- `infra/postgres` - PostgreSQL bootstrap notes.
- `infra/observability` - observability notes.

## Local Commands

Frontend:

```bash
npm install
npm run typecheck:web
npm run build:web
npm run dev:web
```

Backend, once Java 21 and Maven are installed:

```bash
mvn -f services/core-api/pom.xml test
mvn -f services/core-api/pom.xml clean verify
```

Local PostgreSQL:

```bash
docker compose up -d postgres
```

## CI Baseline

This repository did not have an existing CI convention when initialized. The intended CI gate is documented in `docs/testing/testing-strategy.md`; no workflow file is added until a CI provider convention is chosen.
