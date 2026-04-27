# Testing Strategy

## Current Task 1 Gates

Frontend:

```bash
npm install
npm run typecheck:web
npm run build:web
```

Backend, after Java 21 and Maven are installed:

```bash
mvn -f services/core-api/pom.xml test
mvn -f services/core-api/pom.xml clean verify
```

## Intended CI Baseline

No CI provider convention existed in this repository at initialization time, so Task 1 documents intended commands instead of adding a workflow file.

Recommended first CI job sequence:

1. Install Node.js 20.19 or newer.
2. Run `npm ci`.
3. Run `npm run typecheck:web`.
4. Run `npm run build:web`.
5. Install Java 21 and Maven.
6. Run `mvn -f services/core-api/pom.xml clean verify`.

## Future Test Layers

- Contract tests for shared schemas before API implementation.
- Core API unit tests for domain service gates.
- Integration tests against PostgreSQL for Flyway migrations and repository behavior.
- Access-control tests proving client-safe candidate boundaries.
- Workflow tests proving every key transition emits WorkflowEvent.
- AI governance tests proving AI outputs remain claims until approved.
- Browser tests only after real product screens exist.

## Out of Scope for Task 1

- Matching tests
- Consent or disclosure tests
- Claim Ledger tests
- AI provider tests
- Go worker tests
- Demo data tests
