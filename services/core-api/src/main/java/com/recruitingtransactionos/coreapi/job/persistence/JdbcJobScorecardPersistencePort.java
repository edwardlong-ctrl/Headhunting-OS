package com.recruitingtransactionos.coreapi.job.persistence;

import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobScorecardId;
import com.recruitingtransactionos.coreapi.job.port.JobScorecardPersistencePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcJobScorecardPersistencePort implements JobScorecardPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.job_scorecard (
        job_scorecard_id, organization_id, job_id, dimensions,
        scoring_guidance, status, metadata
      ) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?::jsonb)
      """;

  private static final String FIND_ACTIVE_BY_JOB_SQL = """
      SELECT job_scorecard_id, organization_id, job_id,
        dimensions::text AS dimensions, scoring_guidance, status,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.job_scorecard
      WHERE organization_id = ? AND job_id = ? AND status = 'active'
      """;

  private final DataSource dataSource;

  public JdbcJobScorecardPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public JobScorecard create(JobScorecard scorecard) {
    Objects.requireNonNull(scorecard, "scorecard must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, scorecard.jobScorecardId().value());
      statement.setObject(2, scorecard.organizationId());
      statement.setObject(3, scorecard.jobId().value());
      statement.setString(4, scorecard.dimensions());
      statement.setString(5, scorecard.scoringGuidance());
      statement.setString(6, scorecard.status());
      statement.setString(7, scorecard.metadata());
      statement.executeUpdate();
      return scorecard;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create job scorecard", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<JobScorecard> findActiveByJobIdAndOrganizationId(
      UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_BY_JOB_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId.value());
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next() ? Optional.of(toJobScorecard(rs)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find active job scorecard", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static JobScorecard toJobScorecard(ResultSet rs) throws SQLException {
    return JobScorecard.builder()
        .jobScorecardId(new JobScorecardId(rs.getObject("job_scorecard_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .jobId(new JobId(rs.getObject("job_id", UUID.class)))
        .dimensions(rs.getString("dimensions"))
        .scoringGuidance(rs.getString("scoring_guidance"))
        .status(rs.getString("status"))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
