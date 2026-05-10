package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

enum WorkloadTier {
  PILOT("pilot"),
  EXPECTED_PRODUCTION("expected_production");

  private final String wireValue;

  WorkloadTier(String wireValue) {
    this.wireValue = wireValue;
  }

  String wireValue() {
    return wireValue;
  }
}

record PerformanceEnvelope(
    String surface,
    WorkloadTier workloadTier,
    Duration targetP95Latency,
    Duration targetP99Latency,
    long minimumThroughputPerMinute) {

  PerformanceEnvelope {
    requireNonBlank(surface, "surface");
    Objects.requireNonNull(workloadTier, "workloadTier must not be null");
    requirePositive(targetP95Latency, "targetP95Latency");
    requirePositive(targetP99Latency, "targetP99Latency");
    if (targetP99Latency.compareTo(targetP95Latency) < 0) {
      throw new IllegalArgumentException("targetP99Latency must be greater than or equal to targetP95Latency");
    }
    if (minimumThroughputPerMinute <= 0) {
      throw new IllegalArgumentException("minimumThroughputPerMinute must be positive");
    }
  }

  private static void requireNonBlank(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
  }

  private static void requirePositive(Duration value, String label) {
    Objects.requireNonNull(value, label + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(label + " must be positive");
    }
  }
}

record PerformanceObservation(
    String surface,
    WorkloadTier workloadTier,
    long sampleSize,
    Duration p95Latency,
    Duration p99Latency,
    long throughputPerMinute) {

  PerformanceObservation {
    if (surface == null || surface.isBlank()) {
      throw new IllegalArgumentException("surface must not be blank");
    }
    Objects.requireNonNull(workloadTier, "workloadTier must not be null");
    if (sampleSize < 0) {
      throw new IllegalArgumentException("sampleSize must not be negative");
    }
    if (throughputPerMinute < 0) {
      throw new IllegalArgumentException("throughputPerMinute must not be negative");
    }
  }
}

enum PerformanceEnvelopeStatus {
  MEETS_TARGET,
  ACTION_REQUIRED,
  EVIDENCE_MISSING
}

record PerformanceEnvelopeDecision(
    PerformanceEnvelopeStatus status,
    List<String> reasonCodes,
    String action) {

  PerformanceEnvelopeDecision {
    Objects.requireNonNull(status, "status must not be null");
    reasonCodes = List.copyOf(Objects.requireNonNull(reasonCodes, "reasonCodes must not be null"));
    Objects.requireNonNull(action, "action must not be null");
  }
}

final class PerformanceEnvelopePolicy {

  PerformanceEnvelopeDecision evaluate(PerformanceEnvelope envelope, PerformanceObservation observation) {
    Objects.requireNonNull(envelope, "envelope must not be null");
    Objects.requireNonNull(observation, "observation must not be null");
    if (!envelope.surface().equals(observation.surface()) || envelope.workloadTier() != observation.workloadTier()) {
      return new PerformanceEnvelopeDecision(
          PerformanceEnvelopeStatus.EVIDENCE_MISSING,
          List.of("measurement_target_mismatch"),
          "Run the Task 54 harness for " + envelope.surface() + " at " + envelope.workloadTier().wireValue()
              + " and record matching evidence.");
    }
    if (observation.sampleSize() <= 0 || observation.p95Latency() == null || observation.p99Latency() == null) {
      return new PerformanceEnvelopeDecision(
          PerformanceEnvelopeStatus.EVIDENCE_MISSING,
          List.of("measurement_sample_missing"),
          "Run the Task 54 harness for " + envelope.surface() + " at " + envelope.workloadTier().wireValue()
              + " before claiming this envelope meets target.");
    }

    List<String> reasons = new ArrayList<>();
    if (observation.p95Latency().compareTo(envelope.targetP95Latency()) > 0) {
      reasons.add("p95_latency_over_target");
    }
    if (observation.p99Latency().compareTo(envelope.targetP99Latency()) > 0) {
      reasons.add("p99_latency_over_target");
    }
    if (observation.throughputPerMinute() < envelope.minimumThroughputPerMinute()) {
      reasons.add("throughput_below_target");
    }
    if (reasons.isEmpty()) {
      return new PerformanceEnvelopeDecision(PerformanceEnvelopeStatus.MEETS_TARGET, List.of(), "within_target");
    }
    return new PerformanceEnvelopeDecision(
        PerformanceEnvelopeStatus.ACTION_REQUIRED,
        reasons,
        "Investigate " + envelope.surface() + " " + envelope.workloadTier().wireValue()
            + " bottlenecks, then rerun the Task 54 harness and attach the new output.");
  }
}

