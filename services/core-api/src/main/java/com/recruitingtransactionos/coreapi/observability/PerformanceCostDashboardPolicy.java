package com.recruitingtransactionos.coreapi.observability;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class PerformanceCostDashboardPolicy {

  private static final List<AITaskCostBudget> BUDGETS = List.of(
      new AITaskCostBudget(
          "candidate-profile-parser",
          Duration.ofSeconds(45),
          Duration.ofSeconds(120),
          new BigDecimal("0.030000"),
          new BigDecimal("0.080000"),
          new BigDecimal("1200.000000")),
      new AITaskCostBudget(
          "authenticity-risk-assessor",
          Duration.ofSeconds(30),
          Duration.ofSeconds(90),
          new BigDecimal("0.020000"),
          new BigDecimal("0.060000"),
          new BigDecimal("900.000000")),
      new AITaskCostBudget(
          "interview-feedback-structurer",
          Duration.ofSeconds(30),
          Duration.ofSeconds(90),
          new BigDecimal("0.020000"),
          new BigDecimal("0.070000"),
          new BigDecimal("250.000000")));

  private static final long EXPECTED_PRODUCTION_CANDIDATE_PROFILE_RUNS = 30_000;
  private static final long EXPECTED_PRODUCTION_AUTHENTICITY_RUNS = 30_000;
  private static final long EXPECTED_PRODUCTION_INTERVIEW_FEEDBACK_RUNS = 12_000;

  private final AITaskCostAlertPolicy policy = new AITaskCostAlertPolicy();

  public List<String> budgetedTaskKeys() {
    return BUDGETS.stream()
        .map(AITaskCostBudget::taskKey)
        .toList();
  }

  public List<DashboardDecision> classify(List<DashboardObservation> observations) {
    return BUDGETS.stream()
        .map(budget -> classifyBudget(budget, observationFor(budget.taskKey(), observations)))
        .toList();
  }

  private DashboardDecision classifyBudget(AITaskCostBudget budget, DashboardObservation observation) {
    AITaskCostAlertDecision decision = policy.classify(budget, new AITaskCostObservation(
        budget.taskKey(),
        observation.sampleSize(),
        observation.p95Latency(),
        observation.averageCostUnitsPerRun(),
        projectedMonthlyRuns(budget.taskKey())));
    return new DashboardDecision(
        budget.taskKey(),
        decision.severity().name(),
        decision.reasonCodes(),
        decision.projectedMonthlyCostUnits(),
        decision.action(),
        observation.sampleSize(),
        observation.p95Latency(),
        observation.averageCostUnitsPerRun(),
        projectedMonthlyRuns(budget.taskKey()));
  }

  private static DashboardObservation observationFor(
      String taskKey,
      List<DashboardObservation> observations) {
    return observations.stream()
        .filter(observation -> observation.taskKey().equals(taskKey))
        .findFirst()
        .orElse(new DashboardObservation(taskKey, 0, null, null));
  }

  private static long projectedMonthlyRuns(String taskKey) {
    return switch (taskKey) {
      case "candidate-profile-parser" -> EXPECTED_PRODUCTION_CANDIDATE_PROFILE_RUNS;
      case "authenticity-risk-assessor" -> EXPECTED_PRODUCTION_AUTHENTICITY_RUNS;
      case "interview-feedback-structurer" -> EXPECTED_PRODUCTION_INTERVIEW_FEEDBACK_RUNS;
      default -> throw new IllegalArgumentException("unknown_task54_cost_budget");
    };
  }

  public record DashboardObservation(
      String taskKey,
      long sampleSize,
      Duration p95Latency,
      BigDecimal averageCostUnitsPerRun) {
  }

  public record DashboardDecision(
      String taskKey,
      String severity,
      List<String> reasonCodes,
      BigDecimal projectedMonthlyCostUnits,
      String action,
      long sampleSize,
      Duration p95Latency,
      BigDecimal averageCostUnitsPerRun,
      long projectedMonthlyRuns) {
  }
}
