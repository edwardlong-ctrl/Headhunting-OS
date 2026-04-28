package com.recruitingtransactionos.coreapi.governedintake.persistence;

import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcReviewEventCanonicalWriteLookupPort
    implements ReviewEventCanonicalWriteLookupPort {

  private static final String FIND_BY_ID_AND_ORG_SQL = """
      SELECT
        review_event_id,
        organization_id,
        reviewer_user_id,
        target_entity_type,
        target_entity_id,
        field_path,
        claim_ledger_item_id,
        source_span_ref,
        decision,
        risk_tier::text AS risk_tier,
        bulk_flag,
        reason
      FROM governance.review_event
      WHERE organization_id = ?
        AND review_event_id = ?
      """;

  private final DataSource dataSource;

  public JdbcReviewEventCanonicalWriteLookupPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<ReviewEventForCanonicalWrite> findByIdAndOrganizationId(
      UUID organizationId,
      ReviewEventId reviewEventId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(reviewEventId, "reviewEventId must not be null");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_AND_ORG_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, reviewEventId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(new ReviewEventForCanonicalWrite(
            new ReviewEventId(resultSet.getObject("review_event_id", UUID.class)),
            resultSet.getObject("organization_id", UUID.class),
            new EntityRef(
                resultSet.getString("target_entity_type"),
                resultSet.getObject("target_entity_id", UUID.class)),
            resultSet.getString("field_path"),
            new ClaimId(resultSet.getObject("claim_ledger_item_id", UUID.class)),
            resultSet.getString("source_span_ref"),
            reviewDecision(resultSet.getString("decision")),
            riskTier(resultSet.getString("risk_tier")),
            resultSet.getBoolean("bulk_flag"),
            resultSet.getObject("reviewer_user_id", UUID.class),
            resultSet.getString("reason")));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find review event for governed intake canonical write bridge",
          exception);
    }
  }

  private static ReviewDecision reviewDecision(String wireValue) {
    for (ReviewDecision value : ReviewDecision.values()) {
      if (value.wireValue().equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown review decision: " + wireValue);
  }

  private static RiskTier riskTier(String wireValue) {
    for (RiskTier value : RiskTier.values()) {
      if (value.wireValue().equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown risk tier: " + wireValue);
  }
}
