package com.recruitingtransactionos.coreapi.consentdisclosure.persistence;

import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureWorkflowEntityIds;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import java.util.Arrays;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcConsentRecordPort implements ConsentRecordPort {

  private static final String INSERT_SQL = """
      INSERT INTO privacy.consent_record (
        consent_record_ref,
        organization_id,
        workflow_entity_id,
        candidate_ref,
        candidate_profile_ref,
        job_ref,
        profile_version,
        consent_text_version,
        status,
        permitted_disclosure_levels,
        confirmed_at,
        expires_at,
        revoked
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::text[], ?, ?, ?)
      """;

  private static final String FIND_SQL = """
      SELECT
        consent_record_ref,
        organization_id,
        candidate_ref,
        candidate_profile_ref,
        job_ref,
        profile_version,
        consent_text_version,
        status,
        permitted_disclosure_levels,
        confirmed_at,
        expires_at,
        revoked
      FROM privacy.consent_record
      WHERE organization_id = ?
        AND consent_record_ref = ?
      """;

  private static final String FIND_BY_WORKFLOW_ENTITY_ID_SQL = """
      SELECT
        consent_record_ref,
        organization_id,
        candidate_ref,
        candidate_profile_ref,
        job_ref,
        profile_version,
        consent_text_version,
        status,
        permitted_disclosure_levels,
        confirmed_at,
        expires_at,
        revoked
      FROM privacy.consent_record
      WHERE organization_id = ?
        AND workflow_entity_id = ?
      """;

  private final DataSource dataSource;

  public JdbcConsentRecordPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public ConsentRecord append(ConsentRecord consentRecord) {
    Objects.requireNonNull(consentRecord, "consentRecord must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setString(1, consentRecord.consentRecordRef());
      statement.setObject(2, consentRecord.organizationId());
      statement.setObject(3, ConsentDisclosureWorkflowEntityIds.consentEntityId(
          consentRecord.organizationId(),
          consentRecord.consentRecordRef()));
      statement.setString(4, consentRecord.candidateRef());
      statement.setString(5, consentRecord.candidateProfileRef());
      statement.setString(6, consentRecord.jobRef());
      statement.setString(7, consentRecord.profileVersion());
      statement.setString(8, consentRecord.consentTextVersion());
      statement.setString(9, consentRecord.status().wireValue());
      statement.setArray(10, connection.createArrayOf(
          "text",
          consentRecord.permittedDisclosureLevels().stream()
              .map(DisclosureLevel::wireValue)
              .toArray(String[]::new)));
      statement.setObject(11, OffsetDateTime.ofInstant(consentRecord.confirmedAt(), ZoneOffset.UTC));
      if (consentRecord.expiresAt() == null) {
        statement.setNull(12, Types.TIMESTAMP_WITH_TIMEZONE);
      } else {
        statement.setObject(12, OffsetDateTime.ofInstant(consentRecord.expiresAt(), ZoneOffset.UTC));
      }
      statement.setBoolean(13, consentRecord.revoked());
      statement.executeUpdate();
      return consentRecord;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append consent record", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<ConsentRecord> findByRefAndOrganizationId(UUID organizationId, String consentRecordRef) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(consentRecordRef, "consentRecordRef must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, consentRecordRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapConsentRecord(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find consent record", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<ConsentRecord> findByWorkflowEntityId(UUID organizationId, UUID workflowEntityId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(workflowEntityId, "workflowEntityId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_WORKFLOW_ENTITY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, workflowEntityId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(mapConsentRecord(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find consent record by workflow entity id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static ConsentRecord mapConsentRecord(ResultSet resultSet) throws SQLException {
    return new ConsentRecord(
        resultSet.getString("consent_record_ref"),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("candidate_ref"),
        resultSet.getString("candidate_profile_ref"),
        resultSet.getString("job_ref"),
        resultSet.getString("profile_version"),
        resultSet.getString("consent_text_version"),
        ConsentStatus.fromWireValue(resultSet.getString("status")),
        Arrays.stream((String[]) resultSet.getArray("permitted_disclosure_levels").getArray())
            .map(DisclosureLevel::fromWireValue)
            .collect(java.util.stream.Collectors.toSet()),
        resultSet.getObject("confirmed_at", OffsetDateTime.class).toInstant(),
        optionalInstant(resultSet, "expires_at"),
        resultSet.getBoolean("revoked"));
  }

  private static java.time.Instant optionalInstant(ResultSet resultSet, String column)
      throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }
}
