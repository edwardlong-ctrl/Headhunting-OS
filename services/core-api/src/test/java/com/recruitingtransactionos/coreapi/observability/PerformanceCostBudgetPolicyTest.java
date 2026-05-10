package com.recruitingtransactionos.coreapi.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PerformanceCostBudgetPolicyTest {

  private final PerformanceEnvelopePolicy performancePolicy = new PerformanceEnvelopePolicy();
  private final AITaskCostAlertPolicy aiCostAlertPolicy = new AITaskCostAlertPolicy();

  @Test
  void pilotApiAndMatchingEnvelopePassesWithMeasuredLocalWorkload() {
    PerformanceEnvelope apiEnvelope = new PerformanceEnvelope(
        "consultant-api-read",
        WorkloadTier.PILOT,
        Duration.ofMillis(300),
        Duration.ofMillis(750),
        600);
    PerformanceObservation apiObservation = new PerformanceObservation(
        "consultant-api-read",
        WorkloadTier.PILOT,
        500,
        Duration.ofMillis(180),
        Duration.ofMillis(420),
        900);

    PerformanceEnvelopeDecision decision = performancePolicy.evaluate(apiEnvelope, apiObservation);

    assertThat(decision.status()).isEqualTo(PerformanceEnvelopeStatus.MEETS_TARGET);
    assertThat(decision.reasonCodes()).isEmpty();

    PerformanceEnvelope matchingEnvelope = new PerformanceEnvelope(
        "consultant-matching-generation",
        WorkloadTier.PILOT,
        Duration.ofMillis(1_500),
        Duration.ofMillis(3_000),
        120);
    PerformanceObservation matchingObservation = new PerformanceObservation(
        "consultant-matching-generation",
        WorkloadTier.PILOT,
        100,
        Duration.ofMillis(650),
        Duration.ofMillis(1_100),
        240);

    assertThat(performancePolicy.evaluate(matchingEnvelope, matchingObservation).status())
        .isEqualTo(PerformanceEnvelopeStatus.MEETS_TARGET);
  }

  @Test
  void productionEnvelopeFailureIsExplicitAndActionable() {
    PerformanceEnvelope envelope = new PerformanceEnvelope(
        "batch-candidate-parsing",
        WorkloadTier.EXPECTED_PRODUCTION,
        Duration.ofMillis(4_000),
        Duration.ofMillis(12_000),
        1_200);
    PerformanceObservation observation = new PerformanceObservation(
        "batch-candidate-parsing",
        WorkloadTier.EXPECTED_PRODUCTION,
        5_000,
        Duration.ofMillis(5_250),
        Duration.ofMillis(14_500),
        800);

    PerformanceEnvelopeDecision decision = performancePolicy.evaluate(envelope, observation);

    assertThat(decision.status()).isEqualTo(PerformanceEnvelopeStatus.ACTION_REQUIRED);
    assertThat(decision.reasonCodes())
        .containsExactly("p95_latency_over_target", "p99_latency_over_target", "throughput_below_target");
    assertThat(decision.action()).contains("batch-candidate-parsing").contains("expected_production");
  }

  @Test
  void missingProductionEvidenceDoesNotPassByAssumption() {
    PerformanceEnvelope envelope = new PerformanceEnvelope(
        "owner-dashboard-api",
        WorkloadTier.EXPECTED_PRODUCTION,
        Duration.ofMillis(500),
        Duration.ofMillis(1_200),
        2_000);
    PerformanceObservation observation = new PerformanceObservation(
        "owner-dashboard-api",
        WorkloadTier.EXPECTED_PRODUCTION,
        0,
        null,
        null,
        0);

    PerformanceEnvelopeDecision decision = performancePolicy.evaluate(envelope, observation);

    assertThat(decision.status()).isEqualTo(PerformanceEnvelopeStatus.EVIDENCE_MISSING);
    assertThat(decision.reasonCodes()).containsExactly("measurement_sample_missing");
    assertThat(decision.action()).contains("Run the Task 54 harness");
  }

  @Test
  void aiTaskCostAlertClassifiesProjectedMonthlyOverrunAsCritical() {
    AITaskCostBudget budget = new AITaskCostBudget(
        "candidate-profile-parser",
        Duration.ofSeconds(45),
        Duration.ofSeconds(120),
        new BigDecimal("0.030000"),
        new BigDecimal("0.080000"),
        new BigDecimal("600.000000"));
    AITaskCostObservation observation = new AITaskCostObservation(
        "candidate-profile-parser",
        1,
        Duration.ofSeconds(90),
        new BigDecimal("0.090000"),
        10_000);

    AITaskCostAlertDecision decision = aiCostAlertPolicy.classify(budget, observation);

    assertThat(decision.severity()).isEqualTo(CostAlertSeverity.CRITICAL);
    assertThat(decision.projectedMonthlyCostUnits()).isEqualByComparingTo("900.000000");
    assertThat(decision.reasonCodes())
        .containsExactly(
            "ai_task_p95_latency_over_target",
            "ai_task_cost_per_run_over_max",
            "ai_task_projected_monthly_cost_over_budget");
    assertThat(decision.action()).contains("candidate-profile-parser").contains("pause replay/batch expansion");
  }

  @Test
  void aiTaskCostAlertUsesWatchBeforeHardFailure() {
    AITaskCostBudget budget = new AITaskCostBudget(
        "interview-feedback-structurer",
        Duration.ofSeconds(30),
        Duration.ofSeconds(90),
        new BigDecimal("0.020000"),
        new BigDecimal("0.070000"),
        new BigDecimal("250.000000"));
    AITaskCostObservation observation = new AITaskCostObservation(
        "interview-feedback-structurer",
        1,
        Duration.ofSeconds(25),
        new BigDecimal("0.018000"),
        11_500);

    AITaskCostAlertDecision decision = aiCostAlertPolicy.classify(budget, observation);

    assertThat(decision.severity()).isEqualTo(CostAlertSeverity.WATCH);
    assertThat(decision.reasonCodes()).containsExactly("ai_task_projected_monthly_cost_near_budget");
    assertThat(decision.projectedMonthlyCostUnits()).isEqualByComparingTo("207.000000");
  }

  @Test
  void aiTaskCostObservationUsesCompletedTaskRunRecords() {
    AITaskCostObservation observation = AITaskCostObservation.fromCompletedRuns(
        "candidate-profile-parser",
        30_000,
        List.of(
            aiTaskRun("candidate-profile-parser", "00000000-0000-0000-0000-000000540001", 10, "0.010000"),
            aiTaskRun("candidate-profile-parser", "00000000-0000-0000-0000-000000540002", 20, "0.020000"),
            aiTaskRun("candidate-profile-parser", "00000000-0000-0000-0000-000000540003", 40, "0.030000"),
            aiTaskRun("authenticity-risk-assessor", "00000000-0000-0000-0000-000000540004", 90, "0.900000"),
            incompleteAiTaskRun("candidate-profile-parser", "00000000-0000-0000-0000-000000540005")));

    assertThat(observation.sampleSize()).isEqualTo(3);
    assertThat(observation.p95Latency()).isEqualTo(Duration.ofSeconds(40));
    assertThat(observation.averageCostUnitsPerRun()).isEqualByComparingTo("0.020000");
    assertThat(observation.projectedMonthlyRuns()).isEqualTo(30_000);
  }

  @Test
  void aiTaskCostAlertFailsExplicitlyWhenRunEvidenceIsMissing() {
    AITaskCostBudget budget = new AITaskCostBudget(
        "candidate-profile-parser",
        Duration.ofSeconds(45),
        Duration.ofSeconds(120),
        new BigDecimal("0.030000"),
        new BigDecimal("0.080000"),
        new BigDecimal("1200.000000"));
    AITaskCostObservation observation = AITaskCostObservation.fromCompletedRuns(
        "candidate-profile-parser",
        30_000,
        List.of(incompleteAiTaskRun("candidate-profile-parser", "00000000-0000-0000-0000-000000540006")));

    AITaskCostAlertDecision decision = aiCostAlertPolicy.classify(budget, observation);

    assertThat(decision.severity()).isEqualTo(CostAlertSeverity.EVIDENCE_MISSING);
    assertThat(decision.reasonCodes()).containsExactly("ai_task_cost_evidence_missing");
    assertThat(decision.action()).contains("candidate-profile-parser").contains("completed AI task run evidence");
  }

  private static AITaskRunRecord aiTaskRun(
      String taskKey,
      String runId,
      long latencySeconds,
      String costUnits) {
    Instant startedAt = Instant.parse("2026-05-10T00:00:00Z");
    return new AITaskRunRecord(
        new AITaskRunId(UUID.fromString(runId)),
        UUID.fromString("00000000-0000-0000-0000-000000540000"),
        taskKey,
        taskKey + ".v1",
        "/ai/schemas/" + taskKey + "-input.schema.json",
        "/ai/schemas/" + taskKey + "-output.schema.json",
        "prompt." + taskKey + ".v1",
        new ModelRef("deterministic", "pilot", "v1"),
        AITaskRunStatus.SUCCEEDED,
        "required",
        new WriteBackTarget("claim_ledger_proposal"),
        "{}",
        "{}",
        "[]",
        new BigDecimal(costUnits),
        "trace-task54",
        null,
        "{}",
        null,
        null,
        null,
        null,
        new EntityRef("CANDIDATE", UUID.fromString("00000000-0000-0000-0000-000000540099")),
        List.of(),
        startedAt,
        startedAt.plusSeconds(latencySeconds),
        null,
        startedAt);
  }

  private static AITaskRunRecord incompleteAiTaskRun(String taskKey, String runId) {
    Instant startedAt = Instant.parse("2026-05-10T00:00:00Z");
    return new AITaskRunRecord(
        new AITaskRunId(UUID.fromString(runId)),
        UUID.fromString("00000000-0000-0000-0000-000000540000"),
        taskKey,
        taskKey + ".v1",
        "/ai/schemas/" + taskKey + "-input.schema.json",
        "/ai/schemas/" + taskKey + "-output.schema.json",
        "prompt." + taskKey + ".v1",
        new ModelRef("deterministic", "pilot", "v1"),
        AITaskRunStatus.CREATED,
        "required",
        new WriteBackTarget("claim_ledger_proposal"),
        "{}",
        null,
        "[]",
        null,
        "trace-task54",
        null,
        "{}",
        null,
        null,
        null,
        null,
        new EntityRef("CANDIDATE", UUID.fromString("00000000-0000-0000-0000-000000540099")),
        List.of(),
        startedAt,
        null,
        null,
        startedAt);
  }
}
