package com.recruitingtransactionos.coreapi.consentdisclosure.persistence;

import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestId;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcClientUnlockRequestPort implements ClientUnlockRequestPort {

  private static final String INSERT_SQL = """
      INSERT INTO privacy.client_unlock_request (
        client_unlock_request_id, organization_id, shortlist_id, shortlist_candidate_card_id,
        job_id, client_actor_id, anonymous_candidate_card_ref, request_reason, status,
        unlock_decision_ref, approved_disclosure_record_ref
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_LATEST_BY_CARD_SQL = """
      SELECT client_unlock_request_id, organization_id, shortlist_id, shortlist_candidate_card_id,
        job_id, client_actor_id, anonymous_candidate_card_ref, request_reason, status,
        unlock_decision_ref, approved_disclosure_record_ref, created_at, updated_at, version
      FROM privacy.client_unlock_request
      WHERE organization_id = ? AND shortlist_id = ? AND shortlist_candidate_card_id = ?
      ORDER BY created_at DESC
      LIMIT 1
      """;

  private static final String FIND_BY_ORG_SQL = """
      SELECT client_unlock_request_id, organization_id, shortlist_id, shortlist_candidate_card_id,
        job_id, client_actor_id, anonymous_candidate_card_ref, request_reason, status,
        unlock_decision_ref, approved_disclosure_record_ref, created_at, updated_at, version
      FROM privacy.client_unlock_request
      WHERE organization_id = ?
      ORDER BY created_at DESC
      """;

  private static final String FIND_BY_CLIENT_SQL = """
      SELECT client_unlock_request_id, organization_id, shortlist_id, shortlist_candidate_card_id,
        job_id, client_actor_id, anonymous_candidate_card_ref, request_reason, status,
        unlock_decision_ref, approved_disclosure_record_ref, created_at, updated_at, version
      FROM privacy.client_unlock_request
      WHERE organization_id = ? AND client_actor_id = ?
      ORDER BY created_at DESC
      """;

  private static final String FIND_BY_SHORTLIST_SQL = """
      SELECT client_unlock_request_id, organization_id, shortlist_id, shortlist_candidate_card_id,
        job_id, client_actor_id, anonymous_candidate_card_ref, request_reason, status,
        unlock_decision_ref, approved_disclosure_record_ref, created_at, updated_at, version
      FROM privacy.client_unlock_request
      WHERE organization_id = ? AND shortlist_id = ?
      ORDER BY created_at DESC
      """;

  private final DataSource dataSource;

  public JdbcClientUnlockRequestPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public ClientUnlockRequest create(ClientUnlockRequest clientUnlockRequest) {
    Objects.requireNonNull(clientUnlockRequest, "clientUnlockRequest must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, clientUnlockRequest.clientUnlockRequestId().value());
      statement.setObject(2, clientUnlockRequest.organizationId());
      statement.setObject(3, clientUnlockRequest.shortlistId().value());
      statement.setObject(4, clientUnlockRequest.shortlistCandidateCardId().value());
      statement.setObject(5, clientUnlockRequest.jobId());
      statement.setObject(6, clientUnlockRequest.clientActorId());
      statement.setString(7, clientUnlockRequest.anonymousCandidateCardRef());
      statement.setString(8, clientUnlockRequest.requestReason());
      statement.setString(9, clientUnlockRequest.status().wireValue());
      statement.setString(10, clientUnlockRequest.unlockDecisionRef());
      statement.setString(11, clientUnlockRequest.approvedDisclosureRecordRef());
      statement.executeUpdate();
      return findLatestByShortlistCardAndOrganizationId(
          clientUnlockRequest.organizationId(),
          clientUnlockRequest.shortlistId(),
          clientUnlockRequest.shortlistCandidateCardId()).orElseThrow();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create client_unlock_request", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<ClientUnlockRequest> findLatestByShortlistCardAndOrganizationId(
      UUID organizationId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId shortlistCandidateCardId) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_LATEST_BY_CARD_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, shortlistId.value());
      statement.setObject(3, shortlistCandidateCardId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toClientUnlockRequest(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find latest client_unlock_request", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<ClientUnlockRequest> findByOrganizationId(UUID organizationId) {
    return findMany(FIND_BY_ORG_SQL, organizationId, null, null);
  }

  @Override
  public List<ClientUnlockRequest> findByClientActorId(UUID organizationId, UUID clientActorId) {
    return findMany(FIND_BY_CLIENT_SQL, organizationId, clientActorId, null);
  }

  @Override
  public List<ClientUnlockRequest> findByShortlistId(UUID organizationId, ShortlistId shortlistId) {
    return findMany(FIND_BY_SHORTLIST_SQL, organizationId, shortlistId.value(), null);
  }

  private List<ClientUnlockRequest> findMany(String sql, UUID organizationId, Object second, Object third) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      if (second != null) {
        statement.setObject(2, second);
      }
      if (third != null) {
        statement.setObject(3, third);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ClientUnlockRequest> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toClientUnlockRequest(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to query client_unlock_request", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static ClientUnlockRequest toClientUnlockRequest(ResultSet rs) throws SQLException {
    return ClientUnlockRequest.builder()
        .clientUnlockRequestId(new ClientUnlockRequestId(rs.getObject("client_unlock_request_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .shortlistId(new ShortlistId(rs.getObject("shortlist_id", UUID.class)))
        .shortlistCandidateCardId(new ShortlistCandidateCardId(rs.getObject("shortlist_candidate_card_id", UUID.class)))
        .jobId(rs.getObject("job_id", UUID.class))
        .clientActorId(rs.getObject("client_actor_id", UUID.class))
        .anonymousCandidateCardRef(rs.getString("anonymous_candidate_card_ref"))
        .requestReason(rs.getString("request_reason"))
        .status(ClientUnlockRequestStatus.fromWireValue(rs.getString("status")))
        .unlockDecisionRef(rs.getString("unlock_decision_ref"))
        .approvedDisclosureRecordRef(rs.getString("approved_disclosure_record_ref"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
