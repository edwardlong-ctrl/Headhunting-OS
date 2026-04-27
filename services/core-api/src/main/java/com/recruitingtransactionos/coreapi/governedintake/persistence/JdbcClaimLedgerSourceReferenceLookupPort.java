package com.recruitingtransactionos.coreapi.governedintake.persistence;

import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReference;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReferenceLookupPort;
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

public final class JdbcClaimLedgerSourceReferenceLookupPort
    implements ClaimLedgerSourceReferenceLookupPort {

  private static final String FIND_BY_SOURCE_SPAN_SQL = """
      SELECT
        claim_ledger_item_id,
        organization_id,
        entity_type,
        entity_id,
        target_field_path,
        source_span_ref
      FROM governance.claim_ledger_item
      WHERE organization_id = ?
        AND source_span_ref = ?
      ORDER BY created_at ASC, claim_ledger_item_id ASC
      LIMIT 1
      """;

  private final DataSource dataSource;

  public JdbcClaimLedgerSourceReferenceLookupPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<ClaimLedgerSourceReference> findBySourceSpanReference(
      UUID organizationId,
      SourceSpanRef sourceSpanReference) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceSpanReference, "sourceSpanReference must not be null");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_SOURCE_SPAN_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, sourceSpanReference.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(new ClaimLedgerSourceReference(
            new ClaimId(resultSet.getObject("claim_ledger_item_id", UUID.class)),
            resultSet.getObject("organization_id", UUID.class),
            new EntityRef(
                resultSet.getString("entity_type"),
                resultSet.getObject("entity_id", UUID.class)),
            resultSet.getString("target_field_path"),
            new SourceSpanRef(resultSet.getString("source_span_ref"))));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find claim ledger item by governed intake source reference",
          exception);
    }
  }
}
