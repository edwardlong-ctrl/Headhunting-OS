package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.interaction.InteractionStatus;
import com.recruitingtransactionos.coreapi.interaction.InteractionType;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcConsentDisclosurePrerequisiteEvaluator
    implements ConsentDisclosurePrerequisiteEvaluator {

  private static final String JOB_GATE_SQL = """
      SELECT status, commercial_terms::text AS commercial_terms
      FROM recruiting.job
      WHERE organization_id = ?
        AND (
          job_id::text = ?
          OR metadata->>'job_ref' = ?
          OR metadata->>'jobRef' = ?
        )
      ORDER BY created_at DESC
      LIMIT 1
      """;

  private static final String PRIOR_INTERACTION_SQL = """
      SELECT COUNT(*) AS interaction_count
      FROM recruiting.candidate_company_interaction
      WHERE organization_id = ?
        AND interaction_type = ?
        AND status IN (?, ?)
        AND (
          metadata->>'candidate_ref' = ?
          OR metadata->>'candidateRef' = ?
        )
        AND (
          metadata->>'client_ref' = ?
          OR metadata->>'clientRef' = ?
          OR metadata->>'company_ref' = ?
          OR metadata->>'companyRef' = ?
        )
        AND (
          job_id::text = ?
          OR metadata->>'job_ref' = ?
          OR metadata->>'jobRef' = ?
        )
      """;

  private final DataSource dataSource;

  public JdbcConsentDisclosurePrerequisiteEvaluator(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public ConsentDisclosurePrerequisites evaluate(
      ConsentDisclosureServiceRequest request,
      Optional<UnlockDecision> unlockDecision,
      Optional<DisclosureRecord> disclosureRecord) {
    Objects.requireNonNull(request, "request must not be null");
    return new ConsentDisclosurePrerequisites(
        isJobActivated(request.organizationId(), request.jobRef()),
        isFeeAgreementActive(request.organizationId(), request.jobRef()),
        !hasPriorInteraction(
            request.organizationId(),
            request.candidateRef(),
            request.clientRef(),
            request.jobRef(),
            InteractionType.PRIOR_CONTACT),
        !hasPriorInteraction(
            request.organizationId(),
            request.candidateRef(),
            request.clientRef(),
            request.jobRef(),
            InteractionType.PRIOR_APPLICATION),
        isPrivacyRiskCleared(request.requestedLevel(), unlockDecision, disclosureRecord));
  }

  private boolean isJobActivated(UUID organizationId, String jobRef) {
    return readJobGate(organizationId, jobRef)
        .map(JobGateState::jobActivated)
        .orElse(false);
  }

  private boolean isFeeAgreementActive(UUID organizationId, String jobRef) {
    return readJobGate(organizationId, jobRef)
        .map(JobGateState::feeAgreementActive)
        .orElse(false);
  }

  private Optional<JobGateState> readJobGate(UUID organizationId, String jobRef) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(JOB_GATE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, jobRef);
      statement.setString(3, jobRef);
      statement.setString(4, jobRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        JobStatus status = JobStatus.fromWireValue(resultSet.getString("status"));
        String commercialTerms = resultSet.getString("commercial_terms");
        return Optional.of(new JobGateState(
            status == JobStatus.ACTIVATED,
            isCommercialTermsActive(commercialTerms)));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to evaluate disclosure job prerequisites", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private boolean hasPriorInteraction(
      UUID organizationId,
      String candidateRef,
      String clientRef,
      String jobRef,
      InteractionType interactionType) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(PRIOR_INTERACTION_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, interactionType.wireValue());
      statement.setString(3, InteractionStatus.ACTIVE.wireValue());
      statement.setString(4, InteractionStatus.COMPLETED.wireValue());
      statement.setString(5, candidateRef);
      statement.setString(6, candidateRef);
      statement.setString(7, clientRef);
      statement.setString(8, clientRef);
      statement.setString(9, clientRef);
      statement.setString(10, clientRef);
      statement.setString(11, jobRef);
      statement.setString(12, jobRef);
      statement.setString(13, jobRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong("interaction_count") > 0;
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to evaluate prior interaction prerequisites", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  static boolean isCommercialTermsActive(String commercialTerms) {
    if (commercialTerms == null) {
      return false;
    }
    String normalized = commercialTerms.trim();
    if (normalized.isEmpty() || "{}".equals(normalized) || "null".equalsIgnoreCase(normalized)) {
      return false;
    }
    String lower = normalized.toLowerCase();
    return lower.contains("\"active\":true")
        || lower.contains("\"status\":\"active\"")
        || lower.contains("\"feeagreementactive\":true")
        || lower.contains("\"fee_agreement_active\":true");
  }

  private static boolean isPrivacyRiskCleared(
      DisclosureLevel requestedLevel,
      Optional<UnlockDecision> unlockDecision,
      Optional<DisclosureRecord> disclosureRecord) {
    if (!requestedLevel.requiresUnlockAndDisclosure()) {
      return true;
    }
    if (unlockDecision.isEmpty() || disclosureRecord.isEmpty()) {
      return false;
    }
    DisclosureRecord disclosure = disclosureRecord.orElseThrow();
    return disclosure.status() == DisclosureStatus.APPROVED
        && disclosure.disclosureLevel() == requestedLevel
        && disclosure.redactionLevel() == RedactionLevel.L4_IDENTITY_DISCLOSED;
  }

  private record JobGateState(boolean jobActivated, boolean feeAgreementActive) {}
}
