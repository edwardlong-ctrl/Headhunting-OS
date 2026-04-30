package com.recruitingtransactionos.coreapi.job.persistence;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.port.JobPersistencePort;
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

public final class JdbcJobPersistencePort implements JobPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.job (
        job_id, organization_id, company_id, title, description, location,
        seniority_band, role_family, employment_type, compensation, status,
        commercial_terms, owner_consultant_id, activated_at, closed_at,
        close_reason, industry_pack_id, metadata
      ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT job_id, organization_id, company_id, title, description,
        location::text AS location, seniority_band, role_family, employment_type,
        compensation::text AS compensation, status,
        commercial_terms::text AS commercial_terms, owner_consultant_id,
        activated_at, closed_at, close_reason, industry_pack_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.job
      WHERE organization_id = ? AND job_id = ?
      """;

  private static final String FIND_BY_ORG_AND_STATUS_SQL = """
      SELECT job_id, organization_id, company_id, title, description,
        location::text AS location, seniority_band, role_family, employment_type,
        compensation::text AS compensation, status,
        commercial_terms::text AS commercial_terms, owner_consultant_id,
        activated_at, closed_at, close_reason, industry_pack_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.job
      WHERE organization_id = ? AND status = ?
      ORDER BY created_at DESC
      """;

  private static final String FIND_BY_COMPANY_SQL = """
      SELECT job_id, organization_id, company_id, title, description,
        location::text AS location, seniority_band, role_family, employment_type,
        compensation::text AS compensation, status,
        commercial_terms::text AS commercial_terms, owner_consultant_id,
        activated_at, closed_at, close_reason, industry_pack_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.job
      WHERE organization_id = ? AND company_id = ?
      ORDER BY created_at DESC
      """;

  private static final String FIND_ALL_BY_ORG_SQL = """
      SELECT job_id, organization_id, company_id, title, description,
        location::text AS location, seniority_band, role_family, employment_type,
        compensation::text AS compensation, status,
        commercial_terms::text AS commercial_terms, owner_consultant_id,
        activated_at, closed_at, close_reason, industry_pack_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.job
      WHERE organization_id = ?
      ORDER BY created_at DESC
      """;

  private final DataSource dataSource;

  public JdbcJobPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Job create(Job job) {
    Objects.requireNonNull(job, "job must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, job.jobId().value());
      statement.setObject(2, job.organizationId());
      statement.setObject(3, job.companyId().value());
      statement.setString(4, job.title());
      statement.setString(5, job.description());
      statement.setString(6, job.location());
      statement.setString(7, job.seniorityBand());
      statement.setString(8, job.roleFamily());
      statement.setString(9, job.employmentType());
      statement.setString(10, job.compensation());
      statement.setString(11, job.status().wireValue());
      statement.setString(12, job.commercialTerms());
      statement.setObject(13, job.ownerConsultantId());
      statement.setObject(14, job.activatedAt());
      statement.setObject(15, job.closedAt());
      statement.setString(16, job.closeReason());
      statement.setObject(17, job.industryPackId());
      statement.setString(18, job.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(job.organizationId(), job.jobId())
          .orElseThrow(() -> new IllegalStateException("job not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create job", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<Job> findByIdAndOrganizationId(UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId.value());
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next() ? Optional.of(toJob(rs)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find job by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Job> findByOrganizationIdAndStatus(UUID organizationId, JobStatus status) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ORG_AND_STATUS_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, status.wireValue());
      return findAll(statement);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find jobs by status", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Job> findByCompanyIdAndOrganizationId(UUID organizationId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, companyId.value());
      return findAll(statement);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find jobs by company", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Job> findAllByOrganizationId(UUID organizationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_BY_ORG_SQL)) {
      statement.setObject(1, organizationId);
      return findAll(statement);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find jobs by organization", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static List<Job> findAll(PreparedStatement statement) throws SQLException {
    try (ResultSet rs = statement.executeQuery()) {
      List<Job> results = new ArrayList<>();
      while (rs.next()) {
        results.add(toJob(rs));
      }
      return List.copyOf(results);
    }
  }

  private static Job toJob(ResultSet rs) throws SQLException {
    OffsetDateTime activatedAt = rs.getObject("activated_at", OffsetDateTime.class);
    OffsetDateTime closedAt = rs.getObject("closed_at", OffsetDateTime.class);
    return Job.builder()
        .jobId(new JobId(rs.getObject("job_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .companyId(new CompanyId(rs.getObject("company_id", UUID.class)))
        .title(rs.getString("title"))
        .description(rs.getString("description"))
        .location(rs.getString("location"))
        .seniorityBand(rs.getString("seniority_band"))
        .roleFamily(rs.getString("role_family"))
        .employmentType(rs.getString("employment_type"))
        .compensation(rs.getString("compensation"))
        .status(JobStatus.fromWireValue(rs.getString("status")))
        .commercialTerms(rs.getString("commercial_terms"))
        .ownerConsultantId(rs.getObject("owner_consultant_id", UUID.class))
        .activatedAt(activatedAt == null ? null : activatedAt.toInstant())
        .closedAt(closedAt == null ? null : closedAt.toInstant())
        .closeReason(rs.getString("close_reason"))
        .industryPackId(rs.getObject("industry_pack_id", UUID.class))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
