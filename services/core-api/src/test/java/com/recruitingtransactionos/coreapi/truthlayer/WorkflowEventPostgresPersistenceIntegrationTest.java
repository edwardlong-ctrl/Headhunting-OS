package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;
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
class WorkflowEventPostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Instant OCCURRED_AT = Instant.parse("2026-04-28T02:30:00Z");

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
  void appendWorkflowEventPersistsAuditVocabulary() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000060001");
    UUID actorId = uuid("00000000-0000-0000-0000-000000060002");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000060003");
    UUID sourceRefId = uuid("00000000-0000-0000-0000-000000060004");
    AITaskRunId aiTaskRunId =
        new AITaskRunId(uuid("00000000-0000-0000-0000-000000060005"));
    ReviewEventId reviewEventId =
        new ReviewEventId(uuid("00000000-0000-0000-0000-000000060006"));
    UUID correlationId = uuid("00000000-0000-0000-0000-000000060007");
    insertOrganizationAndUser(organizationId, actorId);
    insertAiTaskRun(organizationId, aiTaskRunId.value(), candidateId, sourceRefId);
    insertReviewEvent(organizationId, actorId, reviewEventId.value(), candidateId);

    WorkflowEventAppendCommand command = command(
        organizationId,
        actorId,
        candidateId,
        "CANDIDATE_SHORTLISTED",
        "{\"status\":\"consultant_review\"}",
        "{\"status\":\"client_review\"}",
        ActorRole.CONSULTANT,
        sourceRefId,
        aiTaskRunId,
        reviewEventId,
        correlationId);

    WorkflowEventAppendResult result = service().append(command);

    PersistedWorkflowEvent persisted = findWorkflowEvent(result.workflowEventId());
    assertThat(persisted.organizationId()).isEqualTo(organizationId);
    assertThat(persisted.entityNamespace()).isEqualTo("recruiting");
    assertThat(persisted.entityType()).isEqualTo("CANDIDATE");
    assertThat(persisted.entityId()).isEqualTo(candidateId);
    assertThat(persisted.entityVersion()).isEqualTo(7);
    assertThat(persisted.action()).isEqualTo("CANDIDATE_SHORTLISTED");
    assertThat(persisted.beforeStatus()).isEqualTo("consultant_review");
    assertThat(persisted.afterStatus()).isEqualTo("client_review");
    assertThat(persisted.actorUserId()).isEqualTo(actorId);
    assertThat(persisted.actorRole()).isEqualTo(ActorRole.CONSULTANT.wireValue());
    assertThat(persisted.sourceType()).isEqualTo("domain_service");
    assertThat(persisted.sourceRefId()).isEqualTo(sourceRefId);
    assertThat(persisted.aiTaskRunId()).isEqualTo(aiTaskRunId.value());
    assertThat(persisted.reviewEventId()).isEqualTo(reviewEventId.value());
    assertThat(persisted.reason())
        .isEqualTo("consultant published anonymous shortlist after review");
    assertThat(persisted.idempotencyKey()).isEqualTo("workflow-event-idempotency-key");
    assertThat(persisted.correlationId()).isEqualTo(correlationId);
    assertThat(persisted.previousEventId()).isNull();
    assertThat(persisted.occurredAt().toInstant()).isEqualTo(OCCURRED_AT);
    assertThat(persisted.createdAt()).isNotNull();
  }

  @Test
  void appendWorkflowEventDoesNotMutateCanonicalState() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000060101");
    UUID actorId = uuid("00000000-0000-0000-0000-000000060102");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000060103");
    insertOrganizationAndUser(organizationId, actorId);

    service().append(command(
        organizationId,
        actorId,
        candidateId,
        "CANDIDATE_CONSULTANT_REVIEW_STARTED",
        "{\"status\":\"profile_parsed\"}",
        "{\"status\":\"consultant_review\"}",
        ActorRole.CONSULTANT,
        null,
        null,
        null,
        null));

    assertThat(countRows("workflow.workflow_event", organizationId)).isEqualTo(1);
    assertThat(countRows("recruiting.candidate", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
    assertThat(countRows("governance.claim_ledger_item", organizationId)).isZero();
    assertThat(countRows("governance.review_event", organizationId)).isZero();
    assertThat(countRows("governance.ai_task_run", organizationId)).isZero();
    assertThat(countRows("audit.audit_log", organizationId)).isZero();
  }

  @Test
  void appendWorkflowEventDoesNotValidateTransitionLegality() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000060201");
    UUID actorId = uuid("00000000-0000-0000-0000-000000060202");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000060203");
    insertOrganizationAndUser(organizationId, actorId);

    WorkflowEventAppendResult result = service().append(command(
        organizationId,
        actorId,
        candidateId,
        "CANDIDATE_CONSULTANT_REVIEW_STARTED",
        "{\"status\":\"placed\"}",
        "{\"status\":\"unreviewed_surprise_state\"}",
        ActorRole.CONSULTANT,
        null,
        null,
        null,
        null));

    PersistedWorkflowEvent persisted = findWorkflowEvent(result.workflowEventId());
    assertThat(persisted.beforeStatus()).isEqualTo("placed");
    assertThat(persisted.afterStatus()).isEqualTo("unreviewed_surprise_state");
    assertThat(persisted.action()).isEqualTo("CANDIDATE_CONSULTANT_REVIEW_STARTED");
  }

  @Test
  void appendUsesExplicitValues() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000060301");
    UUID actorId = uuid("00000000-0000-0000-0000-000000060302");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000060303");
    insertOrganizationAndUser(organizationId, actorId);

    PersistedWorkflowEvent persisted = findWorkflowEvent(service().append(command(
        organizationId,
        actorId,
        candidateId,
        "CANDIDATE_PROFILE_PARSED",
        "{\"status\":\"ai_extracted\"}",
        "{\"status\":\"needs_human_review\"}",
        ActorRole.AI,
        uuid("00000000-0000-0000-0000-000000060304"),
        null,
        null,
        uuid("00000000-0000-0000-0000-000000060305")))
        .workflowEventId());

    assertThat(persisted.actorRole()).isEqualTo(ActorRole.AI.wireValue());
    assertThat(persisted.actorRole()).isNotEqualTo(ActorRole.AI.name());
    assertThat(persisted.entityNamespace()).isEqualTo("recruiting");
    assertThat(persisted.entityType()).isEqualTo("CANDIDATE");
    assertThat(persisted.action()).isEqualTo("CANDIDATE_PROFILE_PARSED");
    assertThat(persisted.sourceType()).isEqualTo("domain_service");
    assertThat(persisted.beforeStatus()).isEqualTo("ai_extracted");
    assertThat(persisted.afterStatus()).isEqualTo("needs_human_review");
  }

  @Test
  void appendIsInsertOnlyByContract() {
    assertThat(publicDeclaredMethodNames(WorkflowEventPort.class)).containsExactly("append");
    assertThat(publicDeclaredMethodNames(JdbcWorkflowEventPort.class)).containsExactly("append");
    assertThat(declaredMethodNames(WorkflowEventPort.class))
        .noneMatch(this::looksLikeMutableWriteShortcut);
    assertThat(declaredMethodNames(JdbcWorkflowEventPort.class))
        .noneMatch(this::looksLikeMutableWriteShortcut);
  }

  @Test
  void nullOrInvalidRequiredWorkflowDataIsRejected() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService workflowEventService = new WorkflowEventService(port);

    assertThatThrownBy(() -> workflowEventService.append(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
    assertThat(port.commands()).isEmpty();

    assertThatThrownBy(() -> new WorkflowEventAppendCommand(
        uuid("00000000-0000-0000-0000-000000060401"),
        "recruiting",
        new EntityRef("CANDIDATE", uuid("00000000-0000-0000-0000-000000060402")),
        1,
        "",
        new WorkflowStateSnapshot("{\"status\":\"before\"}"),
        new WorkflowStateSnapshot("{\"status\":\"after\"}"),
        new ActorRef(uuid("00000000-0000-0000-0000-000000060403"), ActorRole.CONSULTANT),
        "domain_service",
        null,
        null,
        null,
        "blank action should be rejected",
        null,
        null,
        null,
        OCCURRED_AT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("action must not be blank");
    assertThat(port.commands()).isEmpty();
  }

  @Test
  void fullFlywayMigrationStillAppliesBeforeWorkflowPersistenceTest() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(2);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2");
  }

  @Test
  void knownConsentDisclosureGapRemainsOutOfScope() throws SQLException {
    assertThat(tableExists("privacy", "consent_record")).isFalse();
    assertThat(tableExists("privacy", "disclosure_record")).isFalse();
  }

  private static WorkflowEventService service() {
    return new WorkflowEventService(new JdbcWorkflowEventPort(dataSource));
  }

  private static WorkflowEventAppendCommand command(
      UUID organizationId,
      UUID actorId,
      UUID candidateId,
      String action,
      String beforeState,
      String afterState,
      ActorRole actorRole,
      UUID sourceRefId,
      AITaskRunId aiTaskRunId,
      ReviewEventId reviewEventId,
      UUID correlationId) {
    return new WorkflowEventAppendCommand(
        organizationId,
        "recruiting",
        new EntityRef("CANDIDATE", candidateId),
        7,
        action,
        new WorkflowStateSnapshot(beforeState),
        new WorkflowStateSnapshot(afterState),
        new ActorRef(actorId, actorRole),
        "domain_service",
        sourceRefId,
        aiTaskRunId,
        reviewEventId,
        "consultant published anonymous shortlist after review",
        "workflow-event-idempotency-key",
        correlationId,
        null,
        OCCURRED_AT);
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
      organization.setString(2, "Task 3C Org " + organizationId);
      organization.setString(3, "Task 3C Org");
      organization.executeUpdate();

      user.setObject(1, userId);
      user.setObject(2, organizationId);
      user.setString(3, "workflow-actor-" + userId + "@example.test");
      user.setString(4, "Task 3C Workflow Actor");
      user.executeUpdate();
    }
  }

  private static void insertAiTaskRun(
      UUID organizationId,
      UUID aiTaskRunId,
      UUID candidateId,
      UUID sourceRefId) throws SQLException {
    UUID aiTaskDefinitionId = uuid("10000000-0000-0000-0000-" + aiTaskRunId.toString()
        .substring(24));
    try (Connection connection = connection();
        PreparedStatement definition = connection.prepareStatement("""
            INSERT INTO governance.ai_task_definition (
              ai_task_definition_id,
              organization_id,
              task_key,
              task_version,
              status,
              input_schema_version,
              output_schema_version,
              human_review_policy,
              write_back_target
            )
            VALUES (?, ?, ?, '1.0', 'active', 'workflow-input.v1', 'workflow-output.v1',
              '{}'::jsonb, 'workflow_event')
            """);
        PreparedStatement run = connection.prepareStatement("""
            INSERT INTO governance.ai_task_run (
              ai_task_run_id,
              organization_id,
              ai_task_definition_id,
              task_version,
              status,
              input_schema_version,
              output_schema_version,
              prompt_version,
              model_provider,
              model_name,
              source_ref_ids,
              target_entity_type,
              target_entity_id,
              write_back_target,
              human_review_status,
              started_at,
              completed_at
            )
            VALUES (?, ?, ?, '1.0', 'succeeded', 'workflow-input.v1', 'workflow-output.v1',
              'prompt.workflow-event.v1', 'test-provider', 'test-model',
              ARRAY[?]::uuid[], 'candidate', ?, 'workflow_event', 'reviewed', ?, ?)
            """)) {
      definition.setObject(1, aiTaskDefinitionId);
      definition.setObject(2, organizationId);
      definition.setString(3, "workflow-event-audit-" + aiTaskRunId);
      definition.executeUpdate();

      run.setObject(1, aiTaskRunId);
      run.setObject(2, organizationId);
      run.setObject(3, aiTaskDefinitionId);
      run.setObject(4, sourceRefId);
      run.setObject(5, candidateId);
      run.setObject(6, OffsetDateTime.ofInstant(OCCURRED_AT.minusSeconds(5), ZoneOffset.UTC));
      run.setObject(7, OffsetDateTime.ofInstant(OCCURRED_AT.minusSeconds(1), ZoneOffset.UTC));
      run.executeUpdate();
    }
  }

  private static void insertReviewEvent(
      UUID organizationId,
      UUID reviewerId,
      UUID reviewEventId,
      UUID candidateId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO governance.review_event (
              review_event_id,
              organization_id,
              reviewer_user_id,
              target_entity_type,
              target_entity_id,
              field_path,
              risk_tier,
              decision,
              bulk_flag,
              duration_ms,
              reason
            )
            VALUES (?, ?, ?, 'candidate', ?, 'shortlist', ?::governance.risk_tier,
              'approved', false, 900, 'review completed before workflow event')
            """)) {
      statement.setObject(1, reviewEventId);
      statement.setObject(2, organizationId);
      statement.setObject(3, reviewerId);
      statement.setObject(4, candidateId);
      statement.setString(5, RiskTier.T2_MEDIUM_RISK.wireValue());
      statement.executeUpdate();
    }
  }

  private static PersistedWorkflowEvent findWorkflowEvent(WorkflowEventId workflowEventId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT
              workflow_event_id,
              organization_id,
              entity_namespace,
              entity_type,
              entity_id,
              entity_version,
              action,
              before_state ->> 'status' AS before_status,
              after_state ->> 'status' AS after_status,
              actor_user_id,
              actor_role::text AS actor_role,
              source_type,
              source_ref_id,
              ai_task_run_id,
              review_event_id,
              reason,
              idempotency_key,
              correlation_id,
              previous_event_id,
              occurred_at,
              created_at
            FROM workflow.workflow_event
            WHERE workflow_event_id = ?
            """)) {
      statement.setObject(1, workflowEventId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new PersistedWorkflowEvent(
            resultSet.getObject("workflow_event_id", UUID.class),
            resultSet.getObject("organization_id", UUID.class),
            resultSet.getString("entity_namespace"),
            resultSet.getString("entity_type"),
            resultSet.getObject("entity_id", UUID.class),
            resultSet.getObject("entity_version", Integer.class),
            resultSet.getString("action"),
            resultSet.getString("before_status"),
            resultSet.getString("after_status"),
            resultSet.getObject("actor_user_id", UUID.class),
            resultSet.getString("actor_role"),
            resultSet.getString("source_type"),
            resultSet.getObject("source_ref_id", UUID.class),
            resultSet.getObject("ai_task_run_id", UUID.class),
            resultSet.getObject("review_event_id", UUID.class),
            resultSet.getString("reason"),
            resultSet.getString("idempotency_key"),
            resultSet.getObject("correlation_id", UUID.class),
            resultSet.getObject("previous_event_id", UUID.class),
            resultSet.getObject("occurred_at", OffsetDateTime.class),
            resultSet.getObject("created_at", OffsetDateTime.class));
      }
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

  private static boolean tableExists(String schema, String table) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS ("
                + "SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE')")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private boolean looksLikeMutableWriteShortcut(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("update")
        || normalized.contains("delete")
        || normalized.contains("upsert")
        || normalized.contains("savecanonical")
        || normalized.contains("savecandidateprofile")
        || normalized.contains("confirmfact")
        || normalized.contains("writecanonical")
        || normalized.contains("canonicalfact")
        || normalized.contains("mutatestate");
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> declaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
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

  private record PersistedWorkflowEvent(
      UUID workflowEventId,
      UUID organizationId,
      String entityNamespace,
      String entityType,
      UUID entityId,
      Integer entityVersion,
      String action,
      String beforeStatus,
      String afterStatus,
      UUID actorUserId,
      String actorRole,
      String sourceType,
      UUID sourceRefId,
      UUID aiTaskRunId,
      UUID reviewEventId,
      String reason,
      String idempotencyKey,
      UUID correlationId,
      UUID previousEventId,
      OffsetDateTime occurredAt,
      OffsetDateTime createdAt) {
  }

  private record RecordingWorkflowEventPort(List<WorkflowEventAppendCommand> commands)
      implements WorkflowEventPort {

    private RecordingWorkflowEventPort() {
      this(new java.util.ArrayList<>());
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return new WorkflowEventAppendResult(
          new WorkflowEventId(uuid("00000000-0000-0000-0000-000000060999")));
    }
  }
}
