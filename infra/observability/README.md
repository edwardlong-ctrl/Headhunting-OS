# Task 40 Observability, Audit, and Replay v1

This runbook covers the provider-neutral Task 40 v1 observability baseline. It
uses local structured logs and PostgreSQL-backed audit records only. It does not
add Prometheus, OpenTelemetry, external log collectors, or vendor dashboards.

## Baseline

- All `/api/**` requests pass through `RequestCorrelationFilter`.
- A valid `X-Request-Id` header is preserved when it matches the safe request-id
  pattern. Missing, unsafe, or overlong values are replaced with a generated
  `req-...` id.
- The request id is written to the response header, SLF4J MDC (`requestId`), and
  the safe request log line.
- Staging and production profiles use key-value structured log patterns with
  timestamp, level, service, request id, organization id, actor role, error
  code, logger, and sanitized message text.
- The request log records method, route, status, duration, organization id,
  actor role, and error code. It must not log request bodies, query strings,
  authorization headers, secrets, source documents, uploaded file contents, raw
  Candidate data, raw CandidateProfile data, or raw AI payloads.

## Admin Observability APIs

The admin observability surface is read-only and uses `ApiResponseEnvelope` plus
`ApiSafeResponseBody` response DTOs.

- `GET /api/admin/observability/workflow-events`
  - Filters: `workflowEventId`, `entityType`, `entityId`, `actionCode`,
    `actorType`, `actorId`, `correlationId`, `causationId`, `occurredFrom`,
    `occurredTo`, `limit`, `offset`.
- `GET /api/admin/observability/review-events`
  - Filters: `targetEntityType`, `targetEntityId`, `status`,
    `claimLedgerItemId`, `reviewerUserId`, `createdFrom`, `createdTo`, `limit`,
    `offset`.
- `GET /api/admin/observability/ai-task-runs`
  - Filters: `taskName`, `status`, `targetEntityType`, `targetEntityId`,
    `correlationId`, `causationId`, `startedFrom`, `startedTo`, `limit`,
    `offset`.
  - Rows include task/model/prompt/schema versions, status, human-review status,
    write-back target, cost, latency, trace ref, replay lineage, requester role,
    target ref, correlation, causation, and timestamps. Raw input/output/tool
    payloads are not read or returned by this surface.
- `GET /api/admin/observability/disclosures/{disclosureRecordRef}/audit-export`
  - Returns safe disclosure metadata plus related WorkflowEvents, AITaskRuns,
    and ReviewEvents when explicit links exist.

The APIs are admin/system only. Non-admin roles must receive a sanitized `403`.
Invalid ranges and invalid bounded parameters fail closed with a sanitized API
error.

## Disclosure Audit Export

The disclosure export intentionally reports only safe audit fields:

- disclosure ref, status, level, redaction level
- candidate/profile/job/client safe refs
- consent record ref, consent status, consent text version, and profile version
- unlock decision ref and approver role/user id when the linked decision exists
- requester fields remain null unless a future safe requester link is modeled;
  current exports return `missing_requester_link` rather than inferring it
- workflow event id when persisted on the disclosure record
- decided timestamp
- related WorkflowEvent, AITaskRun, and ReviewEvent safe rows
- `missing_*` reason codes for absent records or absent explicit links

If linkage is not present in PostgreSQL, the export returns empty arrays plus
reason codes. Do not backfill or infer relationships from raw candidate/source
payloads during an incident.

## Incident Runbook

1. Capture the `X-Request-Id` from the failing API response or client report.
2. Search application logs for `requestId=<value>`.
3. Use the route/status/duration/organization/actor/error code from the
   structured log to identify the impacted API boundary.
4. For workflow-state questions, call the admin WorkflowEvent search with
   `correlationId`, `causationId`, entity filters, or time range.
5. For human-review questions, call the ReviewEvent search by claim, target,
   reviewer, status, or time range.
6. For AI execution questions, call the AITaskRun search by task, status,
   target, correlation, causation, or time range. Use replay lineage fields to
   find the predecessor run before attempting a replay investigation.
7. For disclosure questions, call the disclosure audit export by
   `disclosureRecordRef`. Treat `missing_*` codes as evidence of absent linkage,
   not as permission to infer facts.
8. Preserve the request id, response error code, relevant safe audit refs, and
   timestamps in the incident notes.

## Forbidden Log Handling

If logs contain candidate PII, raw profile text, source document text, uploaded
file contents, bearer tokens, API keys, provider secrets, stack traces, or raw AI
payloads:

1. Stop copying or forwarding the unsafe log excerpt.
2. Record the request id, timestamp, environment, logger name, and safe summary
   of the issue.
3. Rotate exposed credentials if any secret or token appeared.
4. Remove the unsafe log file from local sharing paths and restrict access to
   the minimum operators needed for remediation.
5. Patch the offending logger or exception handler so future output uses safe
   reason codes and refs only.
6. Add a regression test proving the unsafe string does not appear in the
   relevant log or API response.

## Verification

Task 40 v1 verification should include:

```sh
rtk docker info
rtk mvn -f services/core-api/pom.xml -Dtest=*Observability*,*Audit*,*Workflow*,*AITask*,*Disclosure* test
rtk mvn -f services/core-api/pom.xml test
rtk npm --workspace @rto/web run build
rtk git diff --check
```

The npm build is required only when frontend files or frontend dependencies
change. Task 40 v1 currently adds no frontend observability dashboard.
