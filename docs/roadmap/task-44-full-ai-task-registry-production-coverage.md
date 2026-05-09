# Task 44: Full AI Task Registry Production Coverage

## Scope

Task 44 closes the v2.1 AI Task Registry coverage gate for production
inspection. It does not claim that every task has a full executable business
orchestration service.

## Delivered

- Registered all 28 v2.1 production AI tasks:
  - base registry Tasks 0.1-0.5 and 1-13.
  - v2.1 governance registry Tasks 14-23.
- Added registry metadata for every task:
  - registry task id and display name.
  - task version.
  - prompt version and prompt resource.
  - input/output schema resources.
  - eval suite resource.
  - human-review policy.
  - write-back target policy.
- Added missing prompt/schema/eval resources for registry-only tasks while
  preserving the stricter existing executable schemas for current runner-backed
  tasks.
- Added a default production model route so non-executable registry tasks still
  have an inspectable governed route instead of failing route lookup.
- Changed the Admin AI Task Registry read model to list definition-first task
  coverage, not only historical AITaskRun rows.
- Admin `/admin/ai-task-registry` now exposes each production task's version,
  schema, model route, eval result registration, review policy, write-back
  target, aggregate cost, aggregate latency, failure count, and replay history
  count.

## Deliberate Non-Claims

- No worker queue or async orchestration was added.
- No broad write-back execution was added.
- Registry-only tasks do not become canonical fact writers.
- AI outputs remain claims, drafts, recommendations, or risk assessments until
  existing review and canonical-write gates allow a later product path.

## Regression Coverage

- `AITaskRunnerConfigurationTest` proves all v2.1 registry tasks have
  classpath-loadable prompt/schema/eval artifacts.
- `AITaskModelRouterTest` proves the default governed production model route
  applies when a task-specific route is absent.
- `GovernanceReadServiceTest` proves Admin AI Task Registry lists all
  production definitions even when no run history exists.
- `GovernanceReadServicePostgresIntegrationTest` proves the Admin registry
  read path loads against the migrated PostgreSQL schema.
