package com.recruitingtransactionos.coreapi.consentdisclosure.persistence;

import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureWorkflowEntityIds;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcDisclosureRecordPort implements DisclosureRecordPort {

  private static final String INSERT_SQL = """
      INSERT INTO privacy.disclosure_record (
        disclosure_record_ref,
        organization_id,
        workflow_entity_id,
        candidate_ref,
        candidate_profile_ref,
        job_ref,
        client_ref,
        status,
        disclosure_level,
        redaction_level,
        unlock_decision_ref,
        consent_record_ref,
        workflow_event_id,
        decided_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String INSERT_IF_ABSENT_SQL = INSERT_SQL + """
      ON CONFLICT (organization_id, disclosure_record_ref) DO NOTHING
      """;

  private static final String FIND_SQL = """
      SELECT
        disclosure_record_ref,
        organization_id,
        candidate_ref,
        candidate_profile_ref,
        job_ref,
        client_ref,
        status,
        disclosure_level,
        redaction_level,
        unlock_decision_ref,
        consent_record_ref,
        workflow_event_id,
        decided_at
      FROM privacy.disclosure_record
      WHERE organization_id = ?
        AND disclosure_record_ref = ?
      """;

  private static final String FIND_BY_WORKFLOW_ENTITY_ID_SQL = """
      SELECT
        disclosure_record_ref,
        organization_id,
        candidate_ref,
        candidate_profile_ref,
        job_ref,
        client_ref,
        status,
        disclosure_level,
        redaction_level,
        unlock_decision_ref,
        consent_record_ref,
        workflow_event_id,
        decided_at
      FROM privacy.disclosure_record
      WHERE organization_id = ?
        AND workflow_entity_id = ?
      """;

  private static final String TRANSITION_TO_IDENTITY_DISCLOSED_SQL = """
      UPDATE privacy.disclosure_record
      SET status = ?,
          workflow_event_id = ?,
          decided_at = ?
      WHERE organization_id = ?
        AND disclosure_record_ref = ?
      """;

  private final DataSource dataSource;

  public JdbcDisclosureRecordPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public DisclosureRecord appendIfAbsent(DisclosureRecord disclosureRecord) {
    Objects.requireNonNull(disclosureRecord, "disclosureRecord must not be null");
    executeInsert(disclosureRecord, INSERT_IF_ABSENT_SQL);
    return findByRefAndOrganizationId(
        disclosureRecord.organizationId(),
        disclosureRecord.disclosureRecordRef())
        .orElseThrow(() -> new IllegalStateException(
            "Failed to append disclosure record"));
  }

  @Override
  public DisclosureRecord append(DisclosureRecord disclosureRecord) {
    Objects.requireNonNull(disclosureRecord, "disclosureRecord must not be null");
    executeInsert(disclosureRecord, INSERT_SQL);
    return disclosureRecord;
  }

  private void executeInsert(DisclosureRecord disclosureRecord, String sql) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, disclosureRecord.disclosureRecordRef());
      statement.setObject(2, disclosureRecord.organizationId());
      statement.setObject(3, ConsentDisclosureWorkflowEntityIds.disclosureEntityId(
          disclosureRecord.organizationId(),
          disclosureRecord.disclosureRecordRef()));
      statement.setString(4, disclosureRecord.candidateRef());
      statement.setString(5, disclosureRecord.candidateProfileRef());
      statement.setString(6, disclosureRecord.jobRef());
      statement.setString(7, disclosureRecord.clientRef());
      statement.setString(8, disclosureRecord.status().wireValue());
      statement.setString(9, disclosureRecord.disclosureLevel().wireValue());
      statement.setString(10, disclosureRecord.redactionLevel().wireValue());
      statement.setString(11, disclosureRecord.unlockDecisionRef());
      statement.setString(12, disclosureRecord.consentRecordRef());
      if (disclosureRecord.workflowEventId().isPresent()) {
        statement.setObject(13, disclosureRecord.workflowEventId().orElseThrow().value());
      } else {
        statement.setNull(13, Types.OTHER);
      }
      statement.setObject(14, OffsetDateTime.ofInstant(disclosureRecord.decidedAt(), ZoneOffset.UTC));
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append disclosure record", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<DisclosureRecord> findByRefAndOrganizationId(
      UUID organizationId,
      String disclosureRecordRef) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(disclosureRecordRef, "disclosureRecordRef must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, disclosureRecordRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        UUID workflowEventId = resultSet.getObject("workflow_event_id", UUID.class);
        return Optional.of(mapDisclosureRecord(resultSet, workflowEventId));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find disclosure record", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<DisclosureRecord> findByWorkflowEntityId(
      UUID organizationId,
      UUID workflowEntityId) {
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
        UUID workflowEventId = resultSet.getObject("workflow_event_id", UUID.class);
        return Optional.of(mapDisclosureRecord(resultSet, workflowEventId));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find disclosure record by workflow entity id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public DisclosureRecord transitionToIdentityDisclosed(
      UUID organizationId,
      String disclosureRecordRef,
      WorkflowEventId workflowEventId,
      Instant decidedAt) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(disclosureRecordRef, "disclosureRecordRef must not be null");
    Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    Objects.requireNonNull(decidedAt, "decidedAt must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement =
        connection.prepareStatement(TRANSITION_TO_IDENTITY_DISCLOSED_SQL)) {
      statement.setString(1, DisclosureStatus.IDENTITY_DISCLOSED.wireValue());
      statement.setObject(2, workflowEventId.value());
      statement.setObject(3, OffsetDateTime.ofInstant(decidedAt, ZoneOffset.UTC));
      statement.setObject(4, organizationId);
      statement.setString(5, disclosureRecordRef);
      statement.executeUpdate();
      return findByRefAndOrganizationId(organizationId, disclosureRecordRef)
          .orElseThrow(() -> new IllegalStateException(
              "Failed to transition disclosure record to identity_disclosed"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to transition disclosure record", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static DisclosureRecord mapDisclosureRecord(ResultSet resultSet, UUID workflowEventId)
      throws SQLException {
    return new DisclosureRecord(
        resultSet.getString("disclosure_record_ref"),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("candidate_ref"),
        resultSet.getString("candidate_profile_ref"),
        resultSet.getString("job_ref"),
        resultSet.getString("client_ref"),
        DisclosureStatus.fromWireValue(resultSet.getString("status")),
        DisclosureLevel.fromWireValue(resultSet.getString("disclosure_level")),
        RedactionLevel.fromWireValue(resultSet.getString("redaction_level")),
        resultSet.getString("unlock_decision_ref"),
        resultSet.getString("consent_record_ref"),
        workflowEventId == null
            ? Optional.empty()
            : Optional.of(new WorkflowEventId(workflowEventId)),
        resultSet.getObject("decided_at", OffsetDateTime.class).toInstant());
  }
}
