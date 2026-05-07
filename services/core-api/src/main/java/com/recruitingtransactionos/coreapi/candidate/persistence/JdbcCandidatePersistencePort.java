package com.recruitingtransactionos.coreapi.candidate.persistence;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.port.CandidatePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcCandidatePersistencePort implements CandidatePersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.candidate (
        candidate_id, organization_id, status, current_profile_id, privacy_status,
        owner_consultant_id, do_not_contact_reason, merged_into_candidate_id,
        last_activity_at, default_industry_pack_id, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT candidate_id, organization_id, status, current_profile_id, privacy_status,
        owner_consultant_id, do_not_contact_reason, merged_into_candidate_id,
        last_activity_at, default_industry_pack_id, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.candidate
      WHERE organization_id = ? AND candidate_id = ?
      """;

  private static final String FIND_BY_ORG_AND_STATUS_SQL = """
      SELECT candidate_id, organization_id, status, current_profile_id, privacy_status,
        owner_consultant_id, do_not_contact_reason, merged_into_candidate_id,
        last_activity_at, default_industry_pack_id, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.candidate
      WHERE organization_id = ? AND status = ?
      ORDER BY created_at DESC, candidate_id
      """;

  private static final String FIND_ALL_BY_ORG_SQL = """
      SELECT candidate_id, organization_id, status, current_profile_id, privacy_status,
        owner_consultant_id, do_not_contact_reason, merged_into_candidate_id,
        last_activity_at, default_industry_pack_id, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.candidate
      WHERE organization_id = ?
      ORDER BY created_at DESC, candidate_id
      """;

  private static final String LINK_CURRENT_PROFILE_SQL = """
      UPDATE recruiting.candidate
      SET current_profile_id = ?,
          updated_at = NOW(),
          version = version + 1
      WHERE organization_id = ?
        AND candidate_id = ?
        AND EXISTS (
          SELECT 1
          FROM recruiting.candidate_profile
          WHERE organization_id = ?
            AND candidate_id = ?
            AND candidate_profile_id = ?
        )
      """;

  private final DataSource dataSource;

  public JdbcCandidatePersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Candidate create(Candidate candidate) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, candidate.candidateId().value());
      statement.setObject(2, candidate.organizationId());
      statement.setString(3, candidate.status().wireValue());
      statement.setObject(4, candidate.currentProfileId() != null
          ? candidate.currentProfileId().value() : null);
      statement.setString(5, candidate.privacyStatus());
      statement.setObject(6, candidate.ownerConsultantId());
      statement.setString(7, candidate.doNotContactReason());
      statement.setObject(8, candidate.mergedIntoCandidateId() != null
          ? candidate.mergedIntoCandidateId().value() : null);
      statement.setObject(9, candidate.lastActivityAt() != null
          ? OffsetDateTime.parse(candidate.lastActivityAt().toString()) : null);
      statement.setObject(10, candidate.defaultIndustryPackId());
      statement.setString(11, candidate.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(candidate.organizationId(), candidate.candidateId())
          .orElseThrow(() -> new IllegalStateException("candidate not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create candidate", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<Candidate> findByIdAndOrganizationId(UUID organizationId, CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toCandidate(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find candidate by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Candidate linkCurrentProfile(
      UUID organizationId,
      CandidateId candidateId,
      CandidateProfileId candidateProfileId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(LINK_CURRENT_PROFILE_SQL)) {
      statement.setObject(1, candidateProfileId.value());
      statement.setObject(2, organizationId);
      statement.setObject(3, candidateId.value());
      statement.setObject(4, organizationId);
      statement.setObject(5, candidateId.value());
      statement.setObject(6, candidateProfileId.value());
      int updated = statement.executeUpdate();
      if (updated != 1) {
        throw new IllegalArgumentException("candidate profile not found in organization");
      }
      return findByIdAndOrganizationId(organizationId, candidateId)
          .orElseThrow(() -> new IllegalStateException("candidate not readable after profile link"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to link current candidate profile", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Candidate> findByOrganizationIdAndStatus(UUID organizationId, CandidateStatus status) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ORG_AND_STATUS_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, status.wireValue());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Candidate> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCandidate(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find candidates by status", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Candidate> findAllByOrganizationId(UUID organizationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_BY_ORG_SQL)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Candidate> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCandidate(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find candidates by organization", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static Candidate toCandidate(ResultSet resultSet) throws SQLException {
    UUID currentProfileId = resultSet.getObject("current_profile_id", UUID.class);
    UUID mergedIntoCandidateId = resultSet.getObject("merged_into_candidate_id", UUID.class);
    OffsetDateTime lastActivityAt = resultSet.getObject("last_activity_at", OffsetDateTime.class);
    OffsetDateTime createdAt = resultSet.getObject("created_at", OffsetDateTime.class);
    OffsetDateTime updatedAt = resultSet.getObject("updated_at", OffsetDateTime.class);
    return Candidate.builder()
        .candidateId(new CandidateId(resultSet.getObject("candidate_id", UUID.class)))
        .organizationId(resultSet.getObject("organization_id", UUID.class))
        .status(CandidateStatus.fromWireValue(resultSet.getString("status")))
        .currentProfileId(currentProfileId != null ? new CandidateProfileId(currentProfileId) : null)
        .privacyStatus(resultSet.getString("privacy_status"))
        .ownerConsultantId(resultSet.getObject("owner_consultant_id", UUID.class))
        .doNotContactReason(resultSet.getString("do_not_contact_reason"))
        .mergedIntoCandidateId(mergedIntoCandidateId != null
            ? new CandidateId(mergedIntoCandidateId) : null)
        .lastActivityAt(lastActivityAt != null ? lastActivityAt.toInstant() : null)
        .defaultIndustryPackId(resultSet.getObject("default_industry_pack_id", UUID.class))
        .metadata(resultSet.getString("metadata"))
        .createdAt(createdAt.toInstant())
        .updatedAt(updatedAt.toInstant())
        .version(resultSet.getInt("version"))
        .build();
  }
}
