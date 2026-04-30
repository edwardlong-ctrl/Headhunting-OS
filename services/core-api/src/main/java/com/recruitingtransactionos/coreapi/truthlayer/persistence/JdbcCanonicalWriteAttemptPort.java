package com.recruitingtransactionos.coreapi.truthlayer.persistence;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptId;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptPort;
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
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class JdbcCanonicalWriteAttemptPort implements CanonicalWriteAttemptPort {

  private static final String INSERT_SQL = """
      INSERT INTO governance.canonical_write_attempt (
        canonical_write_attempt_id,
        organization_id,
        entity_type,
        entity_id,
        entity_version,
        target_field_path,
        proposed_value_ref,
        source_span_ref,
        claim_ledger_item_id,
        review_event_id,
        decision,
        reason_codes,
        actor_user_id,
        actor_role,
        ai_task_run_id,
        idempotency_key,
        correlation_id,
        causation_id,
        workflow_event_id,
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
        ?,
        ?,
        ?,
        ?,
        ?::text[],
        ?,
        ?::governance.actor_role,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?
      )
      """;

  private static final String FIND_BY_IDEMPOTENCY_KEY_SQL = """
      SELECT canonical_write_attempt_id
      FROM governance.canonical_write_attempt
      WHERE organization_id = ?
        AND idempotency_key = ?
      """;

  private final DataSource dataSource;

  public JdbcCanonicalWriteAttemptPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<CanonicalWriteAttemptIdempotencyRecord> findByIdempotencyKey(
      UUID organizationId,
      WorkflowIdempotencyKey idempotencyKey) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");

    Connection connection = obtainConnection();
    try (PreparedStatement statement = connection.prepareStatement(
        FIND_BY_IDEMPOTENCY_KEY_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, idempotencyKey.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        CanonicalWriteAttemptId attemptId = new CanonicalWriteAttemptId(
            resultSet.getObject("canonical_write_attempt_id", UUID.class));
        return Optional.of(new CanonicalWriteAttemptIdempotencyRecord(attemptId));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find canonical write attempt by idempotency key", exception);
    } finally {
      releaseConnection(connection);
    }
  }

  @Override
  public CanonicalWriteAttemptAppendResult append(CanonicalWriteAttemptAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    CanonicalWriteAttemptId attemptId = new CanonicalWriteAttemptId(UUID.randomUUID());

    Connection connection = obtainConnection();
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      bind(statement, attemptId, connection, command);
      statement.executeUpdate();
      return new CanonicalWriteAttemptAppendResult(attemptId);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append canonical write attempt", exception);
    } finally {
      releaseConnection(connection);
    }
  }

  private Connection obtainConnection() {
    try {
      return DataSourceUtils.getConnection(dataSource);
    } catch (RuntimeException springFailure) {
      try {
        return dataSource.getConnection();
      } catch (SQLException ex) {
        throw new IllegalStateException(
            "Failed to obtain connection from DataSource", ex);
      }
    }
  }

  private void releaseConnection(Connection connection) {
    try {
      DataSourceUtils.releaseConnection(connection, dataSource);
    } catch (RuntimeException springFailure) {
      try {
        connection.close();
      } catch (SQLException ex) {
        throw new IllegalStateException("Failed to close connection", ex);
      }
    }
  }

  private static void bind(
      PreparedStatement statement,
      CanonicalWriteAttemptId attemptId,
      Connection connection,
      CanonicalWriteAttemptAppendCommand command) throws SQLException {
    statement.setObject(1, attemptId.value());
    statement.setObject(2, command.organizationId());
    statement.setString(3, command.targetEntity().entityType());
    statement.setObject(4, command.targetEntity().entityId());
    setNullableInteger(statement, 5, command.targetEntityVersion());
    statement.setString(6, command.targetFieldPath());
    statement.setString(7, command.proposedValueRef());
    setNullableString(statement, 8, command.sourceSpanRef());
    setNullableUuid(statement, 9, claimUuid(command.claimLedgerItemId()));
    setNullableUuid(statement, 10, reviewUuid(command.reviewEventId()));
    statement.setString(11, command.decision());
    statement.setArray(12, connection.createArrayOf("text",
        command.reasonCodes().toArray(new String[0])));
    statement.setObject(13, command.actor().userId());
    statement.setString(14, command.actor().role().wireValue());
    setNullableUuid(statement, 15, aiTaskRunUuid(command.aiTaskRunId()));
    setNullableString(statement, 16, idempotencyKey(command.idempotencyKey()));
    setNullableUuid(statement, 17, correlationUuid(command.correlationId()));
    setNullableUuid(statement, 18, causationUuid(command.causationId()));
    setNullableUuid(statement, 19, workflowEventUuid(command.workflowEventId()));
    statement.setObject(20, OffsetDateTime.ofInstant(command.occurredAt(), ZoneOffset.UTC));
  }

  private static UUID claimUuid(ClaimId claimId) {
    if (claimId == null) {
      return null;
    }
    return claimId.value();
  }

  private static UUID reviewUuid(ReviewEventId reviewEventId) {
    if (reviewEventId == null) {
      return null;
    }
    return reviewEventId.value();
  }

  private static UUID aiTaskRunUuid(AITaskRunId aiTaskRunId) {
    if (aiTaskRunId == null) {
      return null;
    }
    return aiTaskRunId.value();
  }

  private static UUID correlationUuid(WorkflowCorrelationId correlationId) {
    if (correlationId == null) {
      return null;
    }
    return correlationId.value();
  }

  private static UUID causationUuid(WorkflowCausationId causationId) {
    if (causationId == null) {
      return null;
    }
    return causationId.value();
  }

  private static UUID workflowEventUuid(WorkflowEventId workflowEventId) {
    if (workflowEventId == null) {
      return null;
    }
    return workflowEventId.value();
  }

  private static String idempotencyKey(WorkflowIdempotencyKey idempotencyKey) {
    if (idempotencyKey == null) {
      return null;
    }
    return idempotencyKey.value();
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
