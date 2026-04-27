# ADR-0001: Production Kernel Stack

## Status

Accepted for repository initialization.

## Context

The v2.1 specification defines a production-first Recruiting Transaction OS. It requires Java Core plus Go Tools, PostgreSQL as the source of truth, backend ownership of facts, and preserved five-portal UI boundaries from v2.0.

## Decision

Use this initial stack:

- Java 21 and Spring Boot 3.x for `services/core-api`.
- PostgreSQL as the target production database.
- Flyway migration location for database versioning.
- Vite, React, and TypeScript for `apps/web`.
- npm workspaces for frontend and shared package boundaries.
- Go worker directory reserved, with no Go implementation in Task 1.
- Docker Compose local PostgreSQL only, with no application containers until build artifacts and runtime contracts are defined.

## Consequences

- The core API can grow as a modular monolith before service extraction.
- PostgreSQL schema ownership is explicit from the first migration.
- Frontend starts as route shells and does not fake product state.
- CI is documented as intended commands because the repository did not yet contain a CI provider convention.
- AI model integration, Claim Ledger logic, disclosure logic, matching logic, and Go workers are deferred.
