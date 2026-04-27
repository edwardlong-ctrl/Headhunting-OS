package com.recruitingtransactionos.coreapi.truthlayer.persistence;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcWorkflowEventPort implements WorkflowEventPort {

  private static final String INSERT_SQL = """
      INSERT INTO workflow.workflow_event (
        workflow_event_id,
        organization_id,
        entity_namespace,
        entity_type,
        entity_id,
        entity_version,
        action,
        before_state,
        after_state,
        actor_user_id,
        actor_role,
        source_type,
        source_ref_id,
        ai_task_run_id,
        review_event_id,
        reason,
        idempotency_key,
        correlation_id,
        causation_id,
        occurred_at
      )
      VALUES (
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?::jsonb,
        ?::jsonb,
        ?,
        ?::governance.actor_role,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?
      )
      """;

  private static final String FIND_BY_IDEMPOTENCY_KEY_SQL = """
      SELECT
        workflow_event_id,
        organization_id,
        entity_namespace,
        entity_type,
        entity_id,
        entity_version,
        action,
        before_state::text AS before_state,
        after_state::text AS after_state,
        actor_user_id,
        actor_role::text AS actor_role,
        source_type,
        source_ref_id,
        ai_task_run_id,
        review_event_id,
        reason,
        idempotency_key,
        correlation_id,
        causation_id,
        occurred_at
      FROM workflow.workflow_event
      WHERE organization_id = ?
        AND idempotency_key = ?
      """;

  private final DataSource dataSource;

  public JdbcWorkflowEventPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
      UUID organizationId,
      WorkflowIdempotencyKey idempotencyKey) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_IDEMPOTENCY_KEY_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, idempotencyKey.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toIdempotencyRecord(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find workflow event by idempotency key",
          exception);
    }
  }

  @Override
  public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    WorkflowEventId workflowEventId = new WorkflowEventId(UUID.randomUUID());

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      bind(statement, workflowEventId, command);
      statement.executeUpdate();
      return new WorkflowEventAppendResult(workflowEventId);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append workflow event", exception);
    }
  }

  private static void bind(
      PreparedStatement statement,
      WorkflowEventId workflowEventId,
      WorkflowEventAppendCommand command) throws SQLException {
    statement.setObject(1, workflowEventId.value());
    statement.setObject(2, command.organizationId());
    statement.setString(3, command.entityNamespace());
    statement.setString(4, command.entity().entityType());
    statement.setObject(5, command.entity().entityId());
    setNullableInteger(statement, 6, command.entityVersion());
    statement.setString(7, command.action());
    statement.setString(8, command.beforeState().json());
    statement.setString(9, command.afterState().json());
    statement.setObject(10, command.actor().userId());
    statement.setString(11, command.actor().role().wireValue());
    statement.setString(12, command.sourceType());
    setNullableUuid(statement, 13, command.sourceRefId());
    setNullableUuid(statement, 14, aiTaskRunUuid(command.aiTaskRunId()));
    setNullableUuid(statement, 15, reviewEventUuid(command.reviewEventId()));
    statement.setString(16, command.reason());
    setNullableString(statement, 17, idempotencyKey(command.idempotencyKey()));
    setNullableUuid(statement, 18, correlationUuid(command.correlationId()));
    setNullableUuid(statement, 19, causationUuid(command.causationId()));
    statement.setObject(20, OffsetDateTime.ofInstant(command.occurredAt(), ZoneOffset.UTC));
  }

  private static WorkflowEventIdempotencyRecord toIdempotencyRecord(ResultSet resultSet)
      throws SQLException {
    WorkflowEventId workflowEventId =
        new WorkflowEventId(resultSet.getObject("workflow_event_id", UUID.class));
    WorkflowEventAppendCommand command = new WorkflowEventAppendCommand(
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("entity_namespace"),
        new EntityRef(
            resultSet.getString("entity_type"),
            resultSet.getObject("entity_id", UUID.class)),
        resultSet.getObject("entity_version", Integer.class),
        resultSet.getString("action"),
        new WorkflowStateSnapshot(resultSet.getString("before_state")),
        new WorkflowStateSnapshot(resultSet.getString("after_state")),
        new ActorRef(
            resultSet.getObject("actor_user_id", UUID.class),
            actorRole(resultSet.getString("actor_role"))),
        resultSet.getString("source_type"),
        resultSet.getObject("source_ref_id", UUID.class),
        aiTaskRunId(resultSet.getObject("ai_task_run_id", UUID.class)),
        reviewEventId(resultSet.getObject("review_event_id", UUID.class)),
        resultSet.getString("reason"),
        idempotencyKey(resultSet.getString("idempotency_key")),
        correlationId(resultSet.getObject("correlation_id", UUID.class)),
        causationId(resultSet.getObject("causation_id", UUID.class)),
        resultSet.getObject("occurred_at", OffsetDateTime.class).toInstant());
    return new WorkflowEventIdempotencyRecord(workflowEventId, command);
  }

  private static UUID aiTaskRunUuid(AITaskRunId aiTaskRunId) {
    if (aiTaskRunId == null) {
      return null;
    }
    return aiTaskRunId.value();
  }

  private static UUID reviewEventUuid(ReviewEventId reviewEventId) {
    if (reviewEventId == null) {
      return null;
    }
    return reviewEventId.value();
  }

  private static String idempotencyKey(WorkflowIdempotencyKey idempotencyKey) {
    if (idempotencyKey == null) {
      return null;
    }
    return idempotencyKey.value();
  }

  private static WorkflowIdempotencyKey idempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null) {
      return null;
    }
    return new WorkflowIdempotencyKey(idempotencyKey);
  }

  private static UUID correlationUuid(WorkflowCorrelationId correlationId) {
    if (correlationId == null) {
      return null;
    }
    return correlationId.value();
  }

  private static WorkflowCorrelationId correlationId(UUID correlationId) {
    if (correlationId == null) {
      return null;
    }
    return new WorkflowCorrelationId(correlationId);
  }

  private static UUID causationUuid(WorkflowCausationId causationId) {
    if (causationId == null) {
      return null;
    }
    return causationId.value();
  }

  private static WorkflowCausationId causationId(UUID causationId) {
    if (causationId == null) {
      return null;
    }
    return new WorkflowCausationId(causationId);
  }

  private static AITaskRunId aiTaskRunId(UUID value) {
    if (value == null) {
      return null;
    }
    return new AITaskRunId(value);
  }

  private static ReviewEventId reviewEventId(UUID value) {
    if (value == null) {
      return null;
    }
    return new ReviewEventId(value);
  }

  private static ActorRole actorRole(String wireValue) {
    for (ActorRole role : ActorRole.values()) {
      if (role.wireValue().equals(wireValue)) {
        return role;
      }
    }
    throw new IllegalArgumentException("unknown actor role: " + wireValue);
  }

  private static void setNullableInteger(
      PreparedStatement statement,
      int index,
      Integer value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.INTEGER);
      return;
    }
    statement.setInt(index, value);
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
