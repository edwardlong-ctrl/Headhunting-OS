package com.recruitingtransactionos.coreapi.consentdisclosure.persistence;

import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureWorkflowEntityIds;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.CandidateWorkflowStatePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcCandidateWorkflowStatePort implements CandidateWorkflowStatePort {

  private static final String UPSERT_SQL = """
      INSERT INTO recruiting.candidate (
        candidate_id,
        organization_id,
        status,
        current_profile_id,
        owner_consultant_id,
        merged_into_candidate_id,
        last_activity_at,
        default_industry_pack_id,
        identity_fingerprint_hash,
        metadata,
        created_by,
        updated_by
      )
      VALUES (
        ?,
        ?,
        'identity_disclosed',
        NULL,
        NULL,
        NULL,
        ?,
        NULL,
        NULL,
        jsonb_build_object('candidate_ref', ?, 'workflow_anchor', 'consent_disclosure'),
        NULL,
        NULL
      )
      ON CONFLICT (candidate_id) DO UPDATE
      SET status = 'identity_disclosed',
          last_activity_at = EXCLUDED.last_activity_at,
          metadata = jsonb_set(
              COALESCE(recruiting.candidate.metadata, '{}'::jsonb),
              '{candidate_ref}',
              to_jsonb(?::text),
              true),
          updated_at = now(),
          version = recruiting.candidate.version + 1
      """;

  private final DataSource dataSource;

  public JdbcCandidateWorkflowStatePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public void transitionToIdentityDisclosed(
      UUID organizationId,
      String candidateRef,
      Instant disclosedAt) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    Objects.requireNonNull(disclosedAt, "disclosedAt must not be null");
    UUID candidateId =
        ConsentDisclosureWorkflowEntityIds.candidateEntityId(organizationId, candidateRef);
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
      statement.setObject(1, candidateId);
      statement.setObject(2, organizationId);
      statement.setObject(3, OffsetDateTime.ofInstant(disclosedAt, ZoneOffset.UTC));
      statement.setString(4, candidateRef);
      statement.setString(5, candidateRef);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to transition candidate workflow state", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
