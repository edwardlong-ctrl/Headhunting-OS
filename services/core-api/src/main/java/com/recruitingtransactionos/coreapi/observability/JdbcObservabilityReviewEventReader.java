package com.recruitingtransactionos.coreapi.observability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcObservabilityReviewEventReader implements ObservabilityReviewEventReader {

  private static final String SELECT_COLUMNS = """
      SELECT
        review_event_id,
        reviewer_user_id,
        target_entity_type,
        target_entity_id,
        field_path,
        risk_tier::text AS risk_tier,
        decision,
        status,
        claim_ledger_item_id,
        source_span_ref,
        reason,
        created_at
      FROM governance.review_event
      """;

  private final DataSource dataSource;

  public JdbcObservabilityReviewEventReader(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public List<ObservabilityReviewEventRecord> search(ObservabilityReviewEventQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    SearchSql searchSql = searchSql(query);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(searchSql.sql())) {
      for (int index = 0; index < searchSql.binders().size(); index++) {
        searchSql.binders().get(index).bind(statement, index + 1);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ObservabilityReviewEventRecord> records = new ArrayList<>();
        while (resultSet.next()) {
          records.add(map(resultSet));
        }
        return List.copyOf(records);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to search review events", exception);
    }
  }

  private static SearchSql searchSql(ObservabilityReviewEventQuery query) {
    StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
    List<String> predicates = new ArrayList<>();
    List<SqlBinder> binders = new ArrayList<>();
    predicates.add("organization_id = ?");
    binders.add((statement, index) -> statement.setObject(index, query.organizationId()));
    if (query.targetEntityType() != null) {
      predicates.add("target_entity_type = ?");
      binders.add((statement, index) -> statement.setString(index, query.targetEntityType()));
    }
    if (query.targetEntityId() != null) {
      predicates.add("target_entity_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.targetEntityId()));
    }
    if (query.status() != null) {
      predicates.add("status = ?");
      binders.add((statement, index) -> statement.setString(index, query.status()));
    }
    if (query.claimLedgerItemId() != null) {
      predicates.add("claim_ledger_item_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.claimLedgerItemId()));
    }
    if (query.reviewerUserId() != null) {
      predicates.add("reviewer_user_id = ?");
      binders.add((statement, index) -> statement.setObject(index, query.reviewerUserId()));
    }
    if (query.createdFrom() != null) {
      predicates.add("created_at >= ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          OffsetDateTime.ofInstant(query.createdFrom(), ZoneOffset.UTC)));
    }
    if (query.createdTo() != null) {
      predicates.add("created_at <= ?");
      binders.add((statement, index) -> statement.setObject(
          index,
          OffsetDateTime.ofInstant(query.createdTo(), ZoneOffset.UTC)));
    }
    sql.append("WHERE ")
        .append(String.join(" AND ", predicates))
        .append(" ORDER BY created_at DESC, review_event_id DESC LIMIT ? OFFSET ?");
    binders.add((statement, index) -> statement.setInt(index, query.limit()));
    binders.add((statement, index) -> statement.setInt(index, query.offset()));
    return new SearchSql(sql.toString(), binders);
  }

  private static ObservabilityReviewEventRecord map(ResultSet resultSet) throws SQLException {
    return new ObservabilityReviewEventRecord(
        resultSet.getObject("review_event_id", UUID.class),
        resultSet.getObject("reviewer_user_id", UUID.class),
        resultSet.getString("target_entity_type"),
        resultSet.getObject("target_entity_id", UUID.class),
        resultSet.getString("field_path"),
        resultSet.getString("risk_tier"),
        resultSet.getString("decision"),
        resultSet.getString("status"),
        resultSet.getObject("claim_ledger_item_id", UUID.class),
        resultSet.getString("source_span_ref"),
        resultSet.getString("reason"),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private record SearchSql(String sql, List<SqlBinder> binders) {}

  @FunctionalInterface
  private interface SqlBinder {
    void bind(PreparedStatement statement, int index) throws SQLException;
  }
}
