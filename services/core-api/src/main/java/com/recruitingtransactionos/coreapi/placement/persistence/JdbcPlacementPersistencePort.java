package com.recruitingtransactionos.coreapi.placement.persistence;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementStatus;
import com.recruitingtransactionos.coreapi.placement.port.PlacementPersistencePort;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcPlacementPersistencePort implements PlacementPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.placement (
        placement_id, organization_id, job_id, candidate_id, company_id,
        status, offer_details, offer_accepted_at, start_date, onboarded_at,
        guarantee_days, guarantee_expires_at, cancelled_at, cancel_reason, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String UPDATE_SQL = """
      UPDATE recruiting.placement
      SET status = ?,
          offer_details = ?::jsonb,
          offer_accepted_at = ?,
          start_date = ?,
          onboarded_at = ?,
          guarantee_days = ?,
          guarantee_expires_at = ?,
          cancelled_at = ?,
          cancel_reason = ?,
          metadata = ?::jsonb,
          updated_at = ?,
          version = version + 1
      WHERE organization_id = ? AND placement_id = ? AND version = ?
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT placement_id, organization_id, job_id, candidate_id, company_id,
        status, offer_details::text AS offer_details, offer_accepted_at,
        start_date, onboarded_at, guarantee_days, guarantee_expires_at,
        cancelled_at, cancel_reason, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.placement
      WHERE organization_id = ? AND placement_id = ?
      """;

  private static final String FIND_BY_JOB_SQL = """
      SELECT placement_id, organization_id, job_id, candidate_id, company_id,
        status, offer_details::text AS offer_details, offer_accepted_at,
        start_date, onboarded_at, guarantee_days, guarantee_expires_at,
        cancelled_at, cancel_reason, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.placement
      WHERE organization_id = ? AND job_id = ?
      ORDER BY created_at DESC
      """;

  private static final String FIND_ALL_SQL = """
      SELECT placement_id, organization_id, job_id, candidate_id, company_id,
        status, offer_details::text AS offer_details, offer_accepted_at,
        start_date, onboarded_at, guarantee_days, guarantee_expires_at,
        cancelled_at, cancel_reason, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.placement
      WHERE organization_id = ?
      ORDER BY created_at DESC
      """;

  private static final String FIND_BY_CANDIDATE_SQL = """
      SELECT placement_id, organization_id, job_id, candidate_id, company_id,
        status, offer_details::text AS offer_details, offer_accepted_at,
        start_date, onboarded_at, guarantee_days, guarantee_expires_at,
        cancelled_at, cancel_reason, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.placement
      WHERE organization_id = ? AND candidate_id = ?
      ORDER BY created_at DESC
      """;

  private final DataSource dataSource;

  public JdbcPlacementPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Placement create(Placement placement) {
    Objects.requireNonNull(placement, "placement must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, placement.placementId().value());
      statement.setObject(2, placement.organizationId());
      statement.setObject(3, placement.jobId().value());
      statement.setObject(4, placement.candidateId().value());
      statement.setObject(5, placement.companyId().value());
      statement.setString(6, placement.status().wireValue());
      statement.setString(7, placement.offerDetails());
      statement.setTimestamp(8,
          placement.offerAcceptedAt() != null
              ? Timestamp.from(placement.offerAcceptedAt()) : null);
      if (placement.startDate() != null) {
        statement.setDate(9, Date.valueOf(placement.startDate()));
      } else {
        statement.setNull(9, Types.DATE);
      }
      statement.setTimestamp(10,
          placement.onboardedAt() != null ? Timestamp.from(placement.onboardedAt()) : null);
      if (placement.guaranteeDays() != null) {
        statement.setInt(11, placement.guaranteeDays());
      } else {
        statement.setNull(11, Types.INTEGER);
      }
      if (placement.guaranteeExpiresAt() != null) {
        statement.setDate(12, Date.valueOf(placement.guaranteeExpiresAt()));
      } else {
        statement.setNull(12, Types.DATE);
      }
      statement.setTimestamp(13,
          placement.cancelledAt() != null ? Timestamp.from(placement.cancelledAt()) : null);
      statement.setString(14, placement.cancelReason());
      statement.setString(15, placement.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(placement.organizationId(), placement.placementId())
          .orElseThrow(() -> new IllegalStateException("placement not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create placement", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<Placement> findByIdAndOrganizationId(
      UUID organizationId, PlacementId placementId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(placementId, "placementId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, placementId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toPlacement(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find placement by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Placement update(Placement placement) {
    Objects.requireNonNull(placement, "placement must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
      statement.setString(1, placement.status().wireValue());
      statement.setString(2, placement.offerDetails());
      statement.setTimestamp(
          3,
          placement.offerAcceptedAt() != null ? Timestamp.from(placement.offerAcceptedAt()) : null);
      if (placement.startDate() != null) {
        statement.setDate(4, Date.valueOf(placement.startDate()));
      } else {
        statement.setNull(4, Types.DATE);
      }
      statement.setTimestamp(
          5,
          placement.onboardedAt() != null ? Timestamp.from(placement.onboardedAt()) : null);
      if (placement.guaranteeDays() != null) {
        statement.setInt(6, placement.guaranteeDays());
      } else {
        statement.setNull(6, Types.INTEGER);
      }
      if (placement.guaranteeExpiresAt() != null) {
        statement.setDate(7, Date.valueOf(placement.guaranteeExpiresAt()));
      } else {
        statement.setNull(7, Types.DATE);
      }
      statement.setTimestamp(
          8, placement.cancelledAt() != null ? Timestamp.from(placement.cancelledAt()) : null);
      statement.setString(9, placement.cancelReason());
      statement.setString(10, placement.metadata());
      statement.setTimestamp(11, Timestamp.from(placement.updatedAt()));
      statement.setObject(12, placement.organizationId());
      statement.setObject(13, placement.placementId().value());
      statement.setInt(14, placement.version());
      int updatedRows = statement.executeUpdate();
      if (updatedRows != 1) {
        throw new IllegalStateException("placement_update_conflict");
      }
      return findByIdAndOrganizationId(placement.organizationId(), placement.placementId())
          .orElseThrow(() -> new IllegalStateException("placement not readable after update"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to update placement", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Placement> findAllByOrganizationId(UUID organizationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Placement> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toPlacement(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find placements by organization", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Placement> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_JOB_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Placement> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toPlacement(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find placements by job", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Placement> findByCandidateIdAndOrganizationId(
      UUID organizationId, CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_CANDIDATE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Placement> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toPlacement(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find placements by candidate", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static Placement toPlacement(ResultSet rs) throws SQLException {
    OffsetDateTime offerAcceptedAtOdt = rs.getObject("offer_accepted_at", OffsetDateTime.class);
    OffsetDateTime onboardedAtOdt = rs.getObject("onboarded_at", OffsetDateTime.class);
    OffsetDateTime cancelledAtOdt = rs.getObject("cancelled_at", OffsetDateTime.class);
    Date startDateSql = rs.getDate("start_date");
    Date guaranteeExpiresSql = rs.getDate("guarantee_expires_at");
    int gDays = rs.getInt("guarantee_days");
    Integer guaranteeDays = rs.wasNull() ? null : gDays;
    return Placement.builder()
        .placementId(new PlacementId(rs.getObject("placement_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .jobId(new JobId(rs.getObject("job_id", UUID.class)))
        .candidateId(new CandidateId(rs.getObject("candidate_id", UUID.class)))
        .companyId(new CompanyId(rs.getObject("company_id", UUID.class)))
        .status(PlacementStatus.fromWireValue(rs.getString("status")))
        .offerDetails(rs.getString("offer_details"))
        .offerAcceptedAt(offerAcceptedAtOdt != null ? offerAcceptedAtOdt.toInstant() : null)
        .startDate(startDateSql != null ? startDateSql.toLocalDate() : null)
        .onboardedAt(onboardedAtOdt != null ? onboardedAtOdt.toInstant() : null)
        .guaranteeDays(guaranteeDays)
        .guaranteeExpiresAt(
            guaranteeExpiresSql != null ? guaranteeExpiresSql.toLocalDate() : null)
        .cancelledAt(cancelledAtOdt != null ? cancelledAtOdt.toInstant() : null)
        .cancelReason(rs.getString("cancel_reason"))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
