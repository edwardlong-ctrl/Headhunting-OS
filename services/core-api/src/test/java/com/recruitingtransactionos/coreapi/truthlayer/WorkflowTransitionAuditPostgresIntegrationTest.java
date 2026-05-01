package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowAuditReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class WorkflowTransitionAuditPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Instant BASE_TIME = Instant.parse("2026-04-28T07:00:00Z");

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
  void transitionAuditEventPersistsAndTask4CReadModelCanReadIt() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-0000000d0201");
    UUID jobId = uuid("00000000-0000-0000-0000-0000000d0202");
    UUID actorId = uuid("00000000-0000-0000-0000-0000000d0203");
    UUID sourceRefId = uuid("00000000-0000-0000-0000-0000000d0204");
    UUID correlationId = uuid("00000000-0000-0000-0000-0000000d0205");
    UUID causationId = uuid("00000000-0000-0000-0000-0000000d0206");
    insertOrganizationAndUser(organizationId, actorId);

    WorkflowEventId workflowEventId = transitionService().record(WorkflowTransitionAuditRequest
        .builder()
        .organizationId(organizationId)
        .entityNamespace("recruiting")
        .entityType("JOB")
        .entityId(jobId)
        .entityVersion(3)
        .actionCode("JOB_ACTIVATED")
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState("{\"status\":\"contract_pending\"}")
        .afterState("{\"status\":\"active\"}")
        .reason("consultant confirmed commercial terms before activation")
        .idempotencyKey("transition-audit-postgres-persisted")
        .correlationId(correlationId)
        .causationId(causationId)
        .sourceType("domain_service")
        .sourceRefId(sourceRefId)
        .occurredAt(BASE_TIME)
        .build())
        .workflowEventId();

    WorkflowAuditRecord record = queryService().findById(organizationId, workflowEventId)
        .orElseThrow();

    assertThat(record.workflowEventId()).isEqualTo(workflowEventId);
    assertThat(record.organizationId()).isEqualTo(organizationId);
    assertThat(record.entityNamespace()).isEqualTo("recruiting");
    assertThat(record.entityType()).isEqualTo("JOB");
    assertThat(record.entityId()).isEqualTo(jobId);
    assertThat(record.actionCode()).isEqualTo("JOB_ACTIVATED");
    assertThat(record.actorType()).isEqualTo(ActorRole.CONSULTANT);
    assertThat(record.actorId()).isEqualTo(actorId);
    assertThat(record.aiInvolvement()).isEqualTo(WorkflowAiInvolvement.NONE);
    assertThat(record.riskTier()).isEqualTo(RiskTier.T3_HIGH_RISK);
    assertThat(record.beforeState().json()).contains("contract_pending");
    assertThat(record.afterState().json()).contains("active");
    assertThat(record.reason()).isEqualTo("consultant confirmed commercial terms before activation");
    assertThat(record.idempotencyKey())
        .isEqualTo(new WorkflowIdempotencyKey("transition-audit-postgres-persisted"));
    assertThat(record.correlationId()).isEqualTo(new WorkflowCorrelationId(correlationId));
    assertThat(record.causationId()).isEqualTo(new WorkflowCausationId(causationId));
    assertThat(record.sourceType()).isEqualTo("domain_service");
    assertThat(record.sourceRefId()).isEqualTo(sourceRefId);
    assertThat(record.occurredAt()).isEqualTo(BASE_TIME);
  }

  @Test
  void transitionAuditReadModelRemainsScopedByOrganization() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-0000000d0301");
    UUID otherOrganizationId = uuid("00000000-0000-0000-0000-0000000d0302");
    UUID candidateId = uuid("00000000-0000-0000-0000-0000000d0303");
    UUID otherCandidateId = uuid("00000000-0000-0000-0000-0000000d0304");
    UUID actorId = uuid("00000000-0000-0000-0000-0000000d0305");
    UUID otherActorId = uuid("00000000-0000-0000-0000-0000000d0306");
    insertOrganizationAndUser(organizationId, actorId);
    insertOrganizationAndUser(otherOrganizationId, otherActorId);

    WorkflowEventId expected = transitionService().record(request(
        organizationId,
        candidateId,
        actorId,
        "transition-audit-org-expected",
        BASE_TIME.plusSeconds(1)))
        .workflowEventId();
    WorkflowEventId otherOrg = transitionService().record(request(
        otherOrganizationId,
        otherCandidateId,
        otherActorId,
        "transition-audit-org-other",
        BASE_TIME.plusSeconds(2)))
        .workflowEventId();

    List<WorkflowAuditRecord> records = queryService().search(WorkflowAuditQuery
        .builder(organizationId)
        .actionCode("CANDIDATE_MARKED_AVAILABLE")
        .build());

    assertThat(records).extracting(WorkflowAuditRecord::organizationId)
        .containsOnly(organizationId);
    assertThat(records).extracting(WorkflowAuditRecord::workflowEventId)
        .contains(expected)
        .doesNotContain(otherOrg);
  }

  private static WorkflowTransitionAuditRequest request(
      UUID organizationId,
      UUID candidateId,
      UUID actorId,
      String idempotencyKey,
      Instant occurredAt) {
    return WorkflowTransitionAuditRequest.builder()
        .organizationId(organizationId)
        .entityNamespace("recruiting")
        .entityType("CANDIDATE")
        .entityId(candidateId)
        .entityVersion(8)
        .actionCode("CANDIDATE_MARKED_AVAILABLE")
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState("{\"status\":\"consultant_review\"}")
        .afterState("{\"status\":\"available\"}")
        .reason("consultant confirmed candidate availability")
        .idempotencyKey(idempotencyKey)
        .sourceType("domain_service")
        .occurredAt(occurredAt)
        .build();
  }

  private static WorkflowTransitionAuditService transitionService() {
    return new WorkflowTransitionAuditService(
        new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)),
        new com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort() {
          @Override
          public java.util.Optional<String> getCurrentStateJson(UUID orgId, String ns, String type, UUID id) { return java.util.Optional.empty(); }
        });
  }

  private static WorkflowAuditQueryService queryService() {
    return new WorkflowAuditQueryService(new JdbcWorkflowAuditReadPort(dataSource));
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
      organization.setString(2, "Task 4D Org " + organizationId);
      organization.setString(3, "Task 4D Org");
      organization.executeUpdate();

      user.setObject(1, userId);
      user.setObject(2, organizationId);
      user.setString(3, "workflow-transition-actor-" + userId + "@example.test");
      user.setString(4, "Task 4D Transition Actor");
      user.executeUpdate();
    }
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
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
      public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
      }

      @Override
      public void setLogWriter(PrintWriter out) {
        DriverManager.setLogWriter(out);
      }

      @Override
      public void setLoginTimeout(int seconds) {
        DriverManager.setLoginTimeout(seconds);
      }

      @Override
      public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("DriverManager parent logger is not supported");
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
          return iface.cast(this);
        }
        throw new SQLException("DataSource does not wrap " + iface.getName());
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
      }
    };
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }
}
