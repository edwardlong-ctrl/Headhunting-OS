# Task 54: Performance, Load, and Cost Targets

Date: 2026-05-10

Branch: `codex/task-54-performance-cost`

## Scope

Task 54 defines target envelopes, deterministic budget policy, a local bounded
load/cost harness, and cost alert rules for pilot-sized and expected
production-sized workloads.

This task does not claim measured production performance. It does not implement
the Task 50 dashboard UI, external APM vendors, browser timing dashboards, or
provider billing integration.

## Target Envelopes

| Surface | Workload | Target p95 | Target p99 | Throughput floor |
| --- | --- | ---: | ---: | ---: |
| Consultant API read paths | pilot | 300 ms | 750 ms | 600 ops/min |
| Consultant API read paths | expected production | 500 ms | 1200 ms | 5000 ops/min |
| Portal interaction loop | pilot | 1200 ms | 2500 ms | 120 interactions/min |
| Portal interaction loop | expected production | 1800 ms | 3500 ms | 1000 interactions/min |
| Batch candidate parsing | pilot | 4000 ms | 12000 ms | 20 docs/min |
| Batch candidate parsing | expected production | 4000 ms | 12000 ms | 1200 docs/min |
| Consultant match generation | pilot | 1500 ms | 3000 ms | 120 reports/min |
| Consultant match generation | expected production | 2500 ms | 5000 ms | 1000 reports/min |

Portal interaction timing is a target envelope for future browser timing
evidence. Task 54 records the envelope and harness model only; it does not add
flaky Playwright timing assertions to the normal frontend suite.

## AI Task Cost Budgets

Cost unit means the provider-neutral `costUnits` already captured on
`AITaskRunRecord`. Until a real billing adapter exists, these are budget units,
not dollars.

| AI task | Workload | Target p95 | Max p95 | Target cost/run | Max cost/run | Monthly run assumption | Monthly budget |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `candidate-profile-parser` | pilot | 45 s | 120 s | 0.030000 | 0.080000 | 1500 | 180.000000 |
| `candidate-profile-parser` | expected production | 45 s | 120 s | 0.030000 | 0.080000 | 30000 | 1200.000000 |
| `authenticity-risk-assessor` | pilot | 30 s | 90 s | 0.020000 | 0.060000 | 1500 | 120.000000 |
| `authenticity-risk-assessor` | expected production | 30 s | 90 s | 0.020000 | 0.060000 | 30000 | 900.000000 |
| `interview-feedback-structurer` | pilot | 30 s | 90 s | 0.020000 | 0.070000 | 600 | 75.000000 |
| `interview-feedback-structurer` | expected production | 30 s | 90 s | 0.020000 | 0.070000 | 12000 | 250.000000 |

## Alert Rules

The backend policy in `PerformanceCostPolicies` classifies deterministic budget
evidence without adding wall-clock tests. AI cost observations can be built from
completed `AITaskRunRecord` rows, so missing or incomplete task-run evidence does
not silently pass:

- `OK`: measured/model evidence is within target.
- `WATCH`: projected monthly AI task cost reaches at least 80 percent of the
  monthly budget.
- `WARNING`: latency or cost per run exceeds target but remains below the hard
  maximum.
- `CRITICAL`: latency exceeds maximum, cost per run exceeds maximum, projected
  monthly cost exceeds budget, or the observation is wired to the wrong task.
- `EVIDENCE_MISSING`: production-sized or completed AI-task-run evidence cannot
  pass by assumption.

Critical cost alerts are actionable: pause replay or batch expansion, review
provider pricing and model routing, then rerun the Task 54 harness.

## Local Harness

Script:

```bash
rtk python3 scripts/performance/task54_performance_load_cost_harness.py
```

The harness is deterministic and bounded. It models the documented workloads
and exits non-zero if any target requires action, except `WATCH`, which remains
successful but must be reviewed before batch expansion.

## Local Harness Evidence

Command:

```bash
rtk python3 scripts/performance/task54_performance_load_cost_harness.py
```

Output recorded on 2026-05-10. This is deterministic capacity-model evidence,
not a live API/browser/provider timing run and not a production performance
claim:

```text
Task 54 deterministic performance/load/cost harness
Evidence source: deterministic capacity model; no live API/browser/provider calls
Mode: bounded local harness; no production performance claim

PASS latency consultant-api-read tier=pilot samples=500 p95_ms=269/300 p99_ms=281/750 throughput_per_min=900/600 reasons=-
PASS latency consultant-api-read tier=expected_production samples=20000 p95_ms=442/500 p99_ms=458/1200 throughput_per_min=6200/5000 reasons=-
PASS latency portal-interaction tier=pilot samples=120 p95_ms=880/1200 p99_ms=896/2500 throughput_per_min=180/120 reasons=-
PASS latency portal-interaction tier=expected_production samples=3000 p95_ms=1325/1800 p99_ms=1352/3500 throughput_per_min=1250/1000 reasons=-
PASS latency batch-candidate-parsing tier=pilot samples=50 p95_ms=2641/4000 p99_ms=2681/12000 throughput_per_min=32/20 reasons=-
PASS latency batch-candidate-parsing tier=expected_production samples=5000 p95_ms=3159/4000 p99_ms=3204/12000 throughput_per_min=1450/1200 reasons=-
PASS latency consultant-matching-generation tier=pilot samples=100 p95_ms=830/1500 p99_ms=851/3000 throughput_per_min=240/120 reasons=-
PASS latency consultant-matching-generation tier=expected_production samples=10000 p95_ms=1602/2500 p99_ms=1637/5000 throughput_per_min=1320/1000 reasons=-

PASS ai-cost candidate-profile-parser tier=pilot p95_ms=18000/45000/120000 cost_per_run=0.018000/0.030000/0.080000 projected_monthly_cost=27.000000/180.000000 projected_monthly_runs=1500 reasons=-
PASS ai-cost candidate-profile-parser tier=expected_production p95_ms=26000/45000/120000 cost_per_run=0.024000/0.030000/0.080000 projected_monthly_cost=720.000000/1200.000000 projected_monthly_runs=30000 reasons=-
PASS ai-cost authenticity-risk-assessor tier=pilot p95_ms=12000/30000/90000 cost_per_run=0.011000/0.020000/0.060000 projected_monthly_cost=16.500000/120.000000 projected_monthly_runs=1500 reasons=-
PASS ai-cost authenticity-risk-assessor tier=expected_production p95_ms=20000/30000/90000 cost_per_run=0.014000/0.020000/0.060000 projected_monthly_cost=420.000000/900.000000 projected_monthly_runs=30000 reasons=-
PASS ai-cost interview-feedback-structurer tier=pilot p95_ms=11000/30000/90000 cost_per_run=0.010000/0.020000/0.070000 projected_monthly_cost=6.000000/75.000000 projected_monthly_runs=600 reasons=-
WATCH ai-cost interview-feedback-structurer tier=expected_production p95_ms=23000/30000/90000 cost_per_run=0.018000/0.020000/0.070000 projected_monthly_cost=216.000000/250.000000 projected_monthly_runs=12000 reasons=ai_task_projected_monthly_cost_near_budget

PASS failures=0
```

## Remaining Gaps

- Real deployed API p95/p99 evidence is still required before claiming
  production performance.
- Portal timing should be collected against a stable browser environment, not
  enforced as flaky Playwright timing assertions.
- Provider billing integration is still required to convert provider-neutral
  `costUnits` into actual money.
- The expected-production rows are bounded assumptions for capacity planning,
  not a substitute for staging load evidence.
