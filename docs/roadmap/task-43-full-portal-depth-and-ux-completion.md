# Task 43 Full Portal Depth and UX Completion

Current result: **completed for the Task 43 route-depth gate**.

Task 43 extends the Task 42 controlled-pilot surface from pilot paths to the
v2.0/v2.1 named portal route set. It does not certify public production
operation and does not complete Task 44 AI registry production coverage.

## Delivered Scope

- Added a frontend route contract test for the v2.0/v2.1 named Owner,
  Consultant, Client, Candidate, and Admin routes.
- Completed missing Client route entries:
  `/client/dashboard`, `/client/profile`, `/client/jobs/new/ai-intake`,
  `/client/jobs/:jobId/shortlist`, `/client/unlock/:candidateId`, and
  `/client/follow-ups`.
- Completed missing Candidate route aliases:
  `/candidate/upload`, `/candidate/profile/ai-review`, and `/candidate/status`.
- Aligned strict spec route parameter names for client anonymous candidate
  review, candidate opportunity detail, and candidate consent request routes
  while preserving existing compatibility aliases.
- Completed missing Admin route entry:
  `/admin/integrations`.
- Connected the new Client surfaces to existing backend-owned APIs for
  dashboard, profile, job intake/status, shortlist review, unlock requests, and
  follow-up notifications.
- Connected `/admin/integrations` to the existing governance read boundary via
  `GovernanceReadService` and `AdminGovernanceController`.

## Validation Evidence

- `rtk npm --workspace @rto/web run test -- portalRouteContract.test.ts`
- `rtk npm --workspace @rto/web run test`
- `rtk npm run typecheck:web`
- `rtk npm run build:web`
- `rtk git diff --check`
- `rtk mvn -f services/core-api/pom.xml -Dtest=AdminGovernanceControllerMappingTest,GovernanceReadServicePostgresIntegrationTest#onlyRuntimeWiredAdminSectionsAreEditable test`
- `rtk mvn -f services/core-api/pom.xml -Dtest=AdminGovernanceControllerMappingTest test`
- `rtk mvn -f services/core-api/pom.xml -Dtest=GovernanceReadServicePostgresIntegrationTest#onlyRuntimeWiredAdminSectionsAreEditable test`
- `rtk mvn -f services/core-api/pom.xml test` (1029 tests, 0 failures, 0 errors, 3 skipped)
- Browser smoke on `http://127.0.0.1:5173` for `/client/dashboard`,
  `/client/candidates/card_demo`, `/candidate/profile/ai-review`,
  `/candidate/opportunities/demo-opportunity`, `/candidate/consent/demo-request`,
  and `/admin/integrations`; unauthenticated routes rendered their guarded
  sign-in/admin shell states without blank pages.

## Remaining Work After Task 43

- Task 44 must still broaden the AI Task Registry to full production coverage.
- Later Tasks 45-60 still own workflow automation, data lifecycle depth,
  integrations production wiring, support operations, production hardening, and
  final full-product acceptance.
