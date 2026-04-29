package com.recruitingtransactionos.coreapi.job.persistence;

import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobRequirementId;
import com.recruitingtransactionos.coreapi.job.JobRequirementImportance;
import com.recruitingtransactionos.coreapi.job.port.JobRequirementPersistencePort;
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

public final class JdbcJobRequirementPersistencePort implements JobRequirementPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.job_requirement (
        job_requirement_id, organization_id, job_id, requirement_type,
        label, importance, detail, sort_order
      ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)
      """;

  private static final String FIND_BY_JOB_SQL = """
      SELECT job_requirement_id, organization_id, job_id, requirement_type,
        label, importance, detail::text AS detail, sort_order,
        created_at, updated_at, version
      FROM recruiting.job_requirement
      WHERE organization_id = ? AND job_id = ?
      ORDER BY sort_order, label
      """;

  private final DataSource dataSource;

  public JdbcJobRequirementPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public JobRequirement create(JobRequirement requirement) {
    Objects.requireNonNull(requirement, "requirement must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, requirement.jobRequirementId().value());
      statement.setObject(2, requirement.organizationId());
      statement.setObject(3, requirement.jobId().value());
      statement.setString(4, requirement.requirementType());
      statement.setString(5, requirement.label());
      statement.setString(6, requirement.importance().wireValue());
      statement.setString(7, requirement.detail());
      statement.setInt(8, requirement.sortOrder());
      statement.executeUpdate();
      return requirement;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create job requirement", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<JobRequirement> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_JOB_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId.value());
      try (ResultSet rs = statement.executeQuery()) {
        List<JobRequirement> results = new ArrayList<>();
        while (rs.next()) {
          results.add(toJobRequirement(rs));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find job requirements", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static JobRequirement toJobRequirement(ResultSet rs) throws SQLException {
    return JobRequirement.builder()
        .jobRequirementId(new JobRequirementId(rs.getObject("job_requirement_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .jobId(new JobId(rs.getObject("job_id", UUID.class)))
        .requirementType(rs.getString("requirement_type"))
        .label(rs.getString("label"))
        .importance(JobRequirementImportance.fromWireValue(rs.getString("importance")))
        .detail(rs.getString("detail"))
        .sortOrder(rs.getInt("sort_order"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
