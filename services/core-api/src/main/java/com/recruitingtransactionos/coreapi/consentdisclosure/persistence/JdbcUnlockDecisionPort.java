package com.recruitingtransactionos.coreapi.consentdisclosure.persistence;

import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureReviewStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecision;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecisionStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcUnlockDecisionPort implements UnlockDecisionPort {

  private static final String INSERT_SQL = """
      INSERT INTO privacy.unlock_decision (
        unlock_decision_ref,
        organization_id,
        candidate_ref,
        candidate_profile_ref,
        job_ref,
        client_ref,
        requested_disclosure_level,
        status,
        review_status,
        risk_tier,
        approved_by_user_id,
        approved_by_role,
        decided_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::governance.risk_tier, ?, ?::governance.actor_role, ?)
      """;

  private static final String FIND_SQL = """
      SELECT
        unlock_decision_ref,
        organization_id,
        candidate_ref,
        candidate_profile_ref,
        job_ref,
        client_ref,
        requested_disclosure_level,
        status,
        review_status,
        risk_tier,
        approved_by_user_id,
        approved_by_role,
        decided_at
      FROM privacy.unlock_decision
      WHERE organization_id = ?
        AND unlock_decision_ref = ?
      """;

  private final DataSource dataSource;

  public JdbcUnlockDecisionPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public UnlockDecision append(UnlockDecision unlockDecision) {
    Objects.requireNonNull(unlockDecision, "unlockDecision must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setString(1, unlockDecision.unlockDecisionRef());
      statement.setObject(2, unlockDecision.organizationId());
      statement.setString(3, unlockDecision.candidateRef());
      statement.setString(4, unlockDecision.candidateProfileRef());
      statement.setString(5, unlockDecision.jobRef());
      statement.setString(6, unlockDecision.clientRef());
      statement.setString(7, unlockDecision.requestedDisclosureLevel().wireValue());
      statement.setString(8, unlockDecision.status().wireValue());
      statement.setString(9, unlockDecision.reviewStatus().wireValue());
      statement.setString(10, unlockDecision.riskTier().wireValue());
      statement.setObject(11, unlockDecision.approvedBy().userId());
      statement.setString(12, unlockDecision.approvedBy().role().wireValue());
      statement.setObject(13, OffsetDateTime.ofInstant(unlockDecision.decidedAt(), ZoneOffset.UTC));
      statement.executeUpdate();
      return unlockDecision;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append unlock decision", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<UnlockDecision> findByRefAndOrganizationId(UUID organizationId, String unlockDecisionRef) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(unlockDecisionRef, "unlockDecisionRef must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, unlockDecisionRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(new UnlockDecision(
            resultSet.getString("unlock_decision_ref"),
            resultSet.getObject("organization_id", UUID.class),
            resultSet.getString("candidate_ref"),
            resultSet.getString("candidate_profile_ref"),
            resultSet.getString("job_ref"),
            resultSet.getString("client_ref"),
            DisclosureLevel.fromWireValue(resultSet.getString("requested_disclosure_level")),
            UnlockDecisionStatus.fromWireValue(resultSet.getString("status")),
            DisclosureReviewStatus.fromWireValue(resultSet.getString("review_status")),
            RiskTier.fromWireValue(resultSet.getString("risk_tier")),
            new ActorRef(
                resultSet.getObject("approved_by_user_id", UUID.class),
                ActorRole.fromWireValue(resultSet.getString("approved_by_role"))),
            resultSet.getObject("decided_at", OffsetDateTime.class).toInstant()));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find unlock decision", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
