package com.recruitingtransactionos.coreapi.shortlist.persistence;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistCandidateCardPersistencePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcShortlistCandidateCardPersistencePort
    implements ShortlistCandidateCardPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.shortlist_candidate_card (
        shortlist_candidate_card_id, organization_id, shortlist_id,
        anonymous_candidate_card_id, candidate_id, candidate_profile_id,
        sort_order, status, match_report_id, client_notes, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_SHORTLIST_SQL = """
      SELECT shortlist_candidate_card_id, organization_id, shortlist_id,
        anonymous_candidate_card_id, candidate_id, candidate_profile_id,
        sort_order, status, match_report_id, client_notes,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.shortlist_candidate_card
      WHERE organization_id = ? AND shortlist_id = ?
      ORDER BY sort_order
      """;

  private final DataSource dataSource;

  public JdbcShortlistCandidateCardPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public ShortlistCandidateCard create(ShortlistCandidateCard card) {
    Objects.requireNonNull(card, "card must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, card.shortlistCandidateCardId().value());
      statement.setObject(2, card.organizationId());
      statement.setObject(3, card.shortlistId().value());
      statement.setObject(4, card.anonymousCandidateCardId());
      statement.setObject(5, card.candidateId().value());
      statement.setObject(6, card.candidateProfileId());
      statement.setInt(7, card.sortOrder());
      statement.setString(8, card.status().wireValue());
      statement.setObject(9, card.matchReportId());
      statement.setString(10, card.clientNotes());
      statement.setString(11, card.metadata());
      statement.executeUpdate();
      // Re-read from DB to get server-generated audit columns
      return findByShortlistIdAndOrganizationId(card.organizationId(), card.shortlistId())
          .stream()
          .filter(c -> c.shortlistCandidateCardId().equals(card.shortlistCandidateCardId()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException(
              "shortlist_candidate_card not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create shortlist_candidate_card", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<ShortlistCandidateCard> findByShortlistIdAndOrganizationId(
      UUID organizationId, ShortlistId shortlistId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_SHORTLIST_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, shortlistId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ShortlistCandidateCard> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCard(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find shortlist_candidate_cards by shortlist", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static ShortlistCandidateCard toCard(ResultSet rs) throws SQLException {
    return ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(
            rs.getObject("shortlist_candidate_card_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .shortlistId(new ShortlistId(rs.getObject("shortlist_id", UUID.class)))
        .anonymousCandidateCardId(
            rs.getObject("anonymous_candidate_card_id", UUID.class))
        .candidateId(new CandidateId(rs.getObject("candidate_id", UUID.class)))
        .candidateProfileId(rs.getObject("candidate_profile_id", UUID.class))
        .sortOrder(rs.getInt("sort_order"))
        .status(ShortlistCandidateCardStatus.fromWireValue(rs.getString("status")))
        .matchReportId(rs.getObject("match_report_id", UUID.class))
        .clientNotes(rs.getString("client_notes"))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
