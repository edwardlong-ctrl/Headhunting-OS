package com.recruitingtransactionos.coreapi.privacyredaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateRef;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientVisibleCandidateFieldPolicy;
import com.recruitingtransactionos.coreapi.clientsafeprojection.InternalCandidateProjectionSnapshot;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessmentService;
import com.recruitingtransactionos.coreapi.privacyredaction.persistence.JdbcReidentificationRiskAssessmentPort;
import com.recruitingtransactionos.coreapi.privacyredaction.port.ReidentificationRiskAssessmentPort;
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
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedactionAuditTransactionalIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Instant NOW = Instant.parse("2026-05-03T10:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

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
  void persistedAssessmentStoresWorkflowEventId() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000030b001");
    UUID actorId = uuid("00000000-0000-0000-0000-00000030b002");
    insertOrganizationAndUser(organizationId, actorId);

    ReidentificationRiskAssessmentPort port = new JdbcReidentificationRiskAssessmentPort(dataSource);
    RedactionAuditService service = auditedService(port);

    RedactionAuditService.RedactionAuditResult result = service.evaluate(
        new RedactionAuditService.RedactionAuditRequest(
            organizationId,
            "reidentification_risk_assessment_task30_workflow_id_001",
            "candidate_workflow_id",
            "job_workflow_id",
            cleanSnapshot("card_task30_workflow_id_001"),
            actorId,
            ActorRole.SYSTEM,
            "task30_workflow_id_test",
            null,
            NOW));

    assertThat(port.findByRefAndOrganizationId(
        organizationId,
        "reidentification_risk_assessment_task30_workflow_id_001"))
            .hasValueSatisfying(value -> assertThat(value.workflowEventId()).contains(result.workflowEventId()));
  }

  @Test
  void failedAssessmentInsertRollsBackWorkflowEvent() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000030b101");
    UUID actorId = uuid("00000000-0000-0000-0000-00000030b102");
    insertOrganizationAndUser(organizationId, actorId);

    ReidentificationRiskAssessmentPort port = new JdbcReidentificationRiskAssessmentPort(dataSource);
    RedactionAuditService service = auditedService(port);
    RedactionAuditService.RedactionAuditRequest request =
        new RedactionAuditService.RedactionAuditRequest(
            organizationId,
            "reidentification_risk_assessment_task30_tx_rollback_001",
            "candidate_tx_rollback",
            "job_tx_rollback",
            cleanSnapshot("card_task30_tx_rollback_001"),
            actorId,
            ActorRole.SYSTEM,
            "task30_tx_rollback_test",
            null,
            NOW);

    service.evaluate(request);
    int workflowEventsAfterFirstInsert = countWorkflowEventRows(organizationId);

    assertThatThrownBy(() -> service.evaluate(request))
        .isInstanceOf(RuntimeException.class);

    assertThat(countAssessmentRows(organizationId)).isEqualTo(1);
    assertThat(countWorkflowEventRows(organizationId)).isEqualTo(workflowEventsAfterFirstInsert);
  }

  private static RedactionAuditService auditedService(ReidentificationRiskAssessmentPort port) {
    return new RedactionAuditService(
        new ReidentificationRiskAssessmentService(),
        port,
        new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)),
        new SpringRedactionAuditTransactionBoundary(new DataSourceTransactionManager(dataSource)));
  }

  private static InternalCandidateProjectionSnapshot cleanSnapshot(String cardId) {
    return new InternalCandidateProjectionSnapshot(
        "00000000-0000-0000-0000-0000003002b1",
        "00000000-0000-0000-0000-0000003002b2",
        "Jane Safe Candidate",
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

  private static int countWorkflowEventRows(UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM workflow.workflow_event WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
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
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
      }
    };
  }
}
