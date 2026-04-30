package com.recruitingtransactionos.coreapi.truthlayer.persistence;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptId;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Array;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcCanonicalWriteAttemptReadPort implements CanonicalWriteAttemptReadPort {

  private static final String SELECT_COLUMNS = """
      SELECT
        cwa.canonical_write_attempt_id,
        cwa.organization_id,
        cwa.entity_type,
        cwa.entity_id,
        cwa.entity_version,
        cwa.target_field_path,
        cwa.proposed_value_ref,
        cwa.source_span_ref,
        cwa.claim_ledger_item_id,
        cwa.review_event_id,
        cwa.decision,
        cwa.reason_codes,
        cwa.actor_user_id,
        cwa.actor_role::text AS actor_role,
        cwa.ai_task_run_id,
        cwa.idempotency_key,
        cwa.correlation_id,
        cwa.causation_id,
        cwa.workflow_event_id,
        cwa.occurred_at,
        cwa.created_at
      FROM governance.canonical_write_attempt cwa
      """;

  private final DataSource dataSource;

  public JdbcCanonicalWriteAttemptReadPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public List<CanonicalWriteAttemptRecord> search(CanonicalWriteAttemptQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    SearchSql searchSql = searchSql(query);

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(searchSql.sql())) {
      bind(statement, searchSql.binders());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<CanonicalWriteAttemptRecord> records = new ArrayList<>();
        while (resultSet.next()) {
          records.add(toRecord(resultSet));
        }
        return List.copyOf(records);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to search canonical write attempt records", exception);
    }
  }

  private static SearchSql searchSql(CanonicalWriteAttemptQuery query) {
    StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
    List<String> predicates = new ArrayList<>();
    List<SqlBinder> binders = new ArrayList<>();

    predicates.add("cwa.organization_id = ?");
    binders.add((statement, index) -> statement.setObject(index, query.organizationId()));
    if (query.decision() != null) {
      predicates.add("cwa.decision = ?");
      binders.add((statement, index) -> statement.setString(index, query.decision()));
    }
    if (query.actorUserId() != null) {
      predicates.add("cwa.actor_user_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.actorUserId()));
    }
    if (query.entityType() != null) {
      predicates.add("cwa.entity_type = ?");
      binders.add((statement, index) -> statement.setString(index, query.entityType()));
    }
    if (query.entityId() != null) {
      predicates.add("cwa.entity_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.entityId()));
    }
    if (query.reasonCode() != null) {
      predicates.add("? = ANY(cwa.reason_codes)");
      binders.add((statement, index) -> statement.setString(index, query.reasonCode()));
    }
    if (query.idempotencyKey() != null) {
      predicates.add("cwa.idempotency_key = ?");
      binders.add((statement, index) -> statement.setString(
          index, query.idempotencyKey().value()));
    }
    if (query.occurredFrom() != null) {
      predicates.add("cwa.occurred_at >= ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          OffsetDateTime.ofInstant(query.occurredFrom(), ZoneOffset.UTC)));
    }
    if (query.occurredTo() != null) {
      predicates.add("cwa.occurred_at <= ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          OffsetDateTime.ofInstant(query.occurredTo(), ZoneOffset.UTC)));
    }

    sql.append("WHERE ")
        .append(String.join(" AND ", predicates))
        .append(" ORDER BY cwa.occurred_at DESC, cwa.canonical_write_attempt_id DESC")
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

  private static CanonicalWriteAttemptRecord toRecord(ResultSet resultSet) throws SQLException {
    return new CanonicalWriteAttemptRecord(
        new CanonicalWriteAttemptId(
            resultSet.getObject("canonical_write_attempt_id", UUID.class)),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("entity_type"),
        resultSet.getObject("entity_id", UUID.class),
        resultSet.getObject("entity_version", Integer.class),
        resultSet.getString("target_field_path"),
        resultSet.getString("proposed_value_ref"),
        resultSet.getString("source_span_ref"),
        claimId(resultSet.getObject("claim_ledger_item_id", UUID.class)),
        reviewEventId(resultSet.getObject("review_event_id", UUID.class)),
        resultSet.getString("decision"),
        reasonCodes(resultSet.getArray("reason_codes")),
        resultSet.getObject("actor_user_id", UUID.class),
        actorRole(resultSet.getString("actor_role")),
        aiTaskRunId(resultSet.getObject("ai_task_run_id", UUID.class)),
        idempotencyKey(resultSet.getString("idempotency_key")),
        correlationId(resultSet.getObject("correlation_id", UUID.class)),
        causationId(resultSet.getObject("causation_id", UUID.class)),
        workflowEventId(resultSet.getObject("workflow_event_id", UUID.class)),
        resultSet.getObject("occurred_at", OffsetDateTime.class).toInstant(),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private static ClaimId claimId(UUID value) {
    if (value == null) {
      return null;
    }
    return new ClaimId(value);
  }

  private static ReviewEventId reviewEventId(UUID value) {
    if (value == null) {
      return null;
    }
    return new ReviewEventId(value);
  }

  private static AITaskRunId aiTaskRunId(UUID value) {
    if (value == null) {
      return null;
    }
    return new AITaskRunId(value);
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

  private static List<String> reasonCodes(Array reasonCodesArray) throws SQLException {
    if (reasonCodesArray == null) {
      return List.of();
    }
    String[] values = (String[]) reasonCodesArray.getArray();
    return List.of(values);
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
