package com.recruitingtransactionos.coreapi.truthlayer.persistence;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcClaimLedgerPort implements ClaimLedgerPort {

  private static final String INSERT_SQL = """
      INSERT INTO governance.claim_ledger_item (
        claim_ledger_item_id,
        organization_id,
        entity_type,
        entity_id,
        claim_type,
        assertion_strength,
        source_span_ref,
        speaker,
        verification_status,
        canonical_write_allowed,
        client_shareability,
        target_field_path,
        source_item_id,
        ai_task_run_id
      )
      VALUES (
        ?,
        ?,
        ?,
        ?,
        ?::governance.claim_type,
        ?::governance.assertion_strength,
        ?,
        ?::governance.actor_role,
        ?::governance.verification_status,
        false,
        ?::governance.client_shareability,
        ?,
        ?,
        ?
      )
      """;

  private final DataSource dataSource;

  public JdbcClaimLedgerPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public ClaimLedgerAppendResult append(ClaimLedgerAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    ClaimId claimId = new ClaimId(UUID.randomUUID());

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      bind(statement, claimId, command);
      statement.executeUpdate();
      return new ClaimLedgerAppendResult(claimId);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append claim ledger item", exception);
    }
  }

  private static void bind(
      PreparedStatement statement,
      ClaimId claimId,
      ClaimLedgerAppendCommand command) throws SQLException {
    statement.setObject(1, claimId.value());
    statement.setObject(2, command.organizationId());
    statement.setString(3, command.targetEntity().entityType());
    statement.setObject(4, command.targetEntity().entityId());
    statement.setString(5, command.claimType().wireValue());
    statement.setString(6, command.assertionStrength().wireValue());
    statement.setString(7, command.sourceSpanReference().value());
    statement.setString(8, command.speaker().wireValue());
    statement.setString(9, command.verificationStatus().wireValue());
    statement.setString(10, command.clientShareability().wireValue());
    statement.setString(11, command.targetFieldPath());
    setNullableUuid(statement, 12, command.sourceItemId());
    setNullableUuid(statement, 13, aiTaskRunUuid(command.aiTaskRunId()));
  }

  private static UUID aiTaskRunUuid(AITaskRunId aiTaskRunId) {
    if (aiTaskRunId == null) {
      return null;
    }
    return aiTaskRunId.value();
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
}
