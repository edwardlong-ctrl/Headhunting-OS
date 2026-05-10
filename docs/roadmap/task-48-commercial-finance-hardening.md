# Task 48 Commercial and Finance Operations Hardening

## Scope

Task 48 hardens the placement-to-paid commercial workflow without replacing the official accounting system.

Implemented behavior:

- Placement creation can capture a backend-owned fee agreement snapshot: active flag, reference, and payment terms.
- Invoice readiness is blocked unless the placement has a confirmed fee agreement snapshot.
- Invoice ready, invoice sent, paid, guarantee active, guarantee completed, and replacement required states remain placement workflow states and continue to be audited through `WorkflowEvent`.
- Pending commission creation from invoice readiness carries calculation inputs from the placement fee agreement snapshot: salary amount, fee rate, expected fee amount, fee agreement reference, payment terms, and calculation source.
- Owner placement reporting exposes fee agreement state, invoice readiness, and accounting export readiness.
- Owner revenue reporting separates invoice ready, invoice sent, paid placement, active guarantee, completed guarantee, and replacement counts.
- Owner accounting export is a read-only CSV handoff with an explicit disclaimer that it does not replace the official accounting system.

## Audit Boundary

State transitions still go through `PlacementWorkflowService` / `CommissionWorkflowService` and create `WorkflowEvent` via `WorkflowTransitionAuditService`.

The accounting export endpoint is read-only and derived from placement and commission read models. It does not mutate commercial state, does not write confirmed facts, and therefore does not create a workflow transition event.

## Out of Scope

- No broad export framework. Task 57 owns generic export/legal packages.
- No support tooling. Task 56 owns support actions.
- No tenant-wide finance hardening. Task 51 owns multi-org hardening.
- No invoice issuing, payment collection, tax handling, GL posting, or accounting-system replacement.

## Verification Evidence

Focused backend TDD loop:

- `rtk mvn -f services/core-api/pom.xml -Dtest=PlacementWorkflowServiceTest,CommissionWorkflowServiceTest,OwnerPlacementQueryServiceTest,OwnerRevenueQueryServiceTest test`
- Result: 17 tests, 0 failures, 0 errors, 0 skipped.

Focused web verification:

- `rtk npm --workspace @rto/web run test -- OwnerPortal.test.tsx`
- Result: 1 test file passed, 6 tests passed.

TypeScript:

- `rtk npm run typecheck:web`
- Result: passed.

Full verification:

- `rtk npm --workspace @rto/web run test`
- Result: 9 test files passed, 38 tests passed.
- `rtk npm run build:web`
- Result: passed.
- `rtk docker info`
- Result: Docker client/server reachable.
- `rtk mvn -f services/core-api/pom.xml test`
- Result: 1067 tests, 0 failures, 0 errors, 3 skipped.
- `rtk git diff --check`
- Result: passed.
