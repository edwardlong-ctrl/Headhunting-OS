package com.recruitingtransactionos.coreapi.governedintake.persistence;

import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
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

public final class JdbcClaimLedgerItemCanonicalWriteLookupPort
    implements ClaimLedgerItemCanonicalWriteLookupPort {

  private static final String FIND_BY_ID_AND_ORG_SQL = """
      SELECT
        claim_ledger_item_id,
        organization_id,
        entity_type,
        entity_id,
        target_field_path,
        claim_type::text AS claim_type,
        assertion_strength::text AS assertion_strength,
        verification_status::text AS verification_status,
        client_shareability::text AS client_shareability,
        canonical_write_allowed,
        claim_value_text,
        source_span_ref
      FROM governance.claim_ledger_item
      WHERE organization_id = ?
        AND claim_ledger_item_id = ?
      """;

  private final DataSource dataSource;

  public JdbcClaimLedgerItemCanonicalWriteLookupPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<ClaimLedgerItemForCanonicalWrite> findByIdAndOrganizationId(
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
        return Optional.of(new ClaimLedgerItemForCanonicalWrite(
            new ClaimId(resultSet.getObject("claim_ledger_item_id", UUID.class)),
            resultSet.getObject("organization_id", UUID.class),
            new EntityRef(
                resultSet.getString("entity_type"),
                resultSet.getObject("entity_id", UUID.class)),
            resultSet.getString("target_field_path"),
            claimType(resultSet.getString("claim_type")),
            assertionStrength(resultSet.getString("assertion_strength")),
            verificationStatus(resultSet.getString("verification_status")),
            clientShareability(resultSet.getString("client_shareability")),
            resultSet.getBoolean("canonical_write_allowed"),
            resultSet.getString("claim_value_text"),
            new SourceSpanRef(resultSet.getString("source_span_ref"))));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find claim ledger item for governed intake canonical write bridge",
          exception);
    }
  }

  private static ClaimType claimType(String wireValue) {
    for (ClaimType value : ClaimType.values()) {
      if (value.wireValue().equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown claim type: " + wireValue);
  }

  private static AssertionStrength assertionStrength(String wireValue) {
    for (AssertionStrength value : AssertionStrength.values()) {
      if (value.wireValue().equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown assertion strength: " + wireValue);
  }

  private static VerificationStatus verificationStatus(String wireValue) {
    for (VerificationStatus value : VerificationStatus.values()) {
      if (value.wireValue().equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown verification status: " + wireValue);
  }

  private static ClientShareability clientShareability(String wireValue) {
    for (ClientShareability value : ClientShareability.values()) {
      if (value.wireValue().equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown client shareability: " + wireValue);
  }
}
