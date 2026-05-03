package com.recruitingtransactionos.coreapi.privacyredaction;

import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessment;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskFeature;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistence-side wrapper for a {@link ReidentificationRiskAssessment}.
 *
 * <p>This record carries the organization scope, the persistence reference,
 * the optional workflow event id linkage, and the recorded-at timestamp —
 * none of which belong on the pure {@link ReidentificationRiskAssessment}
 * value object that lives in the
 * {@code clientsafeprojection} package.
 *
 * <p>This record is the row representation of
 * {@code privacy.reidentification_risk_assessment} (Flyway V25).
 */
public record PersistedReidentificationRiskAssessment(
    String reidentificationRiskAssessmentRef,
    UUID organizationId,
    String candidateRef,
    String jobRef,
    ReidentificationRiskAssessment assessment,
    Optional<WorkflowEventId> workflowEventId,
    Instant recordedAt) {

  public PersistedReidentificationRiskAssessment {
    Objects.requireNonNull(reidentificationRiskAssessmentRef,
        "reidentificationRiskAssessmentRef must not be null");
    if (reidentificationRiskAssessmentRef.isBlank()) {
      throw new IllegalArgumentException(
          "reidentificationRiskAssessmentRef must not be blank");
    }
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(assessment, "assessment must not be null");
    Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    if (candidateRef != null && candidateRef.isBlank()) {
      throw new IllegalArgumentException("candidateRef must not be blank when provided");
    }
    if (jobRef != null && jobRef.isBlank()) {
      throw new IllegalArgumentException("jobRef must not be blank when provided");
    }
  }

  public Set<ReidentificationRiskFeature> unsafeFeatures() {
    return assessment.unsafeFeatures();
  }
}