record AITaskCostBudget(
    String taskKey,
    Duration targetP95Latency,
    Duration maximumP95Latency,
    BigDecimal targetCostUnitsPerRun,
    BigDecimal maximumCostUnitsPerRun,
    BigDecimal monthlyCostBudgetUnits) {

  AITaskCostBudget {
    if (taskKey == null || taskKey.isBlank()) {
      throw new IllegalArgumentException("taskKey must not be blank");
    }
    requirePositive(targetP95Latency, "targetP95Latency");
    requirePositive(maximumP95Latency, "maximumP95Latency");
    if (maximumP95Latency.compareTo(targetP95Latency) < 0) {
      throw new IllegalArgumentException("maximumP95Latency must be greater than or equal to targetP95Latency");
    }
    targetCostUnitsPerRun = BudgetNumbers.normalizePositive(targetCostUnitsPerRun, "targetCostUnitsPerRun");
    maximumCostUnitsPerRun = BudgetNumbers.normalizePositive(maximumCostUnitsPerRun, "maximumCostUnitsPerRun");
    monthlyCostBudgetUnits = BudgetNumbers.normalizePositive(monthlyCostBudgetUnits, "monthlyCostBudgetUnits");
    if (maximumCostUnitsPerRun.compareTo(targetCostUnitsPerRun) < 0) {
      throw new IllegalArgumentException("maximumCostUnitsPerRun must be greater than or equal to targetCostUnitsPerRun");
    }
  }

  private static void requirePositive(Duration value, String label) {
    Objects.requireNonNull(value, label + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(label + " must be positive");
    }
  }
}

record AITaskCostObservation(
    String taskKey,
    long sampleSize,
    Duration p95Latency,
    BigDecimal averageCostUnitsPerRun,
    long projectedMonthlyRuns) {

  AITaskCostObservation {
    if (taskKey == null || taskKey.isBlank()) {
      throw new IllegalArgumentException("taskKey must not be blank");
    }
    if (sampleSize < 0) {
      throw new IllegalArgumentException("sampleSize must not be negative");
    }
    if (p95Latency != null && p95Latency.isNegative()) {
      throw new IllegalArgumentException("p95Latency must not be negative");
    }
    averageCostUnitsPerRun = averageCostUnitsPerRun == null
        ? null
        : BudgetNumbers.normalizeNonNegative(averageCostUnitsPerRun, "averageCostUnitsPerRun");
    if (projectedMonthlyRuns < 0) {
      throw new IllegalArgumentException("projectedMonthlyRuns must not be negative");
    }
  }

  static AITaskCostObservation fromCompletedRuns(
      String taskKey,
      long projectedMonthlyRuns,
      List<AITaskRunRecord> records) {
    if (taskKey == null || taskKey.isBlank()) {
      throw new IllegalArgumentException("taskKey must not be blank");
    }
    Objects.requireNonNull(records, "records must not be null");
    if (projectedMonthlyRuns < 0) {
      throw new IllegalArgumentException("projectedMonthlyRuns must not be negative");
    }
    List<AITaskRunRecord> completedRuns = records.stream()
        .filter(record -> taskKey.equals(record.taskName()))
        .filter(record -> record.status() == AITaskRunStatus.SUCCEEDED)
        .filter(record -> record.completedAt() != null)
        .filter(record -> record.costUnits() != null)
        .toList();
    if (completedRuns.isEmpty()) {
      return new AITaskCostObservation(taskKey, 0, null, null, projectedMonthlyRuns);
    }

    List<Duration> latencies = completedRuns.stream()
        .map(record -> Duration.between(record.startedAt(), record.completedAt()))
        .sorted(Comparator.naturalOrder())
        .toList();
    Duration p95Latency = latencies.get(nearestRankIndex(latencies.size(), 0.95d));
    BigDecimal totalCost = completedRuns.stream()
        .map(AITaskRunRecord::costUnits)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal averageCost = totalCost.divide(
        BigDecimal.valueOf(completedRuns.size()),
        6,
        RoundingMode.HALF_UP);
    return new AITaskCostObservation(
        taskKey,
        completedRuns.size(),
        p95Latency,
        averageCost,
        projectedMonthlyRuns);
  }

  private static int nearestRankIndex(int sampleSize, double percentile) {
    return Math.max(0, (int) Math.ceil(sampleSize * percentile) - 1);
  }
}

enum CostAlertSeverity {
  OK,
  WATCH,
  WARNING,
  CRITICAL,
  EVIDENCE_MISSING
}

record AITaskCostAlertDecision(
    CostAlertSeverity severity,
    List<String> reasonCodes,
    BigDecimal projectedMonthlyCostUnits,
    String action) {

  AITaskCostAlertDecision {
    Objects.requireNonNull(severity, "severity must not be null");
    reasonCodes = List.copyOf(Objects.requireNonNull(reasonCodes, "reasonCodes must not be null"));
    projectedMonthlyCostUnits = BudgetNumbers.normalizeNonNegative(
        projectedMonthlyCostUnits,
        "projectedMonthlyCostUnits");
    Objects.requireNonNull(action, "action must not be null");
  }
}

