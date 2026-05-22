# Module Boundaries

## apps/web

The Vite/React web app owns browser routing, portal composition, and the
current five-portal user surfaces for Owner, unified Consultant, Client,
Candidate, and Admin workflows. It still does not own facts, permissions,
workflow transitions, matching decisions, disclosure decisions, or AI task
execution.

The preserved v2.0/v2.1 portal taxonomy is:

- `/owner`
- `/consultant`
- `/client`
- `/candidate`
- `/admin`

Consultant is one unified portal. The v2.0 Consultant design boards are historical UI references for different modules inside the same portal, not separate consultant products.

## services/core-api

Java 21 Spring Boot modular monolith. It owns the domain service boundary,
write gates, audit requirements, workflow events, access policy enforcement,
AI task governance, integration boundaries, release-safe APIs, and PostgreSQL
persistence adapters.

Current scope includes the Task 60 accepted v2.1/v2.0 product baseline:
governed intake, document intelligence, AI task registry/runner, matching,
shortlist, consent/disclosure/unlock, workflow legality and automation,
placement/commission, governance consoles, reporting/export, support
operations, tenant hardening, release gates, and acceptance evidence. Public
SaaS operation, managed-cloud signoff, live provider activation, formal
certification, and customer go-live remain outside this module-boundary
document's current acceptance claim.

## services/workers-go

Reserved for future high-throughput auxiliary services such as file conversion, OCR/STT routing, batch embeddings, crawler/enrichment workers, notification gateways, or matching pre-computation.

No Go runtime module is required for the current Task 60 v2.1/v2.0 acceptance
gate. Future workers must remain auxiliary and must not become a fact source.

## packages/contracts

Shared schema and API contract assets. Contracts are the shared language across
frontend, backend, tests, release checks, and future workers.

## packages/design-system

Reserved for reusable UI primitives and tokens. It must not encode product state or role-specific workflow behavior.

## packages/test-fixtures

Reserved for synthetic non-production fixtures and contract-test payloads. Fixtures must never become runtime truth.

## infra

Infrastructure is split by responsibility:

- `infra/docker` for local container notes.
- `infra/postgres` for database bootstrap notes and schema namespace ownership.
- `infra/observability` for future logging, tracing, metrics, and AI task run observability.
