package com.recruitingtransactionos.coreapi.accessaudit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcAccessAuditSearchReader implements AccessAuditSearchReader {

  private static final String SELECT_COLUMNS = """
      SELECT audit_log_id, organization_id, actor_user_id, actor_role::text AS actor_role,
             action, target_entity_type, target_entity_id, result, reason,
             sensitivity_level, occurred_at
      FROM audit.audit_log
      """;

  private final DataSource dataSource;

  public JdbcAccessAuditSearchReader(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public List<AccessAuditRecord> search(AccessAuditSearchQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    SearchSql searchSql = searchSql(query);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(searchSql.sql())) {
      for (int index = 0; index < searchSql.binders().size(); index++) {
        searchSql.binders().get(index).bind(statement, index + 1);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        List<AccessAuditRecord> records = new ArrayList<>();
        while (resultSet.next()) {
          records.add(map(resultSet));
        }
        return List.copyOf(records);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to search access audit records", exception);
    }
  }

  private static SearchSql searchSql(AccessAuditSearchQuery query) {
    StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
    List<String> predicates = new ArrayList<>();
    List<SqlBinder> binders = new ArrayList<>();
    predicates.add("organization_id = ?");
    binders.add((statement, index) -> statement.setObject(index, query.organizationId()));
    if (query.action() != null) {
      predicates.add("action = ?");
      binders.add((statement, index) -> statement.setString(index, query.action()));
    }
    if (query.targetEntityType() != null) {
      predicates.add("target_entity_type = ?");
      binders.add((statement, index) -> statement.setString(index, query.targetEntityType()));
    }
    if (query.result() != null) {
      predicates.add("result = ?");
      binders.add((statement, index) -> statement.setString(index, query.result()));
    }
    if (query.actorUserId() != null) {
      predicates.add("actor_user_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.actorUserId()));
    }
    if (query.targetEntityId() != null) {
      predicates.add("target_entity_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.targetEntityId()));
    }
    sql.append("WHERE ")
        .append(String.join(" AND ", predicates))
        .append(" ORDER BY occurred_at DESC, audit_log_id DESC LIMIT ? OFFSET ?");
    binders.add((statement, index) -> statement.setInt(index, query.limit()));
    binders.add((statement, index) -> statement.setInt(index, query.offset()));
    return new SearchSql(sql.toString(), binders);
  }

  private static AccessAuditRecord map(ResultSet resultSet) throws SQLException {
    return new AccessAuditRecord(
        resultSet.getObject("audit_log_id", UUID.class),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getObject("actor_user_id", UUID.class),
        resultSet.getString("actor_role"),
        resultSet.getString("action"),
        resultSet.getString("target_entity_type"),
        resultSet.getObject("target_entity_id", UUID.class),
        resultSet.getString("result"),
        resultSet.getString("reason"),
        resultSet.getString("sensitivity_level"),
        resultSet.getObject("occurred_at", OffsetDateTime.class).toInstant());
  }

  private record SearchSql(String sql, List<SqlBinder> binders) {}

  @FunctionalInterface
  private interface SqlBinder {
    void bind(PreparedStatement statement, int index) throws SQLException;
  }
}