final class AITaskCostAlertPolicy {

  private static final BigDecimal NEAR_BUDGET_RATIO = new BigDecimal("0.80");

  AITaskCostAlertDecision classify(AITaskCostBudget budget, AITaskCostObservation observation) {
    Objects.requireNonNull(budget, "budget must not be null");
    Objects.requireNonNull(observation, "observation must not be null");
    if (!budget.taskKey().equals(observation.taskKey())) {
      return new AITaskCostAlertDecision(
          CostAlertSeverity.CRITICAL,
          List.of("ai_task_budget_target_mismatch"),
          BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
          "Check cost alert wiring because the observed task does not match the configured budget.");
    }
    if (observation.sampleSize() <= 0
        || observation.p95Latency() == null
        || observation.averageCostUnitsPerRun() == null) {
      return new AITaskCostAlertDecision(
          CostAlertSeverity.EVIDENCE_MISSING,
          List.of("ai_task_cost_evidence_missing"),
          BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
          "Run completed AI task run evidence for " + budget.taskKey()
              + " before claiming its latency/cost budget is within target.");
    }

    BigDecimal projectedMonthlyCost = observation.averageCostUnitsPerRun()
        .multiply(BigDecimal.valueOf(observation.projectedMonthlyRuns()))
        .setScale(6, RoundingMode.HALF_UP);
    List<String> reasons = new ArrayList<>();
    CostAlertSeverity severity = CostAlertSeverity.OK;

    if (observation.p95Latency().compareTo(budget.maximumP95Latency()) > 0) {
      reasons.add("ai_task_p95_latency_over_max");
      severity = max(severity, CostAlertSeverity.CRITICAL);
    } else if (observation.p95Latency().compareTo(budget.targetP95Latency()) > 0) {
      reasons.add("ai_task_p95_latency_over_target");
      severity = max(severity, CostAlertSeverity.WARNING);
    }

    if (observation.averageCostUnitsPerRun().compareTo(budget.maximumCostUnitsPerRun()) > 0) {
      reasons.add("ai_task_cost_per_run_over_max");
      severity = max(severity, CostAlertSeverity.CRITICAL);
    } else if (observation.averageCostUnitsPerRun().compareTo(budget.targetCostUnitsPerRun()) > 0) {
      reasons.add("ai_task_cost_per_run_over_target");
      severity = max(severity, CostAlertSeverity.WARNING);
    }

    if (projectedMonthlyCost.compareTo(budget.monthlyCostBudgetUnits()) > 0) {
      reasons.add("ai_task_projected_monthly_cost_over_budget");
      severity = max(severity, CostAlertSeverity.CRITICAL);
    } else if (projectedMonthlyCost.compareTo(budget.monthlyCostBudgetUnits().multiply(NEAR_BUDGET_RATIO)) >= 0) {
      reasons.add("ai_task_projected_monthly_cost_near_budget");
      severity = max(severity, CostAlertSeverity.WATCH);
    }

    return new AITaskCostAlertDecision(severity, reasons, projectedMonthlyCost, actionFor(severity, budget.taskKey()));
  }

  private static CostAlertSeverity max(CostAlertSeverity left, CostAlertSeverity right) {
    return left.ordinal() >= right.ordinal() ? left : right;
  }

  private static String actionFor(CostAlertSeverity severity, String taskKey) {
    return switch (severity) {
      case OK -> "within_budget";
      case WATCH -> "Monitor " + taskKey + " volume before enabling larger batches.";
      case WARNING -> "Review " + taskKey + " prompt/model route and rerun the Task 54 cost harness.";
      case CRITICAL -> "For " + taskKey
          + ", pause replay/batch expansion, review provider pricing/model route, and rerun the Task 54 cost harness.";
      case EVIDENCE_MISSING -> "Run completed AI task run evidence for " + taskKey
          + " before claiming its latency/cost budget is within target.";
    };
  }
}

final class BudgetNumbers {

  private BudgetNumbers() {
  }

  static BigDecimal normalizePositive(BigDecimal value, String label) {
    BigDecimal normalized = normalizeNonNegative(value, label);
    if (normalized.signum() <= 0) {
      throw new IllegalArgumentException(label + " must be positive");
    }
    return normalized;
  }

  static BigDecimal normalizeNonNegative(BigDecimal value, String label) {
    Objects.requireNonNull(value, label + " must not be null");
    if (value.signum() < 0) {
      throw new IllegalArgumentException(label + " must not be negative");
    }
    return value.setScale(6, RoundingMode.HALF_UP);
  }
}
