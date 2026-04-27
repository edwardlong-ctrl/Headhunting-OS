package com.recruitingtransactionos.coreapi.governedintake.persistence;

import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForReview;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemReviewLookupPort;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcClaimLedgerItemReviewLookupPort implements ClaimLedgerItemReviewLookupPort {

  private static final String FIND_BY_ID_AND_ORG_SQL = """
      SELECT
        claim_ledger_item_id,
        organization_id,
        entity_type,
        entity_id,
        target_field_path,
        source_span_ref,
        verification_status::text AS verification_status,
        client_shareability::text AS client_shareability
      FROM governance.claim_ledger_item
      WHERE organization_id = ?
        AND claim_ledger_item_id = ?
      """;

  private final DataSource dataSource;

  public JdbcClaimLedgerItemReviewLookupPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<ClaimLedgerItemForReview> findByIdAndOrganizationId(
      UUID organizationId,
      ClaimId claimLedgerItemId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(claimLedgerItemId, "claimLedgerItemId must not be null");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_AND_ORG_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, claimLedgerItemId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(new ClaimLedgerItemForReview(
            new ClaimId(resultSet.getObject("claim_ledger_item_id", UUID.class)),
            resultSet.getObject("organization_id", UUID.class),
            new EntityRef(
                resultSet.getString("entity_type"),
                resultSet.getObject("entity_id", UUID.class)),
            resultSet.getString("target_field_path"),
            new SourceSpanRef(resultSet.getString("source_span_ref")),
            verificationStatus(resultSet.getString("verification_status")),
            clientShareability(resultSet.getString("client_shareability"))));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find claim ledger item for governed intake review bridge",
          exception);
    }
  }

  private static VerificationStatus verificationStatus(String wireValue) {
    for (VerificationStatus status : VerificationStatus.values()) {
      if (status.wireValue().equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("unknown verification status: " + wireValue);
  }

  private static ClientShareability clientShareability(String wireValue) {
    for (ClientShareability shareability : ClientShareability.values()) {
      if (shareability.wireValue().equals(wireValue)) {
        return shareability;
      }
    }
    throw new IllegalArgumentException("unknown client shareability: " + wireValue);
  }
}
