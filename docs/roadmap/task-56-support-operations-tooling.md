# Task 56 Support and Operations Tooling

## Scope

Task 56 adds backend support operations tooling under
`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/supportops`.
It is a support service layer, not a support database backdoor.

Delivered capabilities:

- Ticketed same-organization user support lookup.
- Ticketed failed-notification retry through `NotificationService`.
- Ticketed AI task replay through `AITaskReplayService`.
- Ticketed data-correction request workflow through `ReviewEventService` and
  `WorkflowEventService`.
- Support action audit records in `audit.audit_log`.

## Safety Rules

- Support actors must be Admin users.
- Support actions require the support actor organization to match the target
  organization.
- Ticket reference and human-readable reason are required for every action.
- Permission-denied results return the generic `support_action_denied` code and
  do not expose target existence.
- Cross-organization denied attempts are audited under the support actor's
  organization, preserving Task 51 tenant FKs and avoiding target-org leakage.
- Data correction requests create review/workflow evidence and do not mutate
  canonical facts.
- AI replay is treated as audit-only support replay; if an adapter ever reports
  canonical fact persistence, the support action is blocked and audited.

## Implementation Notes

- `SupportOperationsService` is the application boundary.
- `SupportOperationsPermissionPolicy` is fail-closed and does not use role
  alone to bypass organization, ticket, or reason checks.
- `JdbcSupportUserLookupPort` reads only `identity.user_account` and active
  `identity.role_assignment` rows scoped by organization.
- `JdbcSupportActionAuditPort` writes support action evidence to
  `audit.audit_log` with `support.*` action names.
- `NotificationService.retryFailedNotification(...)` enforces organization
  scope, requires an existing failed delivery attempt, and uses
  `support_retry:<notificationId>:<ticketRef>` as the duplicate-safe retry
  source reference with a database-level partial unique index for support retry
  refs.
- `NotificationServiceFailedNotificationRetryPort` is the supportops adapter
  into the notification boundary.
- `AITaskReplaySupportAdapter` is the supportops adapter into the existing AI
  replay boundary.
- `SpringSupportOperationsTransactionBoundary` keeps support side effects and
  support audit evidence in one rollback boundary for Spring-managed runtime.

## Verification Added

- `SupportOperationsServiceTest`
  - Cross-organization lookup cannot infer or access another org's user.
  - Retry/replay commands require ticket/reason and write support audit.
  - Failed notification retry command remains duplicate-safe and org-scoped.
  - AI replay failures are audited and return safe support result codes.
  - AI replay is blocked if canonical fact persistence is reported.
  - Data correction creates review/workflow items and preserves facts.
  - Permission-denied responses do not leak record existence.
- `SupportOperationsPostgresIntegrationTest`
  - Real PostgreSQL user lookup is organization-scoped and audited.
  - Real failed-notification retry reuses `NotificationService` and is
    duplicate-safe.
  - Support retry source refs are database-idempotent without changing ordinary
    notification source-ref semantics.
  - Real data-correction request persists review event, workflow event, and
    support audit evidence without fact mutation.
  - Audit failure rolls back support-created review/workflow artifacts rather
    than leaving unaudited support changes behind.
