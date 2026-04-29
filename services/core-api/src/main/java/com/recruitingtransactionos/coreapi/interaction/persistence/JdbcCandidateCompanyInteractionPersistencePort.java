package com.recruitingtransactionos.coreapi.interaction.persistence;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interaction.InteractionStatus;
import com.recruitingtransactionos.coreapi.interaction.InteractionType;
import com.recruitingtransactionos.coreapi.interaction.port.CandidateCompanyInteractionPersistencePort;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcCandidateCompanyInteractionPersistencePort
    implements CandidateCompanyInteractionPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.candidate_company_interaction (
        candidate_company_interaction_id, organization_id, candidate_id,
        company_id, job_id, interaction_type, status, started_at, ended_at,
        notes, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT candidate_company_interaction_id, organization_id, candidate_id,
        company_id, job_id, interaction_type, status, started_at, ended_at,
        notes, metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.candidate_company_interaction
      WHERE organization_id = ? AND candidate_company_interaction_id = ?
      """;

  private static final String FIND_BY_CANDIDATE_COMPANY_SQL = """
      SELECT candidate_company_interaction_id, organization_id, candidate_id,
        company_id, job_id, interaction_type, status, started_at, ended_at,
        notes, metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.candidate_company_interaction
      WHERE organization_id = ? AND candidate_id = ? AND company_id = ?
      ORDER BY started_at DESC
      """;

  private static final String FIND_BY_JOB_SQL = """
      SELECT candidate_company_interaction_id, organization_id, candidate_id,
        company_id, job_id, interaction_type, status, started_at, ended_at,
        notes, metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.candidate_company_interaction
      WHERE organization_id = ? AND job_id = ?
      ORDER BY started_at DESC
      """;

  private final DataSource dataSource;

  public JdbcCandidateCompanyInteractionPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public CandidateCompanyInteraction create(CandidateCompanyInteraction interaction) {
    Objects.requireNonNull(interaction, "interaction must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, interaction.candidateCompanyInteractionId().value());
      statement.setObject(2, interaction.organizationId());
      statement.setObject(3, interaction.candidateId().value());
      statement.setObject(4, interaction.companyId().value());
      statement.setObject(5, interaction.jobId() != null ? interaction.jobId().value() : null);
      statement.setString(6, interaction.interactionType().wireValue());
      statement.setString(7, interaction.status().wireValue());
      statement.setTimestamp(8, Timestamp.from(interaction.startedAt()));
      statement.setTimestamp(9,
          interaction.endedAt() != null ? Timestamp.from(interaction.endedAt()) : null);
      statement.setString(10, interaction.notes());
      statement.setString(11, interaction.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(
          interaction.organizationId(), interaction.candidateCompanyInteractionId())
          .orElseThrow(() -> new IllegalStateException(
              "candidate_company_interaction not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to create candidate_company_interaction", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<CandidateCompanyInteraction> findByIdAndOrganizationId(
      UUID organizationId, CandidateCompanyInteractionId interactionId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(interactionId, "interactionId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, interactionId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toInteraction(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find candidate_company_interaction by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<CandidateCompanyInteraction> findByCandidateAndCompany(
      UUID organizationId, CandidateId candidateId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement =
        connection.prepareStatement(FIND_BY_CANDIDATE_COMPANY_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateId.value());
      statement.setObject(3, companyId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<CandidateCompanyInteraction> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toInteraction(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find interactions by candidate and company", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<CandidateCompanyInteraction> findByJobId(UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_JOB_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<CandidateCompanyInteraction> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toInteraction(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find interactions by job", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static CandidateCompanyInteraction toInteraction(ResultSet rs) throws SQLException {
    OffsetDateTime endedAtOdt = rs.getObject("ended_at", OffsetDateTime.class);
    UUID jobIdValue = rs.getObject("job_id", UUID.class);
    return CandidateCompanyInteraction.builder()
        .candidateCompanyInteractionId(new CandidateCompanyInteractionId(
            rs.getObject("candidate_company_interaction_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .candidateId(new CandidateId(rs.getObject("candidate_id", UUID.class)))
        .companyId(new CompanyId(rs.getObject("company_id", UUID.class)))
        .jobId(jobIdValue != null ? new JobId(jobIdValue) : null)
        .interactionType(InteractionType.fromWireValue(rs.getString("interaction_type")))
        .status(InteractionStatus.fromWireValue(rs.getString("status")))
        .startedAt(rs.getObject("started_at", OffsetDateTime.class).toInstant())
        .endedAt(endedAtOdt != null ? endedAtOdt.toInstant() : null)
        .notes(rs.getString("notes"))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
