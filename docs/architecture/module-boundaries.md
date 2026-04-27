# Module Boundaries

## apps/web

Frontend route shell only. It owns browser routing, application frame setup, and future portal composition. It does not own facts, permissions, workflow transitions, matching decisions, disclosure decisions, or AI task execution.

Task 1 includes only these route shells:

- `/owner`
- `/consultant`
- `/client`
- `/candidate`
- `/admin`

Consultant is one unified portal. The v2.0 Consultant design boards are historical UI references for different modules inside the same portal, not separate consultant products.

## services/core-api

Future Java 21 Spring Boot modular monolith. It will own the domain service boundary, write gates, audit requirements, workflow events, and access policy enforcement.

Task 1 includes only:

- Spring Boot application entrypoint
- `/health` endpoint
- application configuration
- Flyway migration location and schema namespace migration

No business logic exists here yet.

## services/workers-go

Reserved for future high-throughput auxiliary services such as file conversion, OCR/STT routing, batch embeddings, crawler/enrichment workers, notification gateways, or matching pre-computation.

Task 1 does not create Go runtime code.

## packages/contracts

Reserved for shared schema and API contracts. Contracts must be the only shared language across frontend, backend, tests, and future workers.

Task 1 does not define product data models yet.

## packages/design-system

Reserved for reusable UI primitives and tokens. It must not encode product state or role-specific workflow behavior.

Task 1 does not create design components yet.

## packages/test-fixtures

Reserved for synthetic non-production fixtures and contract-test payloads. Fixtures must never become runtime truth.

Task 1 does not add demo data.

## infra

Infrastructure is split by responsibility:

- `infra/docker` for local container notes.
- `infra/postgres` for database bootstrap notes and schema namespace ownership.
- `infra/observability` for future logging, tracing, metrics, and AI task run observability.
