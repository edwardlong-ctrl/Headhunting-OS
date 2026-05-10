package com.recruitingtransactionos.coreapi.truthlayer.persistence;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcReviewEventPort implements ReviewEventPort {

  private static final String INSERT_SQL = """
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
        claim_ledger_item_id,
        source_span_ref,
        reason
      )
      VALUES (
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?::governance.risk_tier,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?
      )
      """;

  private final DataSource dataSource;

  public JdbcReviewEventPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public ReviewEventAppendResult append(ReviewEventAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    ReviewEventId reviewEventId = new ReviewEventId(UUID.randomUUID());

    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      bind(statement, reviewEventId, command);
      statement.executeUpdate();
      return new ReviewEventAppendResult(reviewEventId);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append review event", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static void bind(
      PreparedStatement statement,
      ReviewEventId reviewEventId,
      ReviewEventAppendCommand command) throws SQLException {
    statement.setObject(1, reviewEventId.value());
    statement.setObject(2, command.organizationId());
    statement.setObject(3, command.reviewerId());
    statement.setString(4, command.targetEntity().entityType());
    statement.setObject(5, command.targetEntity().entityId());
    statement.setString(6, command.targetFieldPath());
    statement.setString(7, command.riskTier().wireValue());
    statement.setString(8, command.decision().wireValue());
    statement.setBoolean(9, command.bulkApproval());
    statement.setInt(10, durationMillis(command.reviewDuration()));
    setNullableUuid(statement, 11, claimUuid(command.claimId()));
    setNullableString(statement, 12, sourceSpanValue(command.sourceSpanReference()));
    statement.setString(13, command.reason());
  }

  private static int durationMillis(Duration reviewDuration) {
    Objects.requireNonNull(reviewDuration, "reviewDuration must not be null");
    try {
      return Math.toIntExact(reviewDuration.toMillis());
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("reviewDuration milliseconds exceed database range",
          exception);
    }
  }

  private static UUID claimUuid(ClaimId claimId) {
    if (claimId == null) {
      return null;
    }
    return claimId.value();
  }

  private static String sourceSpanValue(SourceSpanRef sourceSpanReference) {
    if (sourceSpanReference == null) {
      return null;
    }
    return sourceSpanReference.value();
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
