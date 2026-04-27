package com.recruitingtransactionos.coreapi.governedintake.persistence;

import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventSourceReference;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcReviewEventSourceReferenceLookupPort
    implements ReviewEventSourceReferenceLookupPort {

  private static final String FIND_BY_SOURCE_SPAN_SQL = """
      SELECT
        review_event_id,
        organization_id,
        target_entity_type,
        target_entity_id,
        field_path,
        claim_ledger_item_id,
        source_span_ref,
        decision,
        risk_tier::text AS risk_tier
      FROM governance.review_event
      WHERE organization_id = ?
        AND source_span_ref = ?
      ORDER BY created_at ASC, review_event_id ASC
      LIMIT 1
      """;

  private final DataSource dataSource;

  public JdbcReviewEventSourceReferenceLookupPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<ReviewEventSourceReference> findBySourceSpanReference(
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
        return Optional.of(new ReviewEventSourceReference(
            new ReviewEventId(resultSet.getObject("review_event_id", UUID.class)),
            resultSet.getObject("organization_id", UUID.class),
            new EntityRef(
                resultSet.getString("target_entity_type"),
                resultSet.getObject("target_entity_id", UUID.class)),
            resultSet.getString("field_path"),
            new ClaimId(resultSet.getObject("claim_ledger_item_id", UUID.class)),
            new SourceSpanRef(resultSet.getString("source_span_ref")),
            reviewDecision(resultSet.getString("decision")),
            riskTier(resultSet.getString("risk_tier"))));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find review event by governed intake source reference",
          exception);
    }
  }

  private static ReviewDecision reviewDecision(String wireValue) {
    for (ReviewDecision decision : ReviewDecision.values()) {
      if (decision.wireValue().equals(wireValue)) {
        return decision;
      }
    }
    throw new IllegalArgumentException("unknown review decision: " + wireValue);
  }

  private static RiskTier riskTier(String wireValue) {
    for (RiskTier tier : RiskTier.values()) {
      if (tier.wireValue().equals(wireValue)) {
        return tier;
      }
    }
    throw new IllegalArgumentException("unknown risk tier: " + wireValue);
  }
}
