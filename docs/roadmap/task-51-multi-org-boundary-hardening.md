# Task 51: Multi-organization Boundary Hardening

Date: 2026-05-10

Branch: `codex/task-51-multi-org-boundary`

## Scope

Task 51 hardens tenant boundaries across existing backend-owned product
surfaces. It does not add broad new integration, import, support, reporting, or
legal-audit product features beyond the tenant-boundary enforcement required to
keep those future surfaces safe.

Acceptance target:

- A user from one organization cannot infer, search, export, or access another
  organization's raw or derived private data through existing APIs, audit
  readers, exports, seed/import tooling, or admin-capable support policy.

## Protected Surfaces

- Database identity boundaries:
  `V33__harden_identity_tenant_boundaries.sql` adds an explicit
  `identity.user_account(user_account_id, organization_id)` unique parent key
  and same-organization composite foreign keys for role assignments, sessions,
  access audit logs, workflow events, review events, canonical write attempts,
  AI task runs, client unlock requests, notifications, notification
  preferences/schedules, follow-up submissions, and commission consultant links.
- Access-control policy:
  consultant product read/write access for company, job, shortlist, placement,
  and commission resources now requires explicit `SAME_ORGANIZATION` scope.
- Admin observability:
  access-audit search is tenant-aware, always scoped to the authenticated admin
  principal's organization, and returns only API-safe audit DTOs.
- Tenant-aware exports:
  Owner placement/revenue/accounting export paths defensively filter returned
  placement and commission rows to the requested organization before composing
  summaries or CSV content.
- Tenant-aware seed/import tooling:
  pilot dataset import preflight now fails closed when a seeded
  `identity.user_account` id is already bound to a different organization,
  before writing any organization data.
- Support/admin impersonation:
  `SupportImpersonationPolicy` defines the current Task 51 support boundary.
  It allows only same-organization Admin impersonation of non-governance roles,
  requires ticket reference, reason, and explicit break-glass approval, denies
  cross-organization targets, and records every allow/deny decision through
  access audit.

## Tests Added Or Extended

- `IdentityAuthPostgresIntegrationTest`
  - proves all 33 migrations apply;
  - proves the organization-scoped `user_account` parent key exists;
  - proves `identity.role_assignment` and `identity.session` reject user
    accounts from another organization.
- `JdbcAccessAuditRecorderPostgresIntegrationTest`
  - proves access-audit search is tenant-scoped and does not return another
    organization's audit row.
- `AdminObservabilityControllerPolicyTest`
  - proves admin access-audit search uses the authenticated principal's
    organization and the existing Admin governance read policy.
- `OwnerRevenueQueryServiceTest`
  - proves accounting export content filters out placement and commission rows
    that do not belong to the requested organization.
- `PermissionEnforcerTest`
  - proves consultant product read/write access fails closed without explicit
    same-organization scope.
- `PilotDataPostgresIntegrationTest`
  - proves pilot import fails closed before writes when seeded identity ids are
    already bound to another organization.
- `SupportImpersonationPolicyTest`
  - proves cross-organization support impersonation is denied and audited;
  - proves ticket, reason, break-glass approval, and non-governance target role
    requirements;
  - proves same-organization Admin support impersonation is explicitly audited.

Existing cross-organization and API boundary regression suites remain part of
the Task 51 safety net, including five-portal access-control regressions,
client-safe candidate-card cross-org query tests, consultant write
organization-isolation tests, consent/disclosure cross-org tests, product data
model composite-FK tests, observability read-model tests, and Task 41/52
security/privacy regressions.

## Validation Evidence

Focused RED evidence before the final V33 parent-key fix:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=IdentityAuthPostgresIntegrationTest#migrationAddsPasswordHashAndIdentitySessionTable test
```

Result: failed because `uq_user_account_id_org` was absent.

Focused GREEN evidence after the V33 parent-key fix:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=IdentityAuthPostgresIntegrationTest#migrationAddsPasswordHashAndIdentitySessionTable test
```

Result: 1 test, 0 failures, 0 errors, 0 skipped.

Required Task 51 security regression subset:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=FivePortalBoundaryRegressionTest,PermissionEnforcerTest,AccessControlContractTest,JdbcAccessAuditRecorderPostgresIntegrationTest test
```

Result: 39 tests, 0 failures, 0 errors, 0 skipped.

Task 51 targeted test set:

```bash
rtk mvn -f services/core-api/pom.xml -Dtest=IdentityAuthPostgresIntegrationTest,SupportImpersonationPolicyTest,AdminObservabilityControllerPolicyTest,OwnerRevenueQueryServiceTest,PilotDataPostgresIntegrationTest,ConsultantApiCommandServiceTest,ConsultantApiQueryServiceTest,ConsultantWriteOrgIsolationIntegrationTest test
```

Result: 60 tests, 0 failures, 0 errors, 0 skipped.

Full backend verification:

```bash
rtk git diff --check
rtk docker info
rtk mvn -f services/core-api/pom.xml test
```

Results:

- `rtk git diff --check`: passed.
- `rtk docker info`: Docker client/server reachable.
- `rtk mvn -f services/core-api/pom.xml test`: 1093 tests, 0 failures, 0
  errors, 3 skipped.

## Remaining Non-Task-51 Dependencies

- Task 49 still owns real production integrations and external system
  boundary behavior.
- Task 55 still owns governed real customer import/migration workflows beyond
  the existing pilot seed/import safety checks.
- Task 56 still owns full support tooling and support UI workflows. Task 51
  defines and tests the impersonation policy boundary only.
- Task 57 still owns broad reporting, export packages, and legal audit
  packages beyond the hardened existing Owner accounting handoff and admin
  audit search/export paths.
- Task 60 still owns final full-product acceptance and public production
  release readiness.
