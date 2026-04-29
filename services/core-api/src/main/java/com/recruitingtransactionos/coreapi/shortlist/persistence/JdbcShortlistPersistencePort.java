package com.recruitingtransactionos.coreapi.shortlist.persistence;

import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistPersistencePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcShortlistPersistencePort implements ShortlistPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.shortlist (
        shortlist_id, organization_id, job_id, title, status,
        sent_at, client_viewed_at, owner_consultant_id, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT shortlist_id, organization_id, job_id, title, status,
        sent_at, client_viewed_at, owner_consultant_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.shortlist
      WHERE organization_id = ? AND shortlist_id = ?
      """;

  private static final String FIND_BY_JOB_SQL = """
      SELECT shortlist_id, organization_id, job_id, title, status,
        sent_at, client_viewed_at, owner_consultant_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.shortlist
      WHERE organization_id = ? AND job_id = ?
      ORDER BY created_at DESC
      """;

  private final DataSource dataSource;

  public JdbcShortlistPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Shortlist create(Shortlist shortlist) {
    Objects.requireNonNull(shortlist, "shortlist must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, shortlist.shortlistId().value());
      statement.setObject(2, shortlist.organizationId());
      statement.setObject(3, shortlist.jobId().value());
      statement.setString(4, shortlist.title());
      statement.setString(5, shortlist.status().wireValue());
      statement.setTimestamp(6,
          shortlist.sentAt() != null ? Timestamp.from(shortlist.sentAt()) : null);
      statement.setTimestamp(7,
          shortlist.clientViewedAt() != null
              ? Timestamp.from(shortlist.clientViewedAt()) : null);
      statement.setObject(8, shortlist.ownerConsultantId());
      statement.setString(9, shortlist.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(shortlist.organizationId(), shortlist.shortlistId())
          .orElseThrow(() -> new IllegalStateException("shortlist not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create shortlist", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<Shortlist> findByIdAndOrganizationId(
      UUID organizationId, ShortlistId shortlistId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, shortlistId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toShortlist(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find shortlist by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Shortlist> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_JOB_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Shortlist> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toShortlist(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find shortlists by job", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static Shortlist toShortlist(ResultSet rs) throws SQLException {
    OffsetDateTime sentAtOdt = rs.getObject("sent_at", OffsetDateTime.class);
    OffsetDateTime clientViewedAtOdt = rs.getObject("client_viewed_at", OffsetDateTime.class);
    return Shortlist.builder()
        .shortlistId(new ShortlistId(rs.getObject("shortlist_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .jobId(new JobId(rs.getObject("job_id", UUID.class)))
        .title(rs.getString("title"))
        .status(ShortlistStatus.fromWireValue(rs.getString("status")))
        .sentAt(sentAtOdt != null ? sentAtOdt.toInstant() : null)
        .clientViewedAt(clientViewedAtOdt != null ? clientViewedAtOdt.toInstant() : null)
        .ownerConsultantId(rs.getObject("owner_consultant_id", UUID.class))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
