package com.recruitingtransactionos.coreapi.governanceconsole;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskModelRouter;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerConfiguration;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerProperties;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceSectionResponse;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
import com.recruitingtransactionos.coreapi.governanceconfig.JdbcGovernanceConfigPort;
import com.recruitingtransactionos.coreapi.observability.PerformanceCostDashboardPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class GovernanceConsoleReadServicePostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000500001");
  private static final UUID OTHER_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000500002");
  private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000500003");
  private static final UUID OTHER_ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000500004");
  private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000500005");
  private static final UUID JOB_ID = UUID.fromString("00000000-0000-0000-0000-000000500006");
  private static final UUID CANDIDATE_ID = UUID.fromString("00000000-0000-0000-0000-000000500007");
  private static final UUID MATCH_REPORT_ID = UUID.fromString("00000000-0000-0000-0000-000000500008");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static GovernanceConsoleReadService consoleReadService;
  private static DataSource dataSource;

  @BeforeAll
  static void migrateAndSeed() throws SQLException {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();

    dataSource = new DriverManagerDataSource(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
    ObjectMapper objectMapper = new ObjectMapper();
    GovernanceConfigService configService =
        new GovernanceConfigService(new JdbcGovernanceConfigPort(dataSource), objectMapper);
    AITaskRunnerProperties properties = new AITaskRunnerProperties();
    AITaskRunnerProperties.Route defaultRoute = new AITaskRunnerProperties.Route();
    defaultRoute.setProvider("deepseek");
    defaultRoute.setModel("deepseek-v4-pro");
    properties.getRoutes().put("default", defaultRoute);
    AITaskRunnerProperties.Route deterministicRoute = new AITaskRunnerProperties.Route();
    deterministicRoute.setProvider("deterministic");
    deterministicRoute.setModel("pilot-stub");
    properties.getRoutes().put("negative-case-generator", deterministicRoute);

    consoleReadService = new GovernanceConsoleReadService(
        dataSource,
        configService,
        new AITaskRunnerConfiguration().aiTaskDefinitionRegistry(),
        new AITaskModelRouter(properties, configService),
        properties,
        new PerformanceCostDashboardPolicy());

    seedTenant(ORGANIZATION_ID, ADMIN_USER_ID, "task50-admin@example.test");
    seedTenant(OTHER_ORGANIZATION_ID, OTHER_ADMIN_USER_ID, "task50-other@example.test");
    seedTask50Signals();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "eval-dashboard",
      "negative-cases",
      "review-quality",
      "model-routing",
      "cost-latency",
      "ontology-drift",
      "redaction-incidents",
      "ai-resume-authenticity-risk"
  })
  void task50AdminSectionsLoadFromTenantScopedSignalSources(String sectionKey) {
    GovernanceSectionResponse section = consoleReadService.loadAdminSection(ORGANIZATION_ID, sectionKey);

    assertThat(section.sectionKey()).isEqualTo(sectionKey);
    assertThat(section.metrics()).isNotEmpty();
    assertThat(section.editable()).isFalse();
  }

  @Test
  void evalDashboardSurfacesFailuresAndHallucinationRiskWithoutRawCandidateData() {
    GovernanceSectionResponse section = consoleReadService.loadAdminSection(ORGANIZATION_ID, "eval-dashboard");

    assertThat(metricValue(section, "failedRuns")).isEqualTo("1");
    assertThat(metricValue(section, "schemaRisks")).isEqualTo("1");
    assertThat(metricValue(section, "hallucinationRiskClaims")).isEqualTo("1");
    assertThat(joinedText(section)).contains("schema_validation_failed");
    assertThat(joinedText(section)).doesNotContain(CANDIDATE_ID.toString());
  }

  @Test
  void negativeCaseGeneratorIsDeterministicAuditableAndDoesNotCallLiveProvider() {
    GovernanceSectionResponse first = consoleReadService.loadAdminSection(ORGANIZATION_ID, "negative-cases");
    GovernanceSectionResponse second = consoleReadService.loadAdminSection(ORGANIZATION_ID, "negative-cases");

    assertThat(first.items()).isEqualTo(second.items());
    assertThat(first.items())
        .anySatisfy(item -> {
          assertThat(item.primaryText()).contains("candidate-profile-parser.schema.invalid-output");
          assertThat(item.detail()).contains("source:task-registry", "expected:schema_validation_failed");
        })
        .anySatisfy(item -> {
          assertThat(item.detail()).contains("source:privacy-redaction", "expected:redaction_blocked");
        })
        .anySatisfy(item -> {
          assertThat(item.detail()).contains("source:ontology-drift", "expected:ontology_review_required");
        });
    assertThat(first.warnings())
        .contains("Negative cases are deterministic fixtures generated from registry, ontology, and privacy signals; no live LLM call was made.");
  }

  @Test
  void costLatencyDashboardPreservesWatchAndMissingEvidenceDistinction() {
    GovernanceSectionResponse section = consoleReadService.loadAdminSection(ORGANIZATION_ID, "cost-latency");

    assertThat(section.items())
        .anySatisfy(item -> {
          assertThat(item.primaryText()).isEqualTo("interview-feedback-structurer");
          assertThat(item.status()).isEqualTo("WATCH");
          assertThat(item.detail()).contains("ai_task_projected_monthly_cost_near_budget");
        })
        .anySatisfy(item -> {
          assertThat(item.primaryText()).isEqualTo("candidate-profile-parser");
          assertThat(item.status()).isEqualTo("EVIDENCE_MISSING");
          assertThat(item.detail()).contains("not_instrumented");
        });
  }

  @Test
  void tenantBoundaryFiltersOtherOrganizationGovernanceSignals() {
    GovernanceSectionResponse section = consoleReadService.loadAdminSection(ORGANIZATION_ID, "eval-dashboard");

    assertThat(joinedText(section)).contains("schema_validation_failed");
    assertThat(joinedText(section)).doesNotContain("other_org_schema_failure");
  }

  @Test
  void redactionAndAuthenticityDashboardsExposeSafeRefsOnly() {
    GovernanceSectionResponse redaction =
        consoleReadService.loadAdminSection(ORGANIZATION_ID, "redaction-incidents");
    GovernanceSectionResponse authenticity =
        consoleReadService.loadAdminSection(ORGANIZATION_ID, "ai-resume-authenticity-risk");

    assertThat(metricValue(redaction, "incidents")).isEqualTo("1");
    assertThat(metricValue(authenticity, "highRiskReports")).isEqualTo("1");
    assertThat(joinedText(redaction)).doesNotContain(CANDIDATE_ID.toString());
    assertThat(joinedText(authenticity)).doesNotContain(CANDIDATE_ID.toString());
    assertThat(joinedText(authenticity)).contains(MATCH_REPORT_ID.toString());
  }

  @Test
  void ownerSummaryKeepsAdminDepthOutButSurfacesActionableRiskClasses() {
    GovernanceSectionResponse section = consoleReadService.loadOwnerSummary(ORGANIZATION_ID);

    assertThat(section.sectionKey()).isEqualTo("ai-quality");
    assertThat(metricValue(section, "aiTaskFailures")).isEqualTo("1");
    assertThat(metricValue(section, "staleOntologyWarnings")).isEqualTo("7");
    assertThat(metricValue(section, "privacyIncidents")).isEqualTo("1");
    assertThat(metricValue(section, "highAuthenticityRisk")).isEqualTo("1");
    assertThat(section.items()).allSatisfy(item -> assertThat(item.route()).startsWith("owner/"));
  }

  private static void seedTenant(UUID organizationId, UUID userId, String email) throws SQLException {
    execute("""
        INSERT INTO identity.organization (
          organization_id, legal_name, display_name, status, default_timezone
        ) VALUES (?, ?, ?, 'active', 'UTC')
        """, statement -> {
      statement.setObject(1, organizationId);
      statement.setString(2, email);
      statement.setString(3, email);
    });
    execute("""
        INSERT INTO identity.user_account (
          user_account_id, organization_id, email, display_name, status
        ) VALUES (?, ?, ?, ?, 'active')
        """, statement -> {
      statement.setObject(1, userId);
      statement.setObject(2, organizationId);
      statement.setString(3, email);
      statement.setString(4, email);
    });
  }

  private static void seedTask50Signals() throws SQLException {
    UUID parserDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000500101");
    UUID feedbackDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000500102");
    UUID authenticityDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000500103");
    seedAiTaskDefinition(parserDefinitionId, ORGANIZATION_ID, "candidate-profile-parser");
    seedAiTaskDefinition(feedbackDefinitionId, ORGANIZATION_ID, "interview-feedback-structurer");
    seedAiTaskDefinition(authenticityDefinitionId, ORGANIZATION_ID, "authenticity-risk-assessor");
    seedAiTaskDefinition(UUID.fromString("00000000-0000-0000-0000-000000500104"), OTHER_ORGANIZATION_ID, "candidate-profile-parser");
    seedAiTaskRun(
        UUID.fromString("00000000-0000-0000-0000-000000500201"),
        ORGANIZATION_ID,
        parserDefinitionId,
        "candidate-profile-parser",
        "failed",
        null,
        "schema_validation_failed",
        "{}",
        "{}",
        OffsetDateTime.parse("2026-05-10T01:00:00Z"),
        OffsetDateTime.parse("2026-05-10T01:00:04Z"));
    seedAiTaskRun(
        UUID.fromString("00000000-0000-0000-0000-000000500202"),
        ORGANIZATION_ID,
        feedbackDefinitionId,
        "interview-feedback-structurer",
        "succeeded",
        "0.018000",
        null,
        "{}",
        "{\"structuredFeedback\":\"ok\"}",
        OffsetDateTime.parse("2026-05-10T01:02:00Z"),
        OffsetDateTime.parse("2026-05-10T01:02:23Z"));
    seedAiTaskRun(
        UUID.fromString("00000000-0000-0000-0000-000000500203"),
        ORGANIZATION_ID,
        authenticityDefinitionId,
        "authenticity-risk-assessor",
        "succeeded",
        "0.011000",
        null,
        "{}",
        "{\"authenticityRisk\":\"high\",\"specificityScore\":41,\"independentEvidenceGap\":true,\"flags\":[\"generic_project_claims\"]}",
        OffsetDateTime.parse("2026-05-10T01:03:00Z"),
        OffsetDateTime.parse("2026-05-10T01:03:12Z"));
    seedAiTaskRun(
        UUID.fromString("00000000-0000-0000-0000-000000500204"),
        OTHER_ORGANIZATION_ID,
        UUID.fromString("00000000-0000-0000-0000-000000500104"),
        "candidate-profile-parser",
        "failed",
        null,
        "other_org_schema_failure",
        "{}",
        "{}",
        OffsetDateTime.parse("2026-05-10T01:04:00Z"),
        OffsetDateTime.parse("2026-05-10T01:04:04Z"));

    seedCompanyJobCandidate();
    seedMatchReport();
    seedReviewEvent();
    seedClaimLedgerItem();
    seedRedactionIncident();
  }

  private static void seedAiTaskDefinition(UUID definitionId, UUID organizationId, String taskKey)
      throws SQLException {
    execute("""
        INSERT INTO governance.ai_task_definition (
          ai_task_definition_id, organization_id, task_key, task_version, status,
          input_schema_version, output_schema_version, human_review_policy,
          model_routing_policy, write_back_target, eval_suite_ref
        ) VALUES (?, ?, ?, ?, 'active', 'input.v1', 'output.v1', '{}'::jsonb, '{}'::jsonb, 'no_write_back', ?)
        """, statement -> {
      statement.setObject(1, definitionId);
      statement.setObject(2, organizationId);
      statement.setString(3, taskKey);
      statement.setString(4, taskKey + ".v1");
      statement.setString(5, "/ai/evals/" + taskKey + "-eval-cases.json");
    });
  }

  private static void seedAiTaskRun(
      UUID runId,
      UUID organizationId,
      UUID definitionId,
      String taskKey,
      String status,
      String costUnits,
      String failureReason,
      String inputPayload,
      String outputPayload,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt)
      throws SQLException {
    execute("""
        INSERT INTO governance.ai_task_run (
          ai_task_run_id, organization_id, ai_task_definition_id, task_version, status,
          input_schema_version, output_schema_version, prompt_version, model_provider, model_name,
          human_review_status, started_at, completed_at, error_code, cost_units, failure_reason,
          requested_by_user_id, requested_by_role, input_payload, output_payload
        ) VALUES (?, ?, ?, ?, ?, 'input.v1', 'output.v1', 'prompt.v1', 'deepseek', 'deepseek-v4-pro',
          'required', ?, ?, ?, ?::numeric, ?, ?, 'admin'::governance.actor_role, ?::jsonb, ?::jsonb)
        """, statement -> {
      statement.setObject(1, runId);
      statement.setObject(2, organizationId);
      statement.setObject(3, definitionId);
      statement.setString(4, taskKey + ".v1");
      statement.setString(5, status);
      statement.setObject(6, startedAt);
      statement.setObject(7, completedAt);
      statement.setString(8, failureReason);
      statement.setString(9, costUnits);
      statement.setString(10, failureReason);
      statement.setObject(11, organizationId.equals(ORGANIZATION_ID) ? ADMIN_USER_ID : OTHER_ADMIN_USER_ID);
      statement.setString(12, inputPayload);
      statement.setString(13, outputPayload);
    });
  }

  private static void seedCompanyJobCandidate() throws SQLException {
    execute("""
        INSERT INTO recruiting.company (
          company_id, organization_id, name, status, owner_consultant_id
        ) VALUES (?, ?, 'Task50 Company', 'active', ?)
        """, statement -> {
      statement.setObject(1, COMPANY_ID);
      statement.setObject(2, ORGANIZATION_ID);
      statement.setObject(3, ADMIN_USER_ID);
    });
    execute("""
        INSERT INTO recruiting.job (
          job_id, organization_id, company_id, title, status, owner_consultant_id
        ) VALUES (?, ?, ?, 'Task50 Governance Role', 'activated', ?)
        """, statement -> {
      statement.setObject(1, JOB_ID);
      statement.setObject(2, ORGANIZATION_ID);
      statement.setObject(3, COMPANY_ID);
      statement.setObject(4, ADMIN_USER_ID);
    });
    execute("""
        INSERT INTO recruiting.candidate (
          candidate_id, organization_id, status, privacy_status, owner_consultant_id
        ) VALUES (?, ?, 'available', 'internal_only', ?)
        """, statement -> {
      statement.setObject(1, CANDIDATE_ID);
      statement.setObject(2, ORGANIZATION_ID);
      statement.setObject(3, ADMIN_USER_ID);
    });
  }

  private static void seedMatchReport() throws SQLException {
    execute("""
        INSERT INTO recruiting.match_report (
          match_report_id, organization_id, job_id, candidate_id, shortlist_candidate_card_id,
          subject_type, match_subject_ref, proposed_score, overall_score, score_confidence,
          cap_applied, cap_reason, cap_safe_explanation, human_review_required,
          additional_evidence_required, client_delivery_blocked, authenticity_risk,
          reidentification_risk_signal, ontology_version, industry_pack_version,
          dimension_scores, evidence_coverage, provenance_summary, explanations,
          interview_questions, industry_pack_key, industry_pack_maturity, ontology_stale,
          selection_reason, anti_pattern_warnings, generated_at
        ) VALUES (
          ?, ?, ?, ?, NULL, 'candidate', 'governance-safe-subject', 5, 4, 'MEDIUM',
          true, 'high_authenticity_risk', 'High authenticity risk requires stronger evidence.',
          true, true, false, 'HIGH', 'LOW', 'ontology-semiconductor-v2', 'semiconductor-v2',
          '{}'::jsonb, '{}'::jsonb, '{"authenticityRisk":"HIGH"}'::jsonb,
          '["authenticity task found generic project claims"]'::jsonb, '[]'::jsonb,
          'semiconductor', 'production', false, 'Task50 seeded report', '[]'::jsonb, ?
        )
        """, statement -> {
      statement.setObject(1, MATCH_REPORT_ID);
      statement.setObject(2, ORGANIZATION_ID);
      statement.setObject(3, JOB_ID);
      statement.setObject(4, CANDIDATE_ID);
      statement.setObject(5, OffsetDateTime.parse("2026-05-10T01:05:00Z"));
    });
  }

  private static void seedReviewEvent() throws SQLException {
    execute("""
        INSERT INTO governance.review_event (
          review_event_id, organization_id, reviewer_user_id, target_entity_type, target_entity_id,
          field_path, risk_tier, decision, bulk_flag, duration_ms, reason,
          sample_audit_status, review_velocity_bucket, status, created_at
        ) VALUES (
          ?, ?, ?, 'candidate_profile', ?, 'profile.summary',
          'T2_MEDIUM_RISK'::governance.risk_tier, 'approved', true, 500,
          'bulk_ack_without_verification', 'failed', 'too_fast', 'failed_audit', ?
        )
        """, statement -> {
      statement.setObject(1, UUID.fromString("00000000-0000-0000-0000-000000500301"));
      statement.setObject(2, ORGANIZATION_ID);
      statement.setObject(3, ADMIN_USER_ID);
      statement.setObject(4, CANDIDATE_ID);
      statement.setObject(5, OffsetDateTime.parse("2026-05-10T01:06:00Z"));
    });
  }

  private static void seedClaimLedgerItem() throws SQLException {
    execute("""
        INSERT INTO governance.claim_ledger_item (
          claim_ledger_item_id, organization_id, entity_type, entity_id, claim_type,
          assertion_strength, source_span_ref, speaker, verification_status,
          canonical_write_allowed, client_shareability, target_field_path,
          confidence, claim_value_text, created_by
        ) VALUES (
          ?, ?, 'candidate', ?, 'fact'::governance.claim_type,
          'implied'::governance.assertion_strength, 'task50-span', 'consultant'::governance.actor_role,
          'system_inference'::governance.verification_status, false,
          'internal_only'::governance.client_shareability, 'profile.summary', 0.42,
          'AI-looking claim that still needs evidence', ?
        )
        """, statement -> {
      statement.setObject(1, UUID.fromString("00000000-0000-0000-0000-000000500401"));
      statement.setObject(2, ORGANIZATION_ID);
      statement.setObject(3, CANDIDATE_ID);
      statement.setObject(4, ADMIN_USER_ID);
    });
  }

  private static void seedRedactionIncident() throws SQLException {
    execute("""
        INSERT INTO privacy.reidentification_risk_assessment (
          reidentification_risk_assessment_ref, organization_id, candidate_card_id,
          candidate_ref, job_ref, redaction_level, risk_level, decision,
          unsafe_features, risk_score, explanation, recorded_at
        ) VALUES (
          'task50-redaction-incident', ?, 'anonymous-card-task50',
          ?, 'job-safe-ref', 'L1', 'critical', 'block',
          ARRAY['rare_project_name', 'exact_tapeout_timeline'], 0.94,
          'Unsafe feature combination can re-identify the candidate.', ?
        )
        """, statement -> {
      statement.setObject(1, ORGANIZATION_ID);
      statement.setString(2, CANDIDATE_ID.toString());
      statement.setObject(3, OffsetDateTime.parse("2026-05-10T01:07:00Z"));
    });
  }

  private static String metricValue(GovernanceSectionResponse section, String metricKey) {
    return section.metrics().stream()
        .filter(metric -> metric.key().equals(metricKey))
        .findFirst()
        .orElseThrow()
        .value();
  }

  private static String joinedText(GovernanceSectionResponse section) {
    StringBuilder builder = new StringBuilder(section.title()).append(' ').append(section.description());
    section.metrics().forEach(metric -> builder.append(' ')
        .append(metric.key()).append(' ')
        .append(metric.value()).append(' ')
        .append(metric.helperText()));
    section.items().forEach(item -> builder.append(' ')
        .append(item.primaryText()).append(' ')
        .append(item.secondaryText()).append(' ')
        .append(item.status()).append(' ')
        .append(item.detail()));
    section.warnings().forEach(warning -> builder.append(' ').append(warning));
    return builder.toString();
  }

  private static void execute(String sql, StatementBinder binder) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      binder.bind(statement);
      statement.executeUpdate();
    }
  }

  @FunctionalInterface
  private interface StatementBinder {
    void bind(PreparedStatement statement) throws SQLException;
  }
}
