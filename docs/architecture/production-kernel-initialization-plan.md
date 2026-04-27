# Production Kernel Initialization Plan

## Task 1 Scope

Task 1 establishes a production repository skeleton, build tooling baseline, CI command baseline, and architecture documentation. It intentionally does not implement product features, business workflows, matching, consent, disclosure, Claim Ledger logic, AI model integration, demo shortcuts, or Go workers.

## Source Inputs

- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/specs/v2.0/product-spec-v2.0.md` for preserved UI and portal baseline context

## Initial Production Kernel

The production kernel is organized around the v2.1 rule that backend and PostgreSQL own truth:

- Java 21 Spring Boot modular monolith for the future core domain and governance layer.
- PostgreSQL as target source of truth.
- Flyway migration location prepared with schema namespaces only.
- Vite React TypeScript frontend with five portal route shells only.
- Go worker directory reserved for future auxiliary workers only.
- Shared package boundaries reserved before contracts, design system, and fixtures exist.

## Repository Initialization Steps

1. Preserve all existing specification files.
2. Create production repository directories and tracked boundary files.
3. Add root package management and intended CI commands.
4. Add safe local PostgreSQL Compose service.
5. Add backend health-only Spring Boot skeleton.
6. Add frontend route shells for `/owner`, `/consultant`, `/client`, `/candidate`, and `/admin`.
7. Add architecture, security, testing, and ADR documents.

## Guardrails

- Consultant remains one unified portal.
- Client must never read raw Candidate objects before future unlock/disclosure logic exists.
- AI outputs are future claims, not facts.
- Backend owns truth; frontend route shells must not create fake product state.
- No workflow, consent, disclosure, matching, placement, commission, Claim Ledger, or AI orchestration behavior is implemented in Task 1.

## Next Task Boundary

The recommended next task is to create the contracts-first domain specification and database table plan for the truth layer without implementing workflow behavior. That task should start from v2.1 sections 13, 14, 17, 19, and 20.
