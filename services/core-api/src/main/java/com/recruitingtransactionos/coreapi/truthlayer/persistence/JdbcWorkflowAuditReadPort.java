package com.recruitingtransactionos.coreapi.truthlayer.persistence;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionRegistry;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcWorkflowAuditReadPort implements WorkflowAuditReadPort {

  private static final String SELECT_COLUMNS = """
      SELECT
        we.workflow_event_id,
        we.organization_id,
        we.entity_namespace,
        we.entity_type,
        we.entity_id,
        we.action,
        we.before_state::text AS before_state,
        we.after_state::text AS after_state,
        we.actor_user_id,
        we.actor_role::text AS actor_role,
        we.source_type,
        we.source_ref_id,
        we.ai_task_run_id,
        we.reason,
        we.idempotency_key,
        we.correlation_id,
        we.causation_id,
        we.previous_event_id,
        we.occurred_at,
        we.created_at
      FROM workflow.workflow_event we
      """;

  private static final String FIND_BY_ID_SQL = SELECT_COLUMNS
      + "WHERE we.organization_id = ? AND we.workflow_event_id = ?";

  private final DataSource dataSource;
  private final WorkflowActionRegistry actionRegistry;

  public JdbcWorkflowAuditReadPort(DataSource dataSource) {
    this(dataSource, WorkflowActionRegistry.standard());
  }

  JdbcWorkflowAuditReadPort(
      DataSource dataSource,
      WorkflowActionRegistry actionRegistry) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.actionRegistry = Objects.requireNonNull(actionRegistry,
        "actionRegistry must not be null");
  }

  @Override
  public Optional<WorkflowAuditRecord> findById(
      UUID organizationId,
      WorkflowEventId workflowEventId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, workflowEventId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toRecord(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find workflow audit record by id", exception);
    }
  }

  @Override
  public List<WorkflowAuditRecord> search(WorkflowAuditQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    SearchSql searchSql = searchSql(query);

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(searchSql.sql())) {
      bind(statement, searchSql.binders());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<WorkflowAuditRecord> records = new ArrayList<>();
        while (resultSet.next()) {
          records.add(toRecord(resultSet));
        }
        return List.copyOf(records);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to search workflow audit records", exception);
    }
  }

  private static SearchSql searchSql(WorkflowAuditQuery query) {
    StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
    List<String> predicates = new ArrayList<>();
    List<SqlBinder> binders = new ArrayList<>();

    predicates.add("we.organization_id = ?");
    binders.add((statement, index) -> statement.setObject(index, query.organizationId()));
    if (query.workflowEventId() != null) {
      predicates.add("we.workflow_event_id = ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          query.workflowEventId().value()));
    }
    if (query.entityType() != null) {
      predicates.add("we.entity_type = ?");
      binders.add((statement, index) -> statement.setString(index, query.entityType()));
    }
    if (query.entityId() != null) {
      predicates.add("we.entity_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.entityId()));
    }
    if (query.actionCode() != null) {
      predicates.add("we.action = ?");
      binders.add((statement, index) -> statement.setString(index, query.actionCode()));
    }
    if (query.actorType() != null) {
      predicates.add("we.actor_role = ?::governance.actor_role");
      binders.add((statement, index) -> statement.setString(
          index,
          query.actorType().wireValue()));
    }
    if (query.actorId() != null) {
      predicates.add("we.actor_user_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.actorId()));
    }
    if (query.correlationId() != null) {
      predicates.add("we.correlation_id = ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          query.correlationId().value()));
    }
    if (query.causationId() != null) {
      predicates.add("we.causation_id = ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          query.causationId().value()));
    }
    if (query.idempotencyKey() != null) {
      predicates.add("we.idempotency_key = ?");
      binders.add((statement, index) -> statement.setString(
          index,
          query.idempotencyKey().value()));
    }
    if (query.occurredFrom() != null) {
      predicates.add("we.occurred_at >= ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          OffsetDateTime.ofInstant(query.occurredFrom(), ZoneOffset.UTC)));
    }
    if (query.occurredTo() != null) {
      predicates.add("we.occurred_at <= ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          OffsetDateTime.ofInstant(query.occurredTo(), ZoneOffset.UTC)));
    }

    sql.append("WHERE ")
        .append(String.join(" AND ", predicates))
        .append(" ORDER BY we.occurred_at DESC, we.workflow_event_id DESC")
        .append(" LIMIT ? OFFSET ?");
    binders.add((statement, index) -> statement.setInt(index, query.limit()));
    binders.add((statement, index) -> statement.setInt(index, query.offset()));
    return new SearchSql(sql.toString(), binders);
  }

  private static void bind(PreparedStatement statement, List<SqlBinder> binders)
      throws SQLException {
    for (int index = 0; index < binders.size(); index++) {
      binders.get(index).bind(statement, index + 1);
    }
  }

  private WorkflowAuditRecord toRecord(ResultSet resultSet) throws SQLException {
    String actionCode = resultSet.getString("action");
    ActorRole actorType = actorRole(resultSet.getString("actor_role"));
    UUID aiTaskRunId = resultSet.getObject("ai_task_run_id", UUID.class);
    return new WorkflowAuditRecord(
        new WorkflowEventId(resultSet.getObject("workflow_event_id", UUID.class)),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("entity_namespace"),
        resultSet.getString("entity_type"),
        resultSet.getObject("entity_id", UUID.class),
        actionCode,
        actorType,
        resultSet.getObject("actor_user_id", UUID.class),
        aiInvolvement(actorType, aiTaskRunId),
        riskTier(actionCode),
        new WorkflowStateSnapshot(resultSet.getString("before_state")),
        new WorkflowStateSnapshot(resultSet.getString("after_state")),
        resultSet.getString("reason"),
        idempotencyKey(resultSet.getString("idempotency_key")),
        correlationId(resultSet.getObject("correlation_id", UUID.class)),
        causationId(resultSet.getObject("causation_id", UUID.class)),
        workflowEventId(resultSet.getObject("previous_event_id", UUID.class)),
        resultSet.getString("source_type"),
        resultSet.getObject("source_ref_id", UUID.class),
        resultSet.getObject("occurred_at", OffsetDateTime.class).toInstant(),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private RiskTier riskTier(String actionCode) {
    return actionRegistry.policyFor(actionCode).riskTier();
  }

  private static WorkflowAiInvolvement aiInvolvement(ActorRole actorType, UUID aiTaskRunId) {
    if (actorType == ActorRole.AI) {
      return WorkflowAiInvolvement.AI_AUTOMATED_LOW_RISK;
    }
    if (aiTaskRunId != null) {
      return WorkflowAiInvolvement.AI_ASSISTED;
    }
    return WorkflowAiInvolvement.NONE;
  }

  private static WorkflowEventId workflowEventId(UUID value) {
    if (value == null) {
      return null;
    }
    return new WorkflowEventId(value);
  }

  private static WorkflowIdempotencyKey idempotencyKey(String value) {
    if (value == null) {
      return null;
    }
    return new WorkflowIdempotencyKey(value);
  }

  private static WorkflowCorrelationId correlationId(UUID value) {
    if (value == null) {
      return null;
    }
    return new WorkflowCorrelationId(value);
  }

  private static WorkflowCausationId causationId(UUID value) {
    if (value == null) {
      return null;
    }
    return new WorkflowCausationId(value);
  }

  private static ActorRole actorRole(String wireValue) {
    for (ActorRole role : ActorRole.values()) {
      if (role.wireValue().equals(wireValue)) {
        return role;
      }
    }
    throw new IllegalArgumentException("unknown actor role: " + wireValue);
  }

  @FunctionalInterface
  private interface SqlBinder {
    void bind(PreparedStatement statement, int index) throws SQLException;
  }

  private record SearchSql(String sql, List<SqlBinder> binders) {
  }
}
