#!/usr/bin/env python3
"""Deterministic Task 54 performance/load/cost envelope harness.

This is intentionally bounded and deterministic. It validates target envelopes
and capacity assumptions without adding wall-clock timing assertions to Maven.
Live API/browser timings should be attached separately when a stable deployed
environment exists.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP
from typing import Iterable


@dataclass(frozen=True)
class LatencyThroughputEnvelope:
    name: str
    tier: str
    samples: int
    p95_target_ms: int
    p99_target_ms: int
    min_throughput_per_minute: int
    model_base_ms: int
    model_spread_ms: int
    model_throughput_per_minute: int


@dataclass(frozen=True)
class AITaskBudget:
    task_key: str
    tier: str
    target_p95_ms: int
    max_p95_ms: int
    target_cost_units_per_run: Decimal
    max_cost_units_per_run: Decimal
    projected_monthly_runs: int
    monthly_cost_budget_units: Decimal
    model_p95_ms: int
    model_cost_units_per_run: Decimal


def deterministic_samples(base_ms: int, spread_ms: int, count: int) -> list[int]:
    return [
        base_ms + ((index * 37) % spread_ms) + ((index % 11) * 3)
        for index in range(count)
    ]


def percentile(values: Iterable[int], percentile_value: float) -> int:
    ordered = sorted(values)
    if not ordered:
        raise ValueError("cannot compute percentile for empty values")
    index = round((len(ordered) - 1) * percentile_value)
    return ordered[index]


def money(value: Decimal) -> Decimal:
    return value.quantize(Decimal("0.000001"), rounding=ROUND_HALF_UP)


def evaluate_latency(envelope: LatencyThroughputEnvelope) -> tuple[str, list[str], int, int]:
    samples = deterministic_samples(envelope.model_base_ms, envelope.model_spread_ms, envelope.samples)
    p95 = percentile(samples, 0.95)
    p99 = percentile(samples, 0.99)
    reasons: list[str] = []
    if p95 > envelope.p95_target_ms:
        reasons.append("p95_latency_over_target")
    if p99 > envelope.p99_target_ms:
        reasons.append("p99_latency_over_target")
    if envelope.model_throughput_per_minute < envelope.min_throughput_per_minute:
        reasons.append("throughput_below_target")
    return ("PASS" if not reasons else "ACTION_REQUIRED", reasons, p95, p99)


def evaluate_ai_cost(budget: AITaskBudget) -> tuple[str, list[str], Decimal]:
    projected_monthly_cost = money(budget.model_cost_units_per_run * budget.projected_monthly_runs)
    reasons: list[str] = []
    status = "PASS"
    if budget.model_p95_ms > budget.max_p95_ms:
        reasons.append("ai_task_p95_latency_over_max")
        status = "CRITICAL"
    elif budget.model_p95_ms > budget.target_p95_ms:
        reasons.append("ai_task_p95_latency_over_target")
        status = "WARNING"
    if budget.model_cost_units_per_run > budget.max_cost_units_per_run:
        reasons.append("ai_task_cost_per_run_over_max")
        status = "CRITICAL"
    elif budget.model_cost_units_per_run > budget.target_cost_units_per_run:
        reasons.append("ai_task_cost_per_run_over_target")
        if status == "PASS":
            status = "WARNING"
    if projected_monthly_cost > budget.monthly_cost_budget_units:
        reasons.append("ai_task_projected_monthly_cost_over_budget")
        status = "CRITICAL"
    elif projected_monthly_cost >= money(budget.monthly_cost_budget_units * Decimal("0.80")):
        reasons.append("ai_task_projected_monthly_cost_near_budget")
        if status == "PASS":
            status = "WATCH"
    return status, reasons, projected_monthly_cost


LATENCY_ENVELOPES = [
    LatencyThroughputEnvelope("consultant-api-read", "pilot", 500, 300, 750, 600, 120, 140, 900),
    LatencyThroughputEnvelope("consultant-api-read", "expected_production", 20_000, 500, 1_200, 5_000, 180, 260, 6_200),
    LatencyThroughputEnvelope("portal-interaction", "pilot", 120, 1_200, 2_500, 120, 520, 360, 180),
    LatencyThroughputEnvelope("portal-interaction", "expected_production", 3_000, 1_800, 3_500, 1_000, 760, 580, 1_250),
    LatencyThroughputEnvelope("batch-candidate-parsing", "pilot", 50, 4_000, 12_000, 20, 1_900, 760, 32),
    LatencyThroughputEnvelope("batch-candidate-parsing", "expected_production", 5_000, 4_000, 12_000, 1_200, 2_100, 1_100, 1_450),
    LatencyThroughputEnvelope("consultant-matching-generation", "pilot", 100, 1_500, 3_000, 120, 430, 420, 240),
    LatencyThroughputEnvelope("consultant-matching-generation", "expected_production", 10_000, 2_500, 5_000, 1_000, 780, 850, 1_320),
]

AI_BUDGETS = [
    AITaskBudget("candidate-profile-parser", "pilot", 45_000, 120_000, Decimal("0.030000"), Decimal("0.080000"), 1_500, Decimal("180.000000"), 18_000, Decimal("0.018000")),
    AITaskBudget("candidate-profile-parser", "expected_production", 45_000, 120_000, Decimal("0.030000"), Decimal("0.080000"), 30_000, Decimal("1200.000000"), 26_000, Decimal("0.024000")),
    AITaskBudget("authenticity-risk-assessor", "pilot", 30_000, 90_000, Decimal("0.020000"), Decimal("0.060000"), 1_500, Decimal("120.000000"), 12_000, Decimal("0.011000")),
    AITaskBudget("authenticity-risk-assessor", "expected_production", 30_000, 90_000, Decimal("0.020000"), Decimal("0.060000"), 30_000, Decimal("900.000000"), 20_000, Decimal("0.014000")),
    AITaskBudget("interview-feedback-structurer", "pilot", 30_000, 90_000, Decimal("0.020000"), Decimal("0.070000"), 600, Decimal("75.000000"), 11_000, Decimal("0.010000")),
    AITaskBudget("interview-feedback-structurer", "expected_production", 30_000, 90_000, Decimal("0.020000"), Decimal("0.070000"), 12_000, Decimal("250.000000"), 23_000, Decimal("0.018000")),
]


def main() -> int:
    print("Task 54 deterministic performance/load/cost harness")
    print("Mode: local deterministic model; no production performance claim")
    print("")
    failures = 0
    for envelope in LATENCY_ENVELOPES:
        status, reasons, p95, p99 = evaluate_latency(envelope)
        failures += 0 if status == "PASS" else 1
        print(
            f"{status} latency {envelope.name} tier={envelope.tier} "
            f"samples={envelope.samples} p95_ms={p95}/{envelope.p95_target_ms} "
            f"p99_ms={p99}/{envelope.p99_target_ms} "
            f"throughput_per_min={envelope.model_throughput_per_minute}/{envelope.min_throughput_per_minute} "
            f"reasons={','.join(reasons) if reasons else '-'}"
        )
    print("")
    for budget in AI_BUDGETS:
        status, reasons, projected_monthly_cost = evaluate_ai_cost(budget)
        failures += 0 if status in {"PASS", "WATCH"} else 1
        print(
            f"{status} ai-cost {budget.task_key} tier={budget.tier} "
            f"p95_ms={budget.model_p95_ms}/{budget.target_p95_ms}/{budget.max_p95_ms} "
            f"cost_per_run={budget.model_cost_units_per_run}/{budget.target_cost_units_per_run}/{budget.max_cost_units_per_run} "
            f"projected_monthly_cost={projected_monthly_cost}/{budget.monthly_cost_budget_units} "
            f"projected_monthly_runs={budget.projected_monthly_runs} "
            f"reasons={','.join(reasons) if reasons else '-'}"
        )
    print("")
    if failures:
        print(f"ACTION_REQUIRED failures={failures}")
        return 1
    print("PASS failures=0")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
