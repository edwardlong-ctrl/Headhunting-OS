package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcObservabilityAITaskRunReader implements ObservabilityAITaskRunReader {

  private static final String SELECT_COLUMNS = """
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
        '{}'::text AS input_payload,
        NULL::text AS output_payload,
        NULL::text AS tool_calls,
        run.cost_units,
        run.trace_ref,
        run.error_code,
        '{}'::text AS metadata,
        run.replayed_from_ai_task_run_id,
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
      """;

  private final DataSource dataSource;

  public JdbcObservabilityAITaskRunReader(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public List<AITaskRunRecord> search(ObservabilityAITaskRunQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    SearchSql searchSql = searchSql(query);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(searchSql.sql())) {
      for (int index = 0; index < searchSql.binders().size(); index++) {
        searchSql.binders().get(index).bind(statement, index + 1);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        List<AITaskRunRecord> records = new ArrayList<>();
        while (resultSet.next()) {
          records.add(map(resultSet));
        }
        return List.copyOf(records);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to search AI task runs", exception);
    }
  }

  private static SearchSql searchSql(ObservabilityAITaskRunQuery query) {
    StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
    List<String> predicates = new ArrayList<>();
    List<SqlBinder> binders = new ArrayList<>();
    predicates.add("run.organization_id = ?");
    binders.add((statement, index) -> statement.setObject(index, query.organizationId()));
    if (query.taskName() != null) {
      predicates.add("definition.task_key = ?");
      binders.add((statement, index) -> statement.setString(index, query.taskName()));
    }
    if (query.status() != null) {
      predicates.add("run.status = ?");
      binders.add((statement, index) -> statement.setString(index, query.status()));
    }
    if (query.targetEntityType() != null) {
      predicates.add("run.target_entity_type = ?");
      binders.add((statement, index) -> statement.setString(index, query.targetEntityType()));
    }
    if (query.targetEntityId() != null) {
      predicates.add("run.target_entity_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.targetEntityId()));
    }
    if (query.correlationId() != null) {
      predicates.add("run.correlation_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.correlationId()));
    }
    if (query.causationId() != null) {
      predicates.add("run.causation_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.causationId()));
    }
    if (query.startedFrom() != null) {
      predicates.add("run.started_at >= ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          OffsetDateTime.ofInstant(query.startedFrom(), ZoneOffset.UTC)));
    }
    if (query.startedTo() != null) {
      predicates.add("run.started_at <= ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          OffsetDateTime.ofInstant(query.startedTo(), ZoneOffset.UTC)));
    }
    sql.append("WHERE ")
        .append(String.join(" AND ", predicates))
        .append(" ORDER BY run.started_at DESC, run.ai_task_run_id DESC LIMIT ? OFFSET ?");
    binders.add((statement, index) -> statement.setInt(index, query.limit()));
    binders.add((statement, index) -> statement.setInt(index, query.offset()));
    return new SearchSql(sql.toString(), binders);
  }

  private static AITaskRunRecord map(ResultSet resultSet) throws SQLException {
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
        resultSet.getString("input_payload"),
        resultSet.getString("output_payload"),
        resultSet.getString("tool_calls"),
        resultSet.getBigDecimal("cost_units"),
        resultSet.getString("trace_ref"),
        resultSet.getString("error_code"),
        resultSet.getString("metadata"),
        aiTaskRunId(resultSet.getObject("replayed_from_ai_task_run_id", UUID.class)),
        actorRef(resultSet.getObject("requested_by_user_id", UUID.class), resultSet.getString("requested_by_role")),
        correlationId(resultSet.getObject("correlation_id", UUID.class)),
        causationId(resultSet.getObject("causation_id", UUID.class)),
        new EntityRef(resultSet.getString("target_entity_type"), resultSet.getObject("target_entity_id", UUID.class)),
        sourceReferenceIds(resultSet.getArray("source_ref_ids")),
        resultSet.getObject("started_at", OffsetDateTime.class).toInstant(),
        nullableInstant(resultSet, "completed_at"),
        resultSet.getString("failure_reason"),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private static WriteBackTarget writeBackTarget(String value) {
    return value == null ? null : new WriteBackTarget(value);
  }

  private static AITaskRunId aiTaskRunId(UUID value) {
    return value == null ? null : new AITaskRunId(value);
  }

  private static ActorRef actorRef(UUID userId, String role) {
    if (userId == null || role == null) {
      return null;
    }
    return new ActorRef(userId, ActorRole.fromWireValue(role));
  }

  private static WorkflowCorrelationId correlationId(UUID value) {
    return value == null ? null : new WorkflowCorrelationId(value);
  }

  private static WorkflowCausationId causationId(UUID value) {
    return value == null ? null : new WorkflowCausationId(value);
  }

  private static java.time.Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private static List<UUID> sourceReferenceIds(Array sourceRefIds) throws SQLException {
    if (sourceRefIds == null) {
      return List.of();
    }
    Object raw = sourceRefIds.getArray();
    if (raw instanceof UUID[] values) {
      return List.copyOf(Arrays.asList(values));
    }
    if (raw instanceof Object[] values) {
      return Arrays.stream(values)
          .map(value -> value instanceof UUID uuid ? uuid : UUID.fromString(value.toString()))
          .toList();
    }
    return List.of();
  }

  private record SearchSql(String sql, List<SqlBinder> binders) {}

  @FunctionalInterface
  private interface SqlBinder {
    void bind(PreparedStatement statement, int index) throws SQLException;
  }
}
