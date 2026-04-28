package com.recruitingtransactionos.coreapi.truthlayer.persistence;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcAITaskRunPort implements AITaskRunPort {

  private static final String INSERT_DEFINITION_SQL = """
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
      VALUES (?, ?, ?, ?, 'active', ?, ?, '{}'::jsonb, ?)
      ON CONFLICT (organization_id, task_key, task_version) DO NOTHING
      """;

  private static final String FIND_DEFINITION_SQL = """
      SELECT ai_task_definition_id
      FROM governance.ai_task_definition
      WHERE organization_id = ?
        AND task_key = ?
        AND task_version = ?
      """;

  private static final String INSERT_RUN_SQL = """
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
        model_version,
        source_ref_ids,
        target_entity_type,
        target_entity_id,
        write_back_target,
        human_review_status,
        started_at,
        completed_at,
        failure_reason,
        requested_by_user_id,
        requested_by_role,
        correlation_id,
        causation_id
      )
      VALUES (
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?::governance.actor_role,
        ?,
        ?
      )
      """;

  private static final String FIND_RUN_SQL = """
      SELECT
        run.ai_task_run_id,
        run.organization_id,
        definition.task_key,
        run.task_version,
        run.input_schema_version,
        run.output_schema_version,
        run.prompt_version,
        run.model_provider,
        run.model_name,
        run.model_version,
        run.status,
        run.human_review_status,
        run.write_back_target,
        run.requested_by_user_id,
        run.requested_by_role::text AS requested_by_role,
        run.correlation_id,
        run.causation_id,
        run.target_entity_type,
        run.target_entity_id,
        run.source_ref_ids,
        run.started_at,
        run.completed_at,
        run.failure_reason,
        run.created_at
      FROM governance.ai_task_run run
      JOIN governance.ai_task_definition definition
        ON definition.ai_task_definition_id = run.ai_task_definition_id
      WHERE run.organization_id = ?
        AND run.ai_task_run_id = ?
      """;

  private final DataSource dataSource;

  public JdbcAITaskRunPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public AITaskRunAppendResult append(AITaskRunAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    AITaskRunId aiTaskRunId = new AITaskRunId(UUID.randomUUID());

    try (Connection connection = dataSource.getConnection()) {
      boolean previousAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        insertTaskDefinitionIfAbsent(connection, command);
        UUID definitionId = findTaskDefinitionId(connection, command);
        insertRun(connection, aiTaskRunId, definitionId, command);
        connection.commit();
        return new AITaskRunAppendResult(aiTaskRunId);
      } catch (SQLException | RuntimeException exception) {
        connection.rollback();
        throw exception;
      } finally {
        connection.setAutoCommit(previousAutoCommit);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append AI task run", exception);
    }
  }

  @Override
  public Optional<AITaskRunRecord> findById(UUID organizationId, AITaskRunId aiTaskRunId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(aiTaskRunId, "aiTaskRunId must not be null");

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_RUN_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, aiTaskRunId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toRecord(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find AI task run", exception);
    }
  }

  private static void insertTaskDefinitionIfAbsent(
      Connection connection,
      AITaskRunAppendCommand command) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(INSERT_DEFINITION_SQL)) {
      statement.setObject(1, deterministicDefinitionId(command));
      statement.setObject(2, command.organizationId());
      statement.setString(3, command.taskName());
      statement.setString(4, command.taskVersion());
      statement.setString(5, command.inputSchemaVersion());
      statement.setString(6, command.outputSchemaVersion());
      setNullableString(statement, 7, writeBackTargetValue(command.writeBackTarget()));
      statement.executeUpdate();
    }
  }

  private static UUID findTaskDefinitionId(
      Connection connection,
      AITaskRunAppendCommand command) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(FIND_DEFINITION_SQL)) {
      statement.setObject(1, command.organizationId());
      statement.setString(2, command.taskName());
      statement.setString(3, command.taskVersion());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new IllegalStateException("AI task definition was not created");
        }
        return resultSet.getObject("ai_task_definition_id", UUID.class);
      }
    }
  }

  private static void insertRun(
      Connection connection,
      AITaskRunId aiTaskRunId,
      UUID definitionId,
      AITaskRunAppendCommand command) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(INSERT_RUN_SQL)) {
      statement.setObject(1, aiTaskRunId.value());
      statement.setObject(2, command.organizationId());
      statement.setObject(3, definitionId);
      statement.setString(4, command.taskVersion());
      statement.setString(5, command.status().wireValue());
      statement.setString(6, command.inputSchemaVersion());
      statement.setString(7, command.outputSchemaVersion());
      statement.setString(8, command.promptVersion());
      statement.setString(9, command.model().provider());
      statement.setString(10, command.model().name());
      setNullableString(statement, 11, command.model().version());
      statement.setArray(12, uuidArray(connection, command.sourceReferenceIds().toArray(UUID[]::new)));
      statement.setString(13, command.targetEntity().entityType());
      statement.setObject(14, command.targetEntity().entityId());
      setNullableString(statement, 15, writeBackTargetValue(command.writeBackTarget()));
      statement.setString(16, humanReviewStatus(command.humanReviewStatus()));
      statement.setObject(17, OffsetDateTime.ofInstant(command.startedAt(), ZoneOffset.UTC));
      setNullableInstant(statement, 18, command.completedAt());
      setNullableString(statement, 19, command.failureReason());
      setNullableUuid(statement, 20, requestedByUserId(command.requestedBy()));
      setNullableString(statement, 21, requestedByRole(command.requestedBy()));
      setNullableUuid(statement, 22, correlationUuid(command.correlationId()));
      setNullableUuid(statement, 23, causationUuid(command.causationId()));
      statement.executeUpdate();
    }
  }

  private static AITaskRunRecord toRecord(ResultSet resultSet) throws SQLException {
    return new AITaskRunRecord(
        new AITaskRunId(resultSet.getObject("ai_task_run_id", UUID.class)),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("task_key"),
        resultSet.getString("task_version"),
        resultSet.getString("input_schema_version"),
        resultSet.getString("output_schema_version"),
        resultSet.getString("prompt_version"),
        new ModelRef(
            resultSet.getString("model_provider"),
            resultSet.getString("model_name"),
            resultSet.getString("model_version")),
        AITaskRunStatus.fromWireValue(resultSet.getString("status")),
        resultSet.getString("human_review_status"),
        writeBackTarget(resultSet.getString("write_back_target")),
        requestedBy(
            resultSet.getObject("requested_by_user_id", UUID.class),
            resultSet.getString("requested_by_role")),
        correlationId(resultSet.getObject("correlation_id", UUID.class)),
        causationId(resultSet.getObject("causation_id", UUID.class)),
        new EntityRef(
            resultSet.getString("target_entity_type"),
            resultSet.getObject("target_entity_id", UUID.class)),
        sourceReferenceIds(resultSet.getArray("source_ref_ids")),
        resultSet.getObject("started_at", OffsetDateTime.class).toInstant(),
        nullableInstant(resultSet, "completed_at"),
        resultSet.getString("failure_reason"),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private static UUID deterministicDefinitionId(AITaskRunAppendCommand command) {
    String source = command.organizationId() + ":" + command.taskName() + ":" + command.taskVersion();
    return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
  }

  private static String humanReviewStatus(String value) {
    if (value == null) {
      return "not_required";
    }
    return value;
  }

  private static String writeBackTargetValue(WriteBackTarget writeBackTarget) {
    if (writeBackTarget == null) {
      return null;
    }
    return writeBackTarget.value();
  }

  private static WriteBackTarget writeBackTarget(String value) {
    if (value == null) {
      return null;
    }
    return new WriteBackTarget(value);
  }

  private static UUID requestedByUserId(ActorRef requestedBy) {
    if (requestedBy == null) {
      return null;
    }
    return requestedBy.userId();
  }

  private static String requestedByRole(ActorRef requestedBy) {
    if (requestedBy == null) {
      return null;
    }
    return requestedBy.role().wireValue();
  }

  private static ActorRef requestedBy(UUID userId, String role) {
    if (userId == null && role == null) {
      return null;
    }
    if (userId == null || role == null) {
      throw new IllegalArgumentException("requestedBy must include both user id and role");
    }
    return new ActorRef(userId, ActorRole.fromWireValue(role));
  }

  private static UUID correlationUuid(WorkflowCorrelationId correlationId) {
    if (correlationId == null) {
      return null;
    }
    return correlationId.value();
  }

  private static WorkflowCorrelationId correlationId(UUID value) {
    if (value == null) {
      return null;
    }
    return new WorkflowCorrelationId(value);
  }

  private static UUID causationUuid(WorkflowCausationId causationId) {
    if (causationId == null) {
      return null;
    }
    return causationId.value();
  }

  private static WorkflowCausationId causationId(UUID value) {
    if (value == null) {
      return null;
    }
    return new WorkflowCausationId(value);
  }

  private static java.util.List<UUID> sourceReferenceIds(Array sourceRefIds) throws SQLException {
    if (sourceRefIds == null) {
      return java.util.List.of();
    }
    Object raw = sourceRefIds.getArray();
    if (raw instanceof UUID[] values) {
      return java.util.List.copyOf(Arrays.asList(values));
    }
    if (raw instanceof Object[] values) {
      return Arrays.stream(values)
          .map(value -> value instanceof UUID uuid ? uuid : UUID.fromString(value.toString()))
          .toList();
    }
    return java.util.List.of();
  }

  private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    if (value == null) {
      return null;
    }
    return value.toInstant();
  }

  private static Array uuidArray(Connection connection, UUID[] values) throws SQLException {
    return connection.createArrayOf("uuid", values);
  }

  private static void setNullableInstant(
      PreparedStatement statement,
      int index,
      java.time.Instant value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
      return;
    }
    statement.setObject(index, OffsetDateTime.ofInstant(value, ZoneOffset.UTC));
  }

  private static void setNullableUuid(
      PreparedStatement statement,
      int index,
      UUID value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.OTHER);
      return;
    }
    statement.setObject(index, value);
  }

  private static void setNullableString(
      PreparedStatement statement,
      int index,
      String value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.VARCHAR);
      return;
    }
    statement.setString(index, value);
  }
}
