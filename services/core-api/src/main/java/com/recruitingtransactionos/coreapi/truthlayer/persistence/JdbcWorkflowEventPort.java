package com.recruitingtransactionos.coreapi.truthlayer.persistence;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
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
        previous_event_id,
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

  private final DataSource dataSource;

  public JdbcWorkflowEventPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
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
    setNullableString(statement, 17, command.idempotencyKey());
    setNullableUuid(statement, 18, command.correlationId());
    setNullableUuid(statement, 19, workflowEventUuid(command.previousEventId()));
    statement.setObject(20, OffsetDateTime.ofInstant(command.occurredAt(), ZoneOffset.UTC));
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

  private static UUID workflowEventUuid(WorkflowEventId workflowEventId) {
    if (workflowEventId == null) {
      return null;
    }
    return workflowEventId.value();
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
