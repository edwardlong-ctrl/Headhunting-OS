package com.recruitingtransactionos.coreapi.privacyredaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateRef;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientVisibleCandidateFieldPolicy;
import com.recruitingtransactionos.coreapi.clientsafeprojection.InternalCandidateProjectionSnapshot;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessmentService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskDecision;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskFeature;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskLevel;
import com.recruitingtransactionos.coreapi.privacyredaction.persistence.JdbcReidentificationRiskAssessmentPort;
import com.recruitingtransactionos.coreapi.privacyredaction.port.ReidentificationRiskAssessmentPort;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Task 30 acceptance + persistence integration test.
 *
 * <p>Boots a fresh Postgres via Testcontainers, runs every Flyway migration
 * (including the new V25), and exercises the full re-identification risk
 * assessment / persistence / audit pipeline end-to-end.
 *
 * <p>The first test is the explicit Task 30 acceptance scenario:
 * "Top chip company + unique title + exact year + chip code name" must
 * be generalized or blocked before client display. We assert that
 * {@link RedactionAuditService} returns a {@code blocked == true} result
 * with decision {@link ReidentificationRiskDecision#BLOCK} and that the
 * persisted row + workflow event row both reflect the blocked outcome.
 */
@Testcontainers
class RedactionAuditPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE =
      DockerImageName.parse("postgres:16-alpine");
  private static final Instant NOW = Instant.parse("2026-04-29T13:45:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
  }

  @Test
  void acceptanceScenarioBlocksTopChipCompanyRareTitleExactYearAndChipCodeName()
      throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000030a001");
    UUID systemActorId = uuid("00000000-0000-0000-0000-00000030a002");
    insertOrganizationAndUser(organizationId, systemActorId);

    ReidentificationRiskAssessmentPort port = new JdbcReidentificationRiskAssessmentPort(dataSource);
    WorkflowEventService workflowEventService =
        new WorkflowEventService(new JdbcWorkflowEventPort(dataSource));
    RedactionAuditService service = new RedactionAuditService(
        new ReidentificationRiskAssessmentService(),
        port,
        workflowEventService);

    InternalCandidateProjectionSnapshot acceptanceSnapshot =
        acceptanceScenarioSnapshot(
            "card_task30_acceptance_001",
            "TSMC",
            List.of("Orion-X7 NPU"),
            "Chief Verification Architect at TSMC in 2024 driving Orion-X7 NPU tape-out.",
            "Owned Orion-X7 NPU verification at TSMC during the 2024 tape-out cycle.");

    RedactionAuditService.RedactionAuditResult result = service.evaluate(
        new RedactionAuditService.RedactionAuditRequest(
            organizationId,
            "reidentification_risk_assessment_task30_acceptance_001",
            "candidate_ref_task30_acceptance_001",
            "job_ref_task30_acceptance_001",
            acceptanceSnapshot,
            systemActorId,
            ActorRole.SYSTEM,
            "task30_acceptance_test",
            null,
            NOW));

    assertThat(result.blocked()).isTrue();
    assertThat(result.assessment().decision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(result.assessment().riskLevel()).isEqualTo(ReidentificationRiskLevel.HIGH);
    assertThat(result.assessment().unsafeFeatures())
        .contains(
            ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER,
            ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME,
            ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR);
    // The block decision means the redacted snapshot will never reach a
    // client. We still assert the strongest identity-revealing tokens are
    // generalized away. The year by itself, once company and chip names are
    // redacted, is not uniquely re-identifying and is allowed to remain in
    // the redacted text.
    assertThat(result.redactedSnapshot().safeSummary())
        .doesNotContain("TSMC")
        .doesNotContain("Orion-X7");

    Optional<PersistedReidentificationRiskAssessment> reloaded =
        port.findByRefAndOrganizationId(
            organizationId,
            "reidentification_risk_assessment_task30_acceptance_001");
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().assessment().decision())
        .isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(reloaded.get().assessment().riskScore()).isGreaterThan(0.7);
    assertThat(reloaded.get().assessment().unsafeFeatures())
        .contains(
            ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER,
            ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);

    assertThat(countAssessmentRows(organizationId)).isEqualTo(1);
    assertThat(findWorkflowAction(result.workflowEventId().value()))
        .isEqualTo(WorkflowActionCode.CLIENT_SAFE_REDACTION_BLOCKED.wireValue());
  }

  @Test
  void cleanSnapshotIsAllowedAndRecordsLowRiskAssessment() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000030a101");
    UUID systemActorId = uuid("00000000-0000-0000-0000-00000030a102");
    insertOrganizationAndUser(organizationId, systemActorId);

    ReidentificationRiskAssessmentPort port = new JdbcReidentificationRiskAssessmentPort(dataSource);
    RedactionAuditService service = new RedactionAuditService(
        new ReidentificationRiskAssessmentService(),
        port,
        new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)));

    InternalCandidateProjectionSnapshot cleanSnapshot = cleanSnapshot("card_task30_clean_001");

    RedactionAuditService.RedactionAuditResult result = service.evaluate(
        new RedactionAuditService.RedactionAuditRequest(
            organizationId,
            "reidentification_risk_assessment_task30_clean_001",
            "candidate_ref_task30_clean_001",
            "job_ref_task30_clean_001",
            cleanSnapshot,
            systemActorId,
            ActorRole.SYSTEM,
            "task30_clean_test",
            null,
            NOW));

    assertThat(result.blocked()).isFalse();
    assertThat(result.assessment().decision()).isEqualTo(ReidentificationRiskDecision.ALLOW);
    assertThat(result.assessment().riskLevel()).isEqualTo(ReidentificationRiskLevel.LOW);
    assertThat(result.assessment().unsafeFeatures()).isEmpty();
    assertThat(result.assessment().riskScore()).isZero();
    assertThat(findWorkflowAction(result.workflowEventId().value()))
        .isEqualTo(WorkflowActionCode.REIDENTIFICATION_RISK_ASSESSED.wireValue());
  }

  @Test
  void persistedAssessmentsAreScopedByOrganization() throws SQLException {
    UUID organizationA = uuid("00000000-0000-0000-0000-00000030a201");
    UUID organizationB = uuid("00000000-0000-0000-0000-00000030a202");
    UUID actorA = uuid("00000000-0000-0000-0000-00000030a203");
    UUID actorB = uuid("00000000-0000-0000-0000-00000030a204");
    insertOrganizationAndUser(organizationA, actorA);
    insertOrganizationAndUser(organizationB, actorB);

    ReidentificationRiskAssessmentPort port = new JdbcReidentificationRiskAssessmentPort(dataSource);
    RedactionAuditService service = new RedactionAuditService(
        new ReidentificationRiskAssessmentService(),
        port,
        new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)));

    String sharedRef = "reidentification_risk_assessment_task30_cross_org_shared";
    InternalCandidateProjectionSnapshot snapshotA = cleanSnapshot("card_task30_cross_org_a");
    InternalCandidateProjectionSnapshot snapshotB = cleanSnapshot("card_task30_cross_org_b");

    RedactionAuditService.RedactionAuditResult resultA = service.evaluate(
        new RedactionAuditService.RedactionAuditRequest(
            organizationA, sharedRef,
            "candidate_a", "job_a",
            snapshotA, actorA, ActorRole.SYSTEM, "task30_cross_org",
            null, NOW));
    RedactionAuditService.RedactionAuditResult resultB = service.evaluate(
        new RedactionAuditService.RedactionAuditRequest(
            organizationB, sharedRef,
            "candidate_b", "job_b",
            snapshotB, actorB, ActorRole.SYSTEM, "task30_cross_org",
            null, NOW));

    assertThat(resultA.workflowEventId()).isNotEqualTo(resultB.workflowEventId());
    assertThat(port.findByRefAndOrganizationId(organizationA, sharedRef))
        .hasValueSatisfying(value -> assertThat(value.candidateRef()).isEqualTo("candidate_a"));
    assertThat(port.findByRefAndOrganizationId(organizationB, sharedRef))
        .hasValueSatisfying(value -> assertThat(value.candidateRef()).isEqualTo("candidate_b"));
    assertThat(countAssessmentRows(organizationA)).isEqualTo(1);
    assertThat(countAssessmentRows(organizationB)).isEqualTo(1);
  }

  @Test
  void findRecentByCandidateCardIdReturnsNewestFirst() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000030a301");
    UUID actorId = uuid("00000000-0000-0000-0000-00000030a302");
    insertOrganizationAndUser(organizationId, actorId);

    ReidentificationRiskAssessmentPort port = new JdbcReidentificationRiskAssessmentPort(dataSource);
    RedactionAuditService service = new RedactionAuditService(
        new ReidentificationRiskAssessmentService(),
        port,
        new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)));

    String cardId = "card_task30_recent_001";
    InternalCandidateProjectionSnapshot snapshot = cleanSnapshot(cardId);

    Instant t1 = NOW.minusSeconds(7200);
    Instant t2 = NOW.minusSeconds(3600);
    Instant t3 = NOW;

    service.evaluate(new RedactionAuditService.RedactionAuditRequest(
        organizationId, "reidentification_risk_assessment_task30_recent_old",
        "candidate_recent", "job_recent", snapshot, actorId, ActorRole.SYSTEM,
        "task30_recent", null, t1));
    service.evaluate(new RedactionAuditService.RedactionAuditRequest(
        organizationId, "reidentification_risk_assessment_task30_recent_mid",
        "candidate_recent", "job_recent", snapshot, actorId, ActorRole.SYSTEM,
        "task30_recent", null, t2));
    service.evaluate(new RedactionAuditService.RedactionAuditRequest(
        organizationId, "reidentification_risk_assessment_task30_recent_new",
        "candidate_recent", "job_recent", snapshot, actorId, ActorRole.SYSTEM,
        "task30_recent", null, t3));

    List<PersistedReidentificationRiskAssessment> recent =
        port.findRecentByCandidateCardId(organizationId, cardId, 10);

    assertThat(recent).hasSize(3);
    assertThat(recent.get(0).reidentificationRiskAssessmentRef())
        .isEqualTo("reidentification_risk_assessment_task30_recent_new");
    assertThat(recent.get(2).reidentificationRiskAssessmentRef())
        .isEqualTo("reidentification_risk_assessment_task30_recent_old");
  }

  private static InternalCandidateProjectionSnapshot acceptanceScenarioSnapshot(
      String cardId,
      String exactCurrentEmployer,
      List<String> chipNames,
      String headline,
      String evidence) {
    return new InternalCandidateProjectionSnapshot(
        "00000000-0000-0000-0000-0000003001a1",
        "00000000-0000-0000-0000-0000003001a2",
        "Jane Alpha Candidate",
        null,
        null,
        null,
        exactCurrentEmployer,
        chipNames,
        "Jane Alpha Candidate raw resume text.",
        "Internal notes about the candidate.",
        AnonymousCandidateCardId.of(cardId),
        AnonymousCandidateRef.of("anon_candidate_" + cardId),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        headline,
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        evidence,
        "SystemVerilog, UVM, coverage closure, cross-team debug leadership.",
        List.of(evidence),
        List.of("Strong fit at the verification leadership level."),
        ClientVisibleCandidateFieldPolicy.safeAllowlistedFieldPaths());
  }

  private static InternalCandidateProjectionSnapshot cleanSnapshot(String cardId) {
    return new InternalCandidateProjectionSnapshot(
        "00000000-0000-0000-0000-0000003001b1",
        "00000000-0000-0000-0000-0000003001b2",
        "Jane Beta Candidate",
        null,
        null,
        null,
        null,
        List.of(),
        null,
        null,
        AnonymousCandidateCardId.of(cardId),
        AnonymousCandidateRef.of("anon_candidate_" + cardId),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        "Senior verification leader in advanced-chip programs",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has led complex verification programs without disclosing employer or code names.",
        "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
        List.of("Evidence generalized from approved profile signals."),
        List.of("Strong fit based on generalized capability evidence."),
        ClientVisibleCandidateFieldPolicy.safeAllowlistedFieldPaths());
  }

  private static int countAssessmentRows(UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM privacy.reidentification_risk_assessment WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static String findWorkflowAction(UUID workflowEventId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT action FROM workflow.workflow_event WHERE workflow_event_id = ?")) {
      statement.setObject(1, workflowEventId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new AssertionError("workflow event not found");
        }
        return resultSet.getString("action");
      }
    }
  }

  private static void insertOrganizationAndUser(UUID organizationId, UUID userId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement organization = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id,
              legal_name,
              display_name,
              status,
              default_timezone
            )
            VALUES (?, ?, ?, 'active', 'UTC')
            """);
        PreparedStatement user = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id,
              organization_id,
              email,
              display_name,
              status
            )
            VALUES (?, ?, ?, ?, 'active')
            """)) {
      organization.setObject(1, organizationId);
      organization.setString(2, "Org " + organizationId);
      organization.setString(3, "Org " + organizationId);
      organization.executeUpdate();

      user.setObject(1, userId);
      user.setObject(2, organizationId);
      user.setString(3, "user-" + userId + "@example.com");
      user.setString(4, "User " + userId);
      user.executeUpdate();
    }
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private static DataSource postgresDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return connection();
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
      }

      @Override
      public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public int getLoginTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
          return iface.cast(this);
        }
        throw new SQLException("DataSource does not wrap " + iface.getName());
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return iface.isAssignableFrom(getClass());
      }
    };
  }
}
