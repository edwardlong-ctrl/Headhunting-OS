package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcAITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class AITaskRunPostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Instant STARTED_AT = Instant.parse("2026-04-28T07:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;

  @BeforeAll
  static void migrate() {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
  }

  @Test
  void appendAndReadBackPreservesTaskModelPromptAndSchemaVersions() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000110001");
    UUID requestedBy = uuid("00000000-0000-0000-0000-000000110002");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000110003");
    UUID sourceRefId = uuid("00000000-0000-0000-0000-000000110004");
    insertOrganizationAndUser(organizationId, requestedBy);

    AITaskRunAppendCommand command = command(
        organizationId,
        requestedBy,
        candidateId,
        List.of(sourceRefId),
        AITaskRunStatus.SUCCEEDED,
        new WriteBackTarget(AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL.wireValue()),
        AITaskHumanReviewStatus.REQUIRED,
        STARTED_AT.plusSeconds(12),
        null);

    AITaskRunAppendResult result = service().append(command);

    Optional<AITaskRunRecord> readBack = service().findById(organizationId, result.aiTaskRunId());
    assertThat(readBack).isPresent();
    AITaskRunRecord persisted = readBack.orElseThrow();
    assertThat(persisted.aiTaskRunId()).isEqualTo(result.aiTaskRunId());
    assertThat(persisted.organizationId()).isEqualTo(organizationId);
    assertThat(persisted.taskName()).isEqualTo("candidate-profile-extraction");
    assertThat(persisted.taskVersion()).isEqualTo("candidate-profile-extraction.v1");
    assertThat(persisted.inputSchemaVersion()).isEqualTo("candidate-profile-input.v1");
    assertThat(persisted.outputSchemaVersion()).isEqualTo("candidate-profile-output.v1");
    assertThat(persisted.promptVersion()).isEqualTo("prompt.candidate-profile-extraction.v1");
    assertThat(persisted.model()).isEqualTo(new ModelRef("metadata-only", "no-model-call", "v0"));
    assertThat(persisted.status()).isEqualTo(AITaskRunStatus.SUCCEEDED);
    assertThat(persisted.requestedBy()).isEqualTo(new ActorRef(requestedBy, ActorRole.CONSULTANT));
    assertThat(persisted.correlationId())
        .isEqualTo(new WorkflowCorrelationId(uuid("00000000-0000-0000-0000-000000110005")));
    assertThat(persisted.causationId())
        .isEqualTo(new WorkflowCausationId(uuid("00000000-0000-0000-0000-000000110006")));
    assertThat(persisted.targetEntity()).isEqualTo(new EntityRef("CANDIDATE", candidateId));
    assertThat(persisted.sourceReferenceIds()).containsExactly(sourceRefId);
    assertThat(persisted.writeBackTarget())
        .isEqualTo(new WriteBackTarget("claim_ledger_proposal"));
    assertThat(persisted.humanReviewStatus()).isEqualTo("required");
    assertThat(persisted.startedAt()).isEqualTo(STARTED_AT);
    assertThat(persisted.completedAt()).isEqualTo(STARTED_AT.plusSeconds(12));
    assertThat(persisted.failureReason()).isNull();
    assertThat(persisted.createdAt()).isNotNull();
  }

  @Test
  void appendFailedRunPreservesSafeFailureReasonWithoutCreatingFacts() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000110101");
    UUID requestedBy = uuid("00000000-0000-0000-0000-000000110102");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000110103");
    insertOrganizationAndUser(organizationId, requestedBy);

    AITaskRunAppendResult result = service().append(command(
        organizationId,
        requestedBy,
        candidateId,
        List.of(),
        AITaskRunStatus.FAILED,
        null,
        AITaskHumanReviewStatus.NOT_REQUIRED,
        STARTED_AT.plusSeconds(3),
        "provider_timeout_without_output"));

    AITaskRunRecord persisted = service().findById(organizationId, result.aiTaskRunId())
        .orElseThrow();
    assertThat(persisted.status()).isEqualTo(AITaskRunStatus.FAILED);
    assertThat(persisted.failureReason()).isEqualTo("provider_timeout_without_output");
    assertThat(persisted.writeBackTarget()).isNull();

    assertThat(countRows("governance.ai_task_run", organizationId)).isEqualTo(1);
    assertThat(countRows("recruiting.candidate", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
    assertThat(countRows("governance.claim_ledger_item", organizationId)).isZero();
    assertThat(countRows("governance.review_event", organizationId)).isZero();
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
    assertThat(countRows("audit.audit_log", organizationId)).isZero();
  }

  @Test
  void appendDoesNotRequireOrEnforceWriteBackTarget() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000110201");
    UUID requestedBy = uuid("00000000-0000-0000-0000-000000110202");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000110203");
    insertOrganizationAndUser(organizationId, requestedBy);

    AITaskRunAppendResult result = service().append(command(
        organizationId,
        requestedBy,
        candidateId,
        List.of(),
        AITaskRunStatus.CREATED,
        null,
        AITaskHumanReviewStatus.NOT_REQUIRED,
        null,
        null));

    assertThat(service().findById(organizationId, result.aiTaskRunId()).orElseThrow()
        .writeBackTarget())
        .isNull();
    assertThat(countRows("governance.ai_task_run", organizationId)).isEqualTo(1);
    assertThat(countRows("governance.claim_ledger_item", organizationId)).isZero();
    assertThat(countRows("governance.review_event", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
  }

  @Test
  void appendCanonicalTargetMetadataDoesNotExecuteWriteBackOrCreateGovernanceSideEffects()
      throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000110301");
    UUID requestedBy = uuid("00000000-0000-0000-0000-000000110302");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000110303");
    insertOrganizationAndUser(organizationId, requestedBy);

    AITaskRunAppendResult result = service().append(command(
        organizationId,
        requestedBy,
        candidateId,
        List.of(uuid("00000000-0000-0000-0000-000000110304")),
        AITaskRunStatus.SUCCEEDED,
        new WriteBackTarget(AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE.wireValue()),
        AITaskHumanReviewStatus.APPROVED,
        STARTED_AT.plusSeconds(20),
        null));

    AITaskRunRecord persisted = service().findById(organizationId, result.aiTaskRunId())
        .orElseThrow();
    assertThat(persisted.writeBackTarget())
        .isEqualTo(new WriteBackTarget("canonical_candidate_profile"));
    assertThat(persisted.humanReviewStatus()).isEqualTo("approved");
    assertThat(persisted.model()).isEqualTo(new ModelRef("metadata-only", "no-model-call", "v0"));

    assertThat(countRows("governance.ai_task_run", organizationId)).isEqualTo(1);
    assertThat(countRows("governance.claim_ledger_item", organizationId)).isZero();
    assertThat(countRows("governance.review_event", organizationId)).isZero();
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
    assertThat(countRows("audit.audit_log", organizationId)).isZero();
  }

  @Test
  void migrationAddsOnlyAuditMetadataColumnsAndKeepsAiGovernanceApiOutOfScope()
      throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(15);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");

    assertThat(columnExists("governance", "ai_task_run", "requested_by_user_id")).isTrue();
    assertThat(columnExists("governance", "ai_task_run", "requested_by_role")).isTrue();
    assertThat(columnExists("governance", "ai_task_run", "correlation_id")).isTrue();
    assertThat(columnExists("governance", "ai_task_run", "causation_id")).isTrue();
    assertThat(columnExists("governance", "ai_task_run", "failure_reason")).isTrue();
  }

  private static AITaskRunService service() {
    return new AITaskRunService(new JdbcAITaskRunPort(dataSource));
  }

  private static AITaskRunAppendCommand command(
      UUID organizationId,
      UUID requestedBy,
      UUID candidateId,
      List<UUID> sourceReferenceIds,
      AITaskRunStatus status,
      WriteBackTarget writeBackTarget,
      AITaskHumanReviewStatus humanReviewStatus,
      Instant completedAt,
      String failureReason) {
    return new AITaskRunAppendCommand(
        organizationId,
        "candidate-profile-extraction",
        "candidate-profile-extraction.v1",
        "candidate-profile-input.v1",
        "candidate-profile-output.v1",
        "prompt.candidate-profile-extraction.v1",
        new ModelRef("metadata-only", "no-model-call", "v0"),
        status,
        humanReviewStatus.wireValue(),
        writeBackTarget,
        new ActorRef(requestedBy, ActorRole.CONSULTANT),
        new WorkflowCorrelationId(uuid("00000000-0000-0000-0000-000000110005")),
        new WorkflowCausationId(uuid("00000000-0000-0000-0000-000000110006")),
        new EntityRef("CANDIDATE", candidateId),
        sourceReferenceIds,
        STARTED_AT,
        completedAt,
        failureReason);
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
      organization.setString(2, "Task 10A Org " + organizationId);
      organization.setString(3, "Task 10A Org");
      organization.executeUpdate();

      user.setObject(1, userId);
      user.setObject(2, organizationId);
      user.setString(3, "task10a-requester-" + userId + "@example.test");
      user.setString(4, "Task 10A Requester");
      user.executeUpdate();
    }
  }

  private static int countRows(String tableName, UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM " + tableName + " WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static List<String> appliedMigrationVersions() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank");
        ResultSet resultSet = statement.executeQuery()) {
      List<String> versions = new java.util.ArrayList<>();
      while (resultSet.next()) {
        versions.add(resultSet.getString("version"));
      }
      return versions;
    }
  }

  private static boolean columnExists(String schema, String table, String column)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS ("
                + "SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? AND column_name = ?)")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      statement.setString(3, column);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
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
