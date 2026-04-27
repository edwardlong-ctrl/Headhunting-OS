package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowAuditReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.io.PrintWriter;
import java.lang.reflect.RecordComponent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class WorkflowAuditPostgresReadModelIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Instant BASE_TIME = Instant.parse("2026-04-28T05:00:00Z");

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
  void queryByWorkflowEventIdReturnsExpectedAuditProjection() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-0000000b0101");
    UUID candidateId = uuid("00000000-0000-0000-0000-0000000b0102");
    UUID actorId = uuid("00000000-0000-0000-0000-0000000b0106");
    insertOrganizationAndUser(organizationId, actorId);
    WorkflowEventAppendCommand command = commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-query-by-id")
        .correlationId(uuid("00000000-0000-0000-0000-0000000b0103"))
        .causationId(uuid("00000000-0000-0000-0000-0000000b0104"))
        .sourceRefId(uuid("00000000-0000-0000-0000-0000000b0105"))
        .occurredAt(BASE_TIME)
        .build();

    WorkflowEventId workflowEventId = append(command);

    WorkflowAuditRecord record = queryService().findById(organizationId, workflowEventId)
        .orElseThrow();

    assertThat(record.workflowEventId()).isEqualTo(workflowEventId);
    assertThat(record.organizationId()).isEqualTo(organizationId);
    assertThat(record.entityNamespace()).isEqualTo("recruiting");
    assertThat(record.entityType()).isEqualTo("CANDIDATE");
    assertThat(record.entityId()).isEqualTo(candidateId);
    assertThat(record.actionCode()).isEqualTo("CANDIDATE_SHORTLISTED");
    assertThat(record.actorType()).isEqualTo(ActorRole.CONSULTANT);
    assertThat(record.actorId()).isEqualTo(actorId);
    assertThat(record.aiInvolvement()).isEqualTo(WorkflowAiInvolvement.NONE);
    assertThat(record.riskTier()).isEqualTo(RiskTier.T3_HIGH_RISK);
    assertThat(record.beforeState().json()).contains("consultant_review");
    assertThat(record.afterState().json()).contains("client_review");
    assertThat(record.reason()).isEqualTo("consultant published anonymous shortlist after review");
    assertThat(record.idempotencyKey()).isEqualTo(new WorkflowIdempotencyKey("audit-query-by-id"));
    assertThat(record.correlationId())
        .isEqualTo(new WorkflowCorrelationId(uuid("00000000-0000-0000-0000-0000000b0103")));
    assertThat(record.causationId())
        .isEqualTo(new WorkflowCausationId(uuid("00000000-0000-0000-0000-0000000b0104")));
    assertThat(record.previousEventId()).isNull();
    assertThat(record.sourceType()).isEqualTo("domain_service");
    assertThat(record.sourceRefId()).isEqualTo(uuid("00000000-0000-0000-0000-0000000b0105"));
    assertThat(record.occurredAt()).isEqualTo(BASE_TIME);
    assertThat(record.createdAt()).isNotNull();
  }

  @Test
  void queryByEntityTypeAndEntityIdReturnsExpectedRecordsOnly() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-0000000b0201");
    UUID candidateId = uuid("00000000-0000-0000-0000-0000000b0202");
    UUID otherCandidateId = uuid("00000000-0000-0000-0000-0000000b0203");
    UUID actorId = uuid("00000000-0000-0000-0000-0000000b0204");
    insertOrganizationAndUser(organizationId, actorId);
    WorkflowEventId expected = append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-entity-expected")
        .occurredAt(BASE_TIME.plusSeconds(1))
        .build());
    append(commandBuilder(organizationId, otherCandidateId, actorId)
        .idempotencyKey("audit-entity-other")
        .occurredAt(BASE_TIME.plusSeconds(2))
        .build());

    List<WorkflowAuditRecord> records = queryService().search(WorkflowAuditQuery
        .builder(organizationId)
        .entityType("CANDIDATE")
        .entityId(candidateId)
        .build());

    assertThat(records).extracting(WorkflowAuditRecord::workflowEventId)
        .containsExactly(expected);
  }

  @Test
  void queryByCorrelationIdReturnsRelatedRecordsOnly() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-0000000b0301");
    UUID candidateId = uuid("00000000-0000-0000-0000-0000000b0302");
    UUID correlationId = uuid("00000000-0000-0000-0000-0000000b0303");
    UUID actorId = uuid("00000000-0000-0000-0000-0000000b0305");
    insertOrganizationAndUser(organizationId, actorId);
    WorkflowEventId second = append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-correlation-second")
        .correlationId(correlationId)
        .occurredAt(BASE_TIME.plusSeconds(2))
        .build());
    WorkflowEventId first = append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-correlation-first")
        .correlationId(correlationId)
        .occurredAt(BASE_TIME.plusSeconds(1))
        .build());
    append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-correlation-other")
        .correlationId(uuid("00000000-0000-0000-0000-0000000b0304"))
        .occurredAt(BASE_TIME.plusSeconds(3))
        .build());

    List<WorkflowAuditRecord> records = queryService().search(WorkflowAuditQuery
        .builder(organizationId)
        .correlationId(new WorkflowCorrelationId(correlationId))
        .build());

    assertThat(records).extracting(WorkflowAuditRecord::workflowEventId)
        .containsExactly(second, first);
  }

  @Test
  void queryByCausationIdReturnsCausedRecordsOnly() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-0000000b0401");
    UUID candidateId = uuid("00000000-0000-0000-0000-0000000b0402");
    UUID causationId = uuid("00000000-0000-0000-0000-0000000b0403");
    UUID actorId = uuid("00000000-0000-0000-0000-0000000b0405");
    insertOrganizationAndUser(organizationId, actorId);
    WorkflowEventId expected = append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-causation-expected")
        .causationId(causationId)
        .build());
    append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-causation-other")
        .causationId(uuid("00000000-0000-0000-0000-0000000b0404"))
        .build());

    List<WorkflowAuditRecord> records = queryService().search(WorkflowAuditQuery
        .builder(organizationId)
        .causationId(new WorkflowCausationId(causationId))
        .build());

    assertThat(records).extracting(WorkflowAuditRecord::workflowEventId)
        .containsExactly(expected);
  }

  @Test
  void queryByIdempotencyKeyAndActionCodeReturnsExpectedRecords() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-0000000b0501");
    UUID candidateId = uuid("00000000-0000-0000-0000-0000000b0502");
    UUID actorId = uuid("00000000-0000-0000-0000-0000000b0503");
    insertOrganizationAndUser(organizationId, actorId);
    WorkflowEventId expected = append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-idempotency-expected")
        .action("CANDIDATE_MARKED_AVAILABLE")
        .beforeState("{\"status\":\"consultant_review\"}")
        .afterState("{\"status\":\"available\"}")
        .build());
    append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-idempotency-other")
        .action("CANDIDATE_SHORTLISTED")
        .build());

    assertThat(queryService().search(WorkflowAuditQuery.builder(organizationId)
        .idempotencyKey(new WorkflowIdempotencyKey("audit-idempotency-expected"))
        .build()))
        .extracting(WorkflowAuditRecord::workflowEventId)
        .containsExactly(expected);
    assertThat(queryService().search(WorkflowAuditQuery.builder(organizationId)
        .actionCode("CANDIDATE_MARKED_AVAILABLE")
        .build()))
        .extracting(WorkflowAuditRecord::workflowEventId)
        .containsExactly(expected);
  }

  @Test
  void resultsAreDeterministicallyOrderedAndScopedByOrganization() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-0000000b0601");
    UUID otherOrganizationId = uuid("00000000-0000-0000-0000-0000000b0602");
    UUID candidateId = uuid("00000000-0000-0000-0000-0000000b0603");
    UUID otherCandidateId = uuid("00000000-0000-0000-0000-0000000b0604");
    UUID actorId = uuid("00000000-0000-0000-0000-0000000b0605");
    UUID otherActorId = uuid("00000000-0000-0000-0000-0000000b0606");
    insertOrganizationAndUser(organizationId, actorId);
    insertOrganizationAndUser(otherOrganizationId, otherActorId);
    WorkflowEventId older = append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-order-older")
        .occurredAt(BASE_TIME)
        .build());
    WorkflowEventId otherOrganizationEvent = append(commandBuilder(
        otherOrganizationId,
        otherCandidateId,
        otherActorId)
        .idempotencyKey("audit-order-other-org")
        .occurredAt(BASE_TIME.plusSeconds(10))
        .build());
    WorkflowEventId newer = append(commandBuilder(organizationId, candidateId, actorId)
        .idempotencyKey("audit-order-newer")
        .occurredAt(BASE_TIME.plusSeconds(1))
        .build());

    List<WorkflowAuditRecord> records = queryService().search(WorkflowAuditQuery
        .builder(organizationId)
        .limit(10)
        .build());

    assertThat(records).extracting(WorkflowAuditRecord::organizationId)
        .containsOnly(organizationId);
    assertThat(records).extracting(WorkflowAuditRecord::workflowEventId)
        .contains(newer, older)
        .doesNotContain(otherOrganizationEvent);
    assertThat(records).isSortedAccordingTo(Comparator
        .comparing(WorkflowAuditRecord::occurredAt, Comparator.reverseOrder())
        .thenComparing(record -> record.workflowEventId().value(), Comparator.reverseOrder()));
  }

  @Test
  void readModelDoesNotExposeRawCandidateOrProfilePayloadColumns() {
    List<String> componentNames = Stream.of(WorkflowAuditRecord.class.getRecordComponents())
        .map(RecordComponent::getName)
        .map(String::toLowerCase)
        .toList();

    assertThat(componentNames)
        .doesNotContain("rawcandidate", "candidateprofile", "profilepayload",
            "metadata", "storageref", "parsedtextref");
  }

  private static WorkflowAuditQueryService queryService() {
    return new WorkflowAuditQueryService(new JdbcWorkflowAuditReadPort(dataSource));
  }

  private static WorkflowEventId append(WorkflowEventAppendCommand command) {
    return new WorkflowEventService(new JdbcWorkflowEventPort(dataSource))
        .append(command)
        .workflowEventId();
  }

  private static WorkflowCommandBuilder commandBuilder(
      UUID organizationId,
      UUID candidateId,
      UUID actorId) {
    return new WorkflowCommandBuilder(organizationId, candidateId, actorId);
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
      organization.setString(2, "Task 4C Org " + organizationId);
      organization.setString(3, "Task 4C Org");
      organization.executeUpdate();

      user.setObject(1, userId);
      user.setObject(2, organizationId);
      user.setString(3, "workflow-audit-actor-" + userId + "@example.test");
      user.setString(4, "Task 4C Workflow Audit Actor");
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

  private static final class WorkflowCommandBuilder {
    private final UUID organizationId;
    private final UUID candidateId;
    private final UUID actorId;
    private String action = "CANDIDATE_SHORTLISTED";
    private String beforeState = "{\"status\":\"consultant_review\"}";
    private String afterState = "{\"status\":\"client_review\"}";
    private String idempotencyKey = "audit-read-model";
    private UUID correlationId;
    private UUID causationId;
    private UUID sourceRefId;
    private Instant occurredAt = BASE_TIME;

    private WorkflowCommandBuilder(UUID organizationId, UUID candidateId, UUID actorId) {
      this.organizationId = organizationId;
      this.candidateId = candidateId;
      this.actorId = actorId;
    }

    private WorkflowCommandBuilder action(String action) {
      this.action = action;
      return this;
    }

    private WorkflowCommandBuilder beforeState(String beforeState) {
      this.beforeState = beforeState;
      return this;
    }

    private WorkflowCommandBuilder afterState(String afterState) {
      this.afterState = afterState;
      return this;
    }

    private WorkflowCommandBuilder idempotencyKey(String idempotencyKey) {
      this.idempotencyKey = idempotencyKey;
      return this;
    }

    private WorkflowCommandBuilder correlationId(UUID correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    private WorkflowCommandBuilder causationId(UUID causationId) {
      this.causationId = causationId;
      return this;
    }

    private WorkflowCommandBuilder sourceRefId(UUID sourceRefId) {
      this.sourceRefId = sourceRefId;
      return this;
    }

    private WorkflowCommandBuilder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    private WorkflowEventAppendCommand build() {
      return new WorkflowEventAppendCommand(
          organizationId,
          "recruiting",
          new EntityRef("CANDIDATE", candidateId),
          7,
          action,
          new WorkflowStateSnapshot(beforeState),
          new WorkflowStateSnapshot(afterState),
          new ActorRef(actorId, ActorRole.CONSULTANT),
          "domain_service",
          sourceRefId,
          null,
          null,
          "consultant published anonymous shortlist after review",
          new WorkflowIdempotencyKey(idempotencyKey),
          correlationId == null ? null : new WorkflowCorrelationId(correlationId),
          causationId == null ? null : new WorkflowCausationId(causationId),
          occurredAt);
    }
  }
}
