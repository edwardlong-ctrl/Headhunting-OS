package com.recruitingtransactionos.coreapi.governanceconsole;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskDefinition;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskDefinitionRegistry;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskModelRoute;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskModelRouter;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerProperties;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceItemResponse;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceMetricResponse;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceSectionResponse;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigRecord;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
import com.recruitingtransactionos.coreapi.observability.PerformanceCostDashboardPolicy;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public final class GovernanceConsoleReadService {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private static final List<String> TASK50_ADMIN_SECTIONS = List.of(
      "eval-dashboard",
      "negative-cases",
      "review-quality",
      "model-routing",
      "cost-latency",
      "ontology-drift",
      "redaction-incidents",
      "ai-resume-authenticity-risk");

  private final DataSource dataSource;
  private final GovernanceConfigService governanceConfigService;
  private final AITaskDefinitionRegistry aiTaskDefinitionRegistry;
  private final AITaskModelRouter aiTaskModelRouter;
  private final AITaskRunnerProperties aiTaskRunnerProperties;
  private final PerformanceCostDashboardPolicy performanceCostDashboardPolicy;

  public GovernanceConsoleReadService(
      DataSource dataSource,
      GovernanceConfigService governanceConfigService,
      AITaskDefinitionRegistry aiTaskDefinitionRegistry,
      AITaskModelRouter aiTaskModelRouter,
      AITaskRunnerProperties aiTaskRunnerProperties,
      PerformanceCostDashboardPolicy performanceCostDashboardPolicy) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.governanceConfigService = Objects.requireNonNull(
        governanceConfigService,
        "governanceConfigService must not be null");
    this.aiTaskDefinitionRegistry = Objects.requireNonNull(
        aiTaskDefinitionRegistry,
        "aiTaskDefinitionRegistry must not be null");
    this.aiTaskModelRouter = Objects.requireNonNull(aiTaskModelRouter, "aiTaskModelRouter must not be null");
    this.aiTaskRunnerProperties = Objects.requireNonNull(
        aiTaskRunnerProperties,
        "aiTaskRunnerProperties must not be null");
    this.performanceCostDashboardPolicy = Objects.requireNonNull(
        performanceCostDashboardPolicy,
        "performanceCostDashboardPolicy must not be null");
  }

  public static boolean isTask50AdminSection(String sectionKey) {
    return TASK50_ADMIN_SECTIONS.contains(normalize(sectionKey));
  }

  public GovernanceSectionResponse loadAdminSection(UUID organizationId, String sectionKey) {
    String normalized = normalize(sectionKey);
    return switch (normalized) {
      case "eval-dashboard" -> evalDashboard(organizationId);
      case "negative-cases" -> negativeCases(organizationId);
      case "review-quality" -> reviewQuality(organizationId);
      case "model-routing" -> modelRouting(organizationId);
      case "cost-latency" -> costLatency(organizationId);
      case "ontology-drift" -> ontologyDrift(organizationId);
      case "redaction-incidents" -> redactionIncidents(organizationId);
      case "ai-resume-authenticity-risk" -> authenticityRisk(organizationId);
      default -> throw new IllegalArgumentException("unknown_task50_governance_console_section");
    };
  }

  public GovernanceSectionResponse loadOwnerSummary(UUID organizationId) {
    return section(
        "ai-quality",
        "AI Quality",
        "Owner summary of AI task failures, stale ontology warnings, privacy incidents, low-quality review patterns, and authenticity risk.",
        List.of(
            metric("aiTaskFailures", "AI Task Failures", count(organizationId,
                "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ? AND status = 'failed'"),
                "danger", "Failed AI task runs that need admin triage."),
            metric("hallucinationRisks", "Hallucination Risks", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.claim_ledger_item
                WHERE organization_id = ?
                  AND verification_status IN ('system_inference', 'conflicting', 'needs_confirmation')
                """), "warning", "Claims that must not be treated as facts."),
            metric("staleOntologyWarnings", "Stale Ontology Warnings", ontologyReviewQueueCount(organizationId),
                "warning", "Industry packs requiring drift or review-deadline attention."),
            metric("privacyIncidents", "Privacy Incidents", count(organizationId, """
                SELECT COUNT(*)
                FROM privacy.reidentification_risk_assessment
                WHERE organization_id = ?
                  AND decision <> 'allow'
                """), "danger", "Redaction or re-identification assessments that blocked output."),
            metric("lowQualityReviews", "Low-quality Reviews", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.review_event
                WHERE organization_id = ?
                  AND (bulk_flag = true OR status IN ('failed_audit', 'superseded_by_review'))
                """), "warning", "Bulk, failed, or superseded review patterns."),
            metric("highAuthenticityRisk", "High Authenticity Risk", count(organizationId, """
                SELECT COUNT(*)
                FROM recruiting.match_report
                WHERE organization_id = ?
                  AND authenticity_risk = 'HIGH'
                """), "warning", "Match reports capped or flagged for AI-resume authenticity risk.")),
        List.of(
            item("AI task failures", "Admin eval dashboard", metricStatus(organizationId, """
                SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ? AND status = 'failed'
                """), "Open Admin Eval Dashboard for failed run, schema, and hallucination details.", "owner/ai-quality"),
            item("Ontology drift", "Admin ontology drift", metricStatus(organizationId, ontologyReviewQueueSql()),
                "Review stale packs or drift signals before using strong recommendations.", "owner/ai-quality"),
            item("Privacy incidents", "Admin redaction incidents", metricStatus(organizationId, """
                SELECT COUNT(*) FROM privacy.reidentification_risk_assessment WHERE organization_id = ? AND decision <> 'allow'
                """), "Review blocked redaction and re-identification incidents.", "owner/ai-quality")),
        List.of("Owner summary is read-only and intentionally omits raw Candidate/Profile identifiers; Admin has the deeper console."),
        false);
  }

  private GovernanceSectionResponse evalDashboard(UUID organizationId) {
    List<GovernanceItemResponse> items = new ArrayList<>();
    items.addAll(failedAiRunItems(organizationId));
    items.addAll(hallucinationClaimItems(organizationId));
    return section(
        "eval-dashboard",
        "Eval Dashboard",
        "AI task run failures, schema/eval risks, hallucination-prone claims, and registered eval coverage.",
        List.of(
            metric("registeredEvalSuites", "Registered Eval Suites",
                String.valueOf(aiTaskDefinitionRegistry.definitions().size()), "info",
                "Eval suite artifacts registered in the v2.1 AI task registry."),
            metric("failedRuns", "Failed Runs", count(organizationId,
                "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ? AND status = 'failed'"),
                "danger", "Failed AI task run rows."),
            metric("schemaRisks", "Schema Risks", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.ai_task_run
                WHERE organization_id = ?
                  AND status = 'failed'
                  AND (
                    COALESCE(error_code, '') ILIKE '%schema%'
                    OR COALESCE(failure_reason, '') ILIKE '%schema%'
                  )
                """), "danger", "Failures tied to schema validation or schema-shaped error codes."),
            metric("hallucinationRiskClaims", "Hallucination-risk Claims", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.claim_ledger_item
                WHERE organization_id = ?
                  AND verification_status IN ('system_inference', 'conflicting', 'needs_confirmation')
                """), "warning", "Claims that are not verified facts."),
            metric("humanReviewRequired", "Human Review Required", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.ai_task_run
                WHERE organization_id = ?
                  AND human_review_status = 'required'
                """), "warning", "AI runs still requiring human review.")),
        items,
        List.of("A zero failed-run count means no failure events were recorded; it is not a live provider health claim."),
        false);
  }

  private GovernanceSectionResponse negativeCases(UUID organizationId) {
    List<GovernanceItemResponse> items = new ArrayList<>();
    aiTaskDefinitionRegistry.definitions().stream()
        .filter(definition -> definition.registryTaskId().equals("3")
            || definition.registryTaskId().equals("17")
            || definition.registryTaskId().equals("19")
            || definition.registryTaskId().equals("23"))
        .forEach(definition -> items.add(item(
            definition.taskKey() + ".schema.invalid-output",
            "task:" + definition.registryTaskId() + " eval:" + definition.evalSuiteResourcePath(),
            "generated",
            "caseId:task50." + definition.taskKey()
                + ".schema.invalid-output | source:task-registry | expected:schema_validation_failed"
                + " | audit:deterministic-no-llm | inputSchema:" + definition.inputSchemaResourcePath()
                + " | outputSchema:" + definition.outputSchemaResourcePath(),
            "admin/negative-cases")));
    items.addAll(ontologyNegativeCaseItems(organizationId));
    items.addAll(redactionNegativeCaseItems(organizationId));
    return section(
        "negative-cases",
        "Negative Case Generator",
        "Deterministic negative eval fixtures generated from registry, ontology, and redaction risk patterns.",
        List.of(
            metric("generatedCases", "Generated Cases", String.valueOf(items.size()), "info",
                "Auditable deterministic fixtures surfaced for eval regression."),
            metric("registryCases", "Registry-derived", "4", "info",
                "Schema and task-policy cases generated from AI task definitions."),
            metric("ontologyCases", "Ontology-derived", String.valueOf(ontologyNegativeCaseItems(organizationId).size()),
                "warning", "Cases derived from pack negative examples and drift warnings."),
            metric("privacyCases", "Privacy-derived", String.valueOf(redactionNegativeCaseItems(organizationId).size()),
                "danger", "Cases derived from blocked redaction/re-identification assessments.")),
        items,
        List.of("Negative cases are deterministic fixtures generated from registry, ontology, and privacy signals; no live LLM call was made."),
        false);
  }

  private GovernanceSectionResponse reviewQuality(UUID organizationId) {
    return section(
        "review-quality",
        "Review Quality Signals",
        "Low-quality review patterns, bulk-ack distinction, audit failures, and superseded/override-like review rows.",
        List.of(
            metric("totalReviews", "Total Reviews", count(organizationId,
                "SELECT COUNT(*) FROM governance.review_event WHERE organization_id = ?"),
                "info", "All recorded review events."),
            metric("bulkAckNotVerified", "Bulk Ack Not Verified", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.review_event
                WHERE organization_id = ?
                  AND bulk_flag = true
                """), "warning", "Bulk approval remains human_acknowledged only, not candidate/external verified."),
            metric("bulkRatio", "Bulk Ratio", ratio(organizationId, """
                SELECT COUNT(*) FILTER (WHERE bulk_flag = true) AS numerator, COUNT(*) AS denominator
                FROM governance.review_event
                WHERE organization_id = ?
                """), "warning", "Bulk reviews divided by all reviews."),
            metric("failedAudit", "Failed Audit", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.review_event
                WHERE organization_id = ?
                  AND status = 'failed_audit'
                """), "danger", "Review samples that failed audit."),
            metric("supersededReviews", "Superseded Reviews", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.review_event
                WHERE organization_id = ?
                  AND status = 'superseded_by_review'
                """), "warning", "Reviews replaced by later decisions.")),
        reviewQualityItems(organizationId),
        List.of("Review quality is read-only; no console action promotes claims or bypasses canonical write gates."),
        false);
  }

  private GovernanceSectionResponse modelRouting(UUID organizationId) {
    List<GovernanceItemResponse> items = aiTaskDefinitionRegistry.definitions().stream()
        .map(definition -> modelRoutingItem(organizationId, definition))
        .toList();
    long failClosed = items.stream()
        .filter(item -> item.status().startsWith("fail_closed"))
        .count();
    return section(
        "model-routing",
        "Model Routing Console",
        "Configured task routes, provider config status, and fail-closed routing inspection. This does not switch live providers.",
        List.of(
            metric("taskRoutes", "Task Routes", String.valueOf(items.size()), "info",
                "Routes resolved from registry defaults and governed config overlays."),
            metric("routeOverrides", "Route Overrides",
                String.valueOf(governanceConfigService.list(organizationId, "model-routing").size()), "info",
                "Saved model-routing config overlay records."),
            metric("failClosedRoutes", "Fail-closed Routes", String.valueOf(failClosed), "warning",
                "Routes with missing config or explicit fail-closed provider status."),
            metric("providerHealthClaims", "Provider Health Claims", "none", "info",
                "Console only inspects configuration and recorded runs; no live provider governance is claimed.")),
        items,
        List.of("Model routing is inspection-only here; broad live provider switching remains outside Task 50."),
        true,
        configJson(organizationId, "model-routing"),
        configUpdatedAt(organizationId, "model-routing"));
  }

  private GovernanceSectionResponse costLatency(UUID organizationId) {
    List<PerformanceCostDashboardPolicy.DashboardDecision> decisions =
        performanceCostDashboardPolicy.classify(costObservations(organizationId));
    List<GovernanceItemResponse> items = decisions.stream()
        .map(decision -> item(
            decision.taskKey(),
            "projectedMonthlyRuns:" + decision.projectedMonthlyRuns(),
            decision.severity(),
            "sampleSize:" + decision.sampleSize()
                + " | p95Ms:" + (decision.p95Latency() == null ? "not_instrumented" : decision.p95Latency().toMillis())
                + " | avgCostUnits:" + (decision.averageCostUnitsPerRun() == null
                    ? "not_instrumented"
                    : decision.averageCostUnitsPerRun().toPlainString())
                + " | projectedMonthlyCost:" + decision.projectedMonthlyCostUnits().toPlainString()
                + " | reasons:" + String.join(",", decision.reasonCodes())
                + (decision.sampleSize() == 0 ? ",not_instrumented" : "")
                + " | action:" + decision.action(),
            "admin/cost-latency"))
        .toList();
    return section(
        "cost-latency",
        "Cost and Latency Dashboard",
        "Task 54 AI task cost/latency budget evidence from completed AITaskRun rows.",
        List.of(
            metric("budgetedTasks", "Budgeted Tasks", String.valueOf(decisions.size()), "info",
                "Task 54 AI cost budget rows."),
            metric("watchAlerts", "WATCH Alerts", String.valueOf(countSeverity(decisions, "WATCH")), "warning",
                "Near-budget rows that must stay visible before batch expansion."),
            metric("criticalAlerts", "Critical Alerts", String.valueOf(countSeverity(decisions, "CRITICAL")), "danger",
                "Rows that exceed hard budget/latency thresholds."),
            metric("evidenceMissing", "Evidence Missing", String.valueOf(countSeverity(decisions, "EVIDENCE_MISSING")),
                "warning", "Rows with no completed run evidence; not shown as fake zeroes.")),
        items,
        List.of("Cost units are provider-neutral Task 54 units, not provider billing dollars."),
        false);
  }

  private GovernanceSectionResponse ontologyDrift(UUID organizationId) {
    return section(
        "ontology-drift",
        "Ontology Drift Dashboard",
        "Task 47 industry-pack review deadlines, drift signals, negative cases, and stale ontology warnings.",
        List.of(
            metric("packCount", "Industry Packs", count(organizationId,
                "SELECT COUNT(*) FROM recruiting.industry_pack WHERE ?::uuid IS NOT NULL"),
                "info", "Global pack inventory visible to this organization."),
            metric("reviewQueue", "Review Queue", ontologyReviewQueueCount(organizationId), "warning",
                "Packs requiring calibration or drift review."),
            metric("staleReviewDeadlines", "Stale Deadlines", count(organizationId, """
                SELECT COUNT(*)
                FROM recruiting.industry_pack pack
                JOIN recruiting.ontology_version version
                  ON version.industry_pack_id = pack.industry_pack_id
                 AND version.deprecated_at IS NULL
                WHERE ?::uuid IS NOT NULL
                  AND (pack.calibration_review_by <= now() OR version.review_by <= now())
                """), "warning", "Expired pack or ontology review deadlines."),
            metric("driftSignals", "Drift Signals", count(organizationId, """
                SELECT COALESCE(SUM(jsonb_array_length(drift_signals)), 0)
                FROM recruiting.industry_pack
                WHERE ?::uuid IS NOT NULL
                """), "warning", "Configured Task 47 drift signal count.")),
        ontologyDriftItems(organizationId),
        List.of("Ontology drift is surfaced as review evidence; no automatic ontology mutation is performed."),
        false);
  }

  private GovernanceSectionResponse redactionIncidents(UUID organizationId) {
    return section(
        "redaction-incidents",
        "Redaction Incident Dashboard",
        "Re-identification risk, blocked redaction decisions, unsafe feature patterns, and disclosure-safe audit linkage.",
        List.of(
            metric("assessments", "Assessments", count(organizationId,
                "SELECT COUNT(*) FROM privacy.reidentification_risk_assessment WHERE organization_id = ?"),
                "info", "Recorded re-identification risk assessments."),
            metric("incidents", "Incidents", count(organizationId, """
                SELECT COUNT(*)
                FROM privacy.reidentification_risk_assessment
                WHERE organization_id = ?
                  AND decision <> 'allow'
                """), "danger", "Assessments that blocked or constrained output."),
            metric("highRisk", "High/Critical Risk", count(organizationId, """
                SELECT COUNT(*)
                FROM privacy.reidentification_risk_assessment
                WHERE organization_id = ?
                  AND risk_level IN ('high', 'critical')
                """), "danger", "High and critical re-identification risks."),
            metric("workflowLinked", "Workflow Linked", count(organizationId, """
                SELECT COUNT(*)
                FROM privacy.reidentification_risk_assessment
                WHERE organization_id = ?
                  AND workflow_event_id IS NOT NULL
                """), "info", "Assessments linked to WorkflowEvent audit evidence.")),
        redactionIncidentItems(organizationId),
        List.of("Candidate refs and raw profile data are intentionally omitted from this console response."),
        false);
  }

  private GovernanceSectionResponse authenticityRisk(UUID organizationId) {
    return section(
        "ai-resume-authenticity-risk",
        "AI Resume Authenticity Risk",
        "Deterministic authenticity signals from match reports and recorded authenticity-risk-assessor outputs.",
        List.of(
            metric("highRiskReports", "High-risk Reports", count(organizationId, """
                SELECT COUNT(*)
                FROM recruiting.match_report
                WHERE organization_id = ?
                  AND authenticity_risk = 'HIGH'
                """), "warning", "Match reports flagged with high resume authenticity risk."),
            metric("evidenceGaps", "Evidence Gaps", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.ai_task_run run
                JOIN governance.ai_task_definition definition
                  ON definition.ai_task_definition_id = run.ai_task_definition_id
                WHERE run.organization_id = ?
                  AND definition.task_key = 'authenticity-risk-assessor'
                  AND run.output_payload ->> 'independentEvidenceGap' = 'true'
                """), "warning", "Authenticity task outputs requiring independent evidence."),
            metric("capApplied", "Cap Applied", count(organizationId, """
                SELECT COUNT(*)
                FROM recruiting.match_report
                WHERE organization_id = ?
                  AND authenticity_risk = 'HIGH'
                  AND cap_applied = true
                """), "warning", "High-risk reports already capped by matching policy."),
            metric("taskRuns", "Task Runs", count(organizationId, """
                SELECT COUNT(*)
                FROM governance.ai_task_run run
                JOIN governance.ai_task_definition definition
                  ON definition.ai_task_definition_id = run.ai_task_definition_id
                WHERE run.organization_id = ?
                  AND definition.task_key = 'authenticity-risk-assessor'
                """), "info", "Recorded authenticity-risk-assessor runs.")),
        authenticityRiskItems(organizationId),
        List.of("This is deterministic risk surfacing from existing signals, not a claimed ML resume-fraud detector."),
        false);
  }

  private GovernanceItemResponse modelRoutingItem(UUID organizationId, AITaskDefinition definition) {
    try {
      AITaskModelRoute route = aiTaskModelRouter.routeFor(organizationId, definition.taskKey());
      String providerStatus = providerStatus(route);
      return item(
          definition.displayName(),
          definition.taskKey() + " | " + route.providerKey() + "/" + route.modelName(),
          providerStatus,
          "review:" + definition.humanReviewStatus().wireValue()
              + " | writeBack:" + definition.writeBackTarget().wireValue()
              + " | providerStatus:" + providerStatus
              + " | liveHealthCheck:not_performed",
          "admin/model-routing");
    } catch (RuntimeException exception) {
      return item(
          definition.displayName(),
          definition.taskKey(),
          "fail_closed_route_missing",
          "routeError:" + exception.getMessage() + " | liveHealthCheck:not_performed",
          "admin/model-routing");
    }
  }

  private String providerStatus(AITaskModelRoute route) {
    if ("deepseek".equals(route.providerKey()) && isBlank(aiTaskRunnerProperties.getDeepseek().getApiKey())) {
      return "fail_closed_missing_api_key";
    }
    if ("deterministic".equals(route.providerKey())) {
      return "local_deterministic_provider";
    }
    return "configured_no_live_health_claim";
  }

  private List<PerformanceCostDashboardPolicy.DashboardObservation> costObservations(UUID organizationId) {
    String sql = """
        SELECT
          definition.task_key,
          COUNT(run.ai_task_run_id) FILTER (
            WHERE run.status = 'succeeded'
              AND run.completed_at IS NOT NULL
              AND run.cost_units IS NOT NULL
          ) AS sample_size,
          AVG(run.cost_units) FILTER (
            WHERE run.status = 'succeeded'
              AND run.completed_at IS NOT NULL
              AND run.cost_units IS NOT NULL
          ) AS average_cost_units,
          PERCENTILE_CONT(0.95) WITHIN GROUP (
            ORDER BY EXTRACT(EPOCH FROM (run.completed_at - run.started_at)) * 1000
          ) FILTER (
            WHERE run.status = 'succeeded'
              AND run.completed_at IS NOT NULL
              AND run.cost_units IS NOT NULL
          ) AS p95_ms
        FROM governance.ai_task_definition definition
        LEFT JOIN governance.ai_task_run run
          ON run.organization_id = definition.organization_id
         AND run.ai_task_definition_id = definition.ai_task_definition_id
        WHERE definition.organization_id = ?
          AND definition.task_key = ANY(?)
        GROUP BY definition.task_key
        """;
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      Array taskKeys = connection.createArrayOf(
          "text",
          performanceCostDashboardPolicy.budgetedTaskKeys().toArray(String[]::new));
      statement.setArray(2, taskKeys);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<PerformanceCostDashboardPolicy.DashboardObservation> observations = new ArrayList<>();
        while (resultSet.next()) {
          Long p95Ms = resultSet.getObject("p95_ms") == null
              ? null
              : resultSet.getLong("p95_ms");
          observations.add(new PerformanceCostDashboardPolicy.DashboardObservation(
              resultSet.getString("task_key"),
              resultSet.getLong("sample_size"),
              p95Ms == null ? null : Duration.ofMillis(p95Ms),
              resultSet.getBigDecimal("average_cost_units")));
        }
        return List.copyOf(observations);
      } finally {
        taskKeys.free();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("failed_to_load_cost_latency_observations", exception);
    }
  }

  private List<GovernanceItemResponse> failedAiRunItems(UUID organizationId) {
    return limitedItems(organizationId, """
        SELECT definition.task_key, run.status, COALESCE(run.error_code, ''), COALESCE(run.failure_reason, '')
        FROM governance.ai_task_run run
        JOIN governance.ai_task_definition definition
          ON definition.ai_task_definition_id = run.ai_task_definition_id
        WHERE run.organization_id = ?
          AND run.status = 'failed'
        ORDER BY run.created_at DESC
        LIMIT 10
        """, resultSet -> item(
        resultSet.getString(1),
        "AI task failure",
        resultSet.getString(2),
        "error:" + resultSet.getString(3) + " | reason:" + resultSet.getString(4),
        "admin/eval-dashboard"));
  }

  private List<GovernanceItemResponse> hallucinationClaimItems(UUID organizationId) {
    return limitedItems(organizationId, """
        SELECT COALESCE(target_field_path, '<unknown>'), verification_status::text, claim_type::text,
               COALESCE(confidence::text, 'n/a')
        FROM governance.claim_ledger_item
        WHERE organization_id = ?
          AND verification_status IN ('system_inference', 'conflicting', 'needs_confirmation')
        ORDER BY created_at DESC
        LIMIT 10
        """, resultSet -> item(
        resultSet.getString(1),
        "claimType:" + resultSet.getString(3),
        resultSet.getString(2),
        "confidence:" + resultSet.getString(4) + " | factStatus:not_verified",
        "admin/eval-dashboard"));
  }

  private List<GovernanceItemResponse> ontologyNegativeCaseItems(UUID organizationId) {
    return limitedItems(organizationId, """
        SELECT pack.pack_key, negative_case.value, negative_case.ordinality
        FROM recruiting.industry_pack pack
        JOIN LATERAL jsonb_array_elements_text(pack.negative_cases)
          WITH ORDINALITY AS negative_case(value, ordinality) ON true
        WHERE ?::uuid IS NOT NULL
        ORDER BY pack.pack_key, negative_case.ordinality
        LIMIT 6
        """, resultSet -> item(
        "ontology." + resultSet.getString(1) + ".negative." + resultSet.getLong(3),
        "pack:" + resultSet.getString(1),
        "generated",
        "source:ontology-drift | expected:ontology_review_required | pattern:" + resultSet.getString(2),
        "admin/negative-cases"));
  }

  private List<GovernanceItemResponse> redactionNegativeCaseItems(UUID organizationId) {
    return limitedItems(organizationId, """
        SELECT ROW_NUMBER() OVER (
                 ORDER BY recorded_at DESC, reidentification_risk_assessment_ref DESC
               ) AS incident_ordinal,
               risk_level,
               decision,
               unsafe_features
        FROM privacy.reidentification_risk_assessment
        WHERE organization_id = ?
          AND decision <> 'allow'
        ORDER BY recorded_at DESC
        LIMIT 6
        """, resultSet -> item(
        "privacy.reidentification.incident-" + resultSet.getLong("incident_ordinal"),
        "risk:" + resultSet.getString("risk_level"),
        "generated",
        "source:privacy-redaction | expected:redaction_blocked | decision:" + resultSet.getString("decision")
            + " | unsafeFeatures:" + safeArrayText(resultSet.getArray("unsafe_features")),
        "admin/negative-cases"));
  }

  private List<GovernanceItemResponse> reviewQualityItems(UUID organizationId) {
    return limitedItems(organizationId, """
        SELECT field_path, status, review_velocity_bucket, bulk_flag, decision
        FROM governance.review_event
        WHERE organization_id = ?
        ORDER BY created_at DESC
        LIMIT 10
        """, resultSet -> item(
        resultSet.getString(1),
        "velocity:" + resultSet.getString(3) + " | bulk:" + resultSet.getBoolean(4),
        resultSet.getString(2),
        "decision:" + resultSet.getString(5)
            + " | reasonText:omitted"
            + " | bulkAckVerified:false",
        "admin/review-quality"));
  }

  private List<GovernanceItemResponse> ontologyDriftItems(UUID organizationId) {
    return limitedItems(organizationId, """
        SELECT pack.pack_key, pack.display_name, pack.maturity, version.version_key,
               pack.calibration_review_by <= now() OR version.review_by <= now() AS stale,
               jsonb_array_length(pack.drift_signals) AS drift_count,
               jsonb_array_length(pack.negative_cases) AS negative_count
        FROM recruiting.industry_pack pack
        JOIN LATERAL (
          SELECT version_key, review_by
          FROM recruiting.ontology_version
          WHERE industry_pack_id = pack.industry_pack_id
            AND deprecated_at IS NULL
          ORDER BY effective_from DESC
          LIMIT 1
        ) version ON true
        WHERE ?::uuid IS NOT NULL
        ORDER BY pack.pack_key
        LIMIT 20
        """, resultSet -> {
      boolean stale = resultSet.getBoolean("stale");
      int driftCount = resultSet.getInt("drift_count");
      String status = stale || driftCount > 0 || !"production".equals(resultSet.getString("maturity"))
          ? "review_queue"
          : "current";
      return item(
          resultSet.getString("display_name"),
          resultSet.getString("pack_key") + " | ontology:" + resultSet.getString("version_key"),
          status,
          "maturity:" + resultSet.getString("maturity")
              + " | stale:" + stale
              + " | driftSignals:" + driftCount
              + " | negativeCases:" + resultSet.getInt("negative_count"),
          "admin/ontology-drift");
    });
  }

  private List<GovernanceItemResponse> redactionIncidentItems(UUID organizationId) {
    return limitedItems(organizationId, """
        SELECT ROW_NUMBER() OVER (
                 ORDER BY recorded_at DESC, reidentification_risk_assessment_ref DESC
               ) AS incident_ordinal,
               risk_level,
               decision,
               risk_score,
               unsafe_features,
               workflow_event_id IS NOT NULL AS workflow_linked
        FROM privacy.reidentification_risk_assessment
        WHERE organization_id = ?
          AND decision <> 'allow'
        ORDER BY recorded_at DESC
        LIMIT 10
        """, resultSet -> item(
        "redaction incident " + resultSet.getLong("incident_ordinal"),
        "client-safe projection gate",
        resultSet.getString("decision"),
        "riskLevel:" + resultSet.getString("risk_level")
            + " | riskScore:" + resultSet.getDouble("risk_score")
            + " | unsafeFeatures:" + safeArrayText(resultSet.getArray("unsafe_features"))
            + " | workflowLinked:" + resultSet.getBoolean("workflow_linked"),
        "admin/redaction-incidents"));
  }

  private List<GovernanceItemResponse> authenticityRiskItems(UUID organizationId) {
    List<GovernanceItemResponse> items = new ArrayList<>();
    items.addAll(limitedItems(organizationId, """
        SELECT match_report_id::text, authenticity_risk, cap_applied, cap_reason,
               additional_evidence_required, human_review_required
        FROM recruiting.match_report
        WHERE organization_id = ?
          AND authenticity_risk IN ('HIGH', 'MEDIUM')
        ORDER BY generated_at DESC
        LIMIT 10
        """, resultSet -> item(
        resultSet.getString("match_report_id"),
        "match_report",
        resultSet.getString("authenticity_risk"),
        "capApplied:" + resultSet.getBoolean("cap_applied")
            + " | capReason:" + resultSet.getString("cap_reason")
            + " | additionalEvidenceRequired:" + resultSet.getBoolean("additional_evidence_required")
            + " | humanReviewRequired:" + resultSet.getBoolean("human_review_required"),
        "admin/ai-resume-authenticity-risk")));
    items.addAll(limitedItems(organizationId, """
        SELECT definition.task_key, run.output_payload ->> 'authenticityRisk' AS risk,
               run.output_payload ->> 'specificityScore' AS specificity,
               run.output_payload ->> 'independentEvidenceGap' AS evidence_gap
        FROM governance.ai_task_run run
        JOIN governance.ai_task_definition definition
          ON definition.ai_task_definition_id = run.ai_task_definition_id
        WHERE run.organization_id = ?
          AND definition.task_key = 'authenticity-risk-assessor'
        ORDER BY run.created_at DESC
        LIMIT 10
        """, resultSet -> item(
        resultSet.getString("task_key"),
        "recorded task output",
        resultSet.getString("risk"),
        "specificityScore:" + resultSet.getString("specificity")
            + " | independentEvidenceGap:" + resultSet.getString("evidence_gap")
            + " | detectorClaim:deterministic-surfacing-only",
        "admin/ai-resume-authenticity-risk")));
    return List.copyOf(items);
  }

  private String ontologyReviewQueueCount(UUID organizationId) {
    return count(organizationId, ontologyReviewQueueSql());
  }

  private String ontologyReviewQueueSql() {
    return """
        SELECT COUNT(*)
        FROM recruiting.industry_pack pack
        JOIN LATERAL (
          SELECT review_by
          FROM recruiting.ontology_version
          WHERE industry_pack_id = pack.industry_pack_id
            AND deprecated_at IS NULL
          ORDER BY effective_from DESC
          LIMIT 1
        ) version ON true
        WHERE ?::uuid IS NOT NULL
          AND (
            pack.maturity <> 'production'
            OR jsonb_array_length(pack.drift_signals) > 0
            OR pack.calibration_review_by <= now()
            OR version.review_by <= now()
          )
        """;
  }

  private long countSeverity(List<PerformanceCostDashboardPolicy.DashboardDecision> decisions, String severity) {
    return decisions.stream()
        .filter(decision -> decision.severity().equals(severity))
        .count();
  }

  private String metricStatus(UUID organizationId, String sql) {
    long value = Long.parseLong(count(organizationId, sql));
    return value > 0 ? "action_required" : "no_events";
  }

  private String count(UUID organizationId, String sql) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return String.valueOf(resultSet.getLong(1));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("failed_to_count_governance_console_rows", exception);
    }
  }

  private String ratio(UUID organizationId, String sql) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        long numerator = resultSet.getLong("numerator");
        long denominator = resultSet.getLong("denominator");
        if (denominator == 0) {
          return "no review events";
        }
        return String.format(Locale.ROOT, "%.1f%%", (numerator * 100.0d) / denominator);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("failed_to_calculate_governance_console_ratio", exception);
    }
  }

  private List<GovernanceItemResponse> limitedItems(
      UUID organizationId,
      String sql,
      ResultMapper<GovernanceItemResponse> mapper) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<GovernanceItemResponse> items = new ArrayList<>();
        while (resultSet.next()) {
          items.add(mapper.map(resultSet));
        }
        return List.copyOf(items);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("failed_to_load_governance_console_items", exception);
    }
  }

  private GovernanceSectionResponse section(
      String sectionKey,
      String title,
      String description,
      List<GovernanceMetricResponse> metrics,
      List<GovernanceItemResponse> items,
      List<String> warnings,
      boolean editable) {
    return section(sectionKey, title, description, metrics, items, warnings, editable, "{}", "");
  }

  private GovernanceSectionResponse section(
      String sectionKey,
      String title,
      String description,
      List<GovernanceMetricResponse> metrics,
      List<GovernanceItemResponse> items,
      List<String> warnings,
      boolean editable,
      String configJson,
      String updatedAt) {
    return new GovernanceSectionResponse(
        sectionKey,
        title,
        description,
        metrics,
        items,
        warnings,
        editable,
        configJson,
        updatedAt);
  }

  private GovernanceMetricResponse metric(
      String key,
      String label,
      String value,
      String severity,
      String helperText) {
    return new GovernanceMetricResponse(key, label, value, severity, helperText);
  }

  private GovernanceItemResponse item(
      String primary,
      String secondary,
      String status,
      String detail,
      String route) {
    return new GovernanceItemResponse(primary, secondary, status, detail, route);
  }

  private static String safeArrayText(Array array) throws SQLException {
    if (array == null) {
      return "[]";
    }
    Object value = array.getArray();
    if (value instanceof String[] strings) {
      return String.join(",", strings);
    }
    return String.valueOf(value);
  }

  private String configJson(UUID organizationId, String sectionKey) {
    return governanceConfigService.find(organizationId, sectionKey, "default")
        .map(GovernanceConfigRecord::payloadJson)
        .orElse("{}");
  }

  private String configUpdatedAt(UUID organizationId, String sectionKey) {
    Optional<GovernanceConfigRecord> record =
        governanceConfigService.find(organizationId, sectionKey, "default");
    return record
        .map(value -> TIMESTAMP_FORMATTER.format(OffsetDateTime.ofInstant(value.updatedAt(), ZoneOffset.UTC)))
        .orElse("");
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String normalize(String sectionKey) {
    if (sectionKey == null || sectionKey.isBlank()) {
      throw new IllegalArgumentException("sectionKey must not be blank");
    }
    return sectionKey.strip().toLowerCase(Locale.ROOT);
  }

  @FunctionalInterface
  private interface ResultMapper<T> {
    T map(ResultSet resultSet) throws SQLException;
  }
}
