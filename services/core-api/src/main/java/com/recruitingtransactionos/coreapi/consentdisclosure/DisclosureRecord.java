package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record DisclosureRecord(
    String disclosureRecordRef,
    UUID organizationId,
    String candidateRef,
    String candidateProfileRef,
    String jobRef,
    String clientRef,
    DisclosureStatus status,
    DisclosureLevel disclosureLevel,
    RedactionLevel redactionLevel,
    String unlockDecisionRef,
    String consentRecordRef,
    Optional<WorkflowEventId> workflowEventId,
    Instant decidedAt) {

  public DisclosureRecord {
    requireNonBlank(disclosureRecordRef, "disclosureRecordRef");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    requireNonBlank(candidateRef, "candidateRef");
    requireNonBlank(candidateProfileRef, "candidateProfileRef");
    requireNonBlank(jobRef, "jobRef");
    requireNonBlank(clientRef, "clientRef");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(disclosureLevel, "disclosureLevel must not be null");
    Objects.requireNonNull(redactionLevel, "redactionLevel must not be null");
    requireNonBlank(unlockDecisionRef, "unlockDecisionRef");
    requireNonBlank(consentRecordRef, "consentRecordRef");
    workflowEventId = Objects.requireNonNull(workflowEventId,
        "workflowEventId must not be null");
    Objects.requireNonNull(decidedAt, "decidedAt must not be null");
  }

  public boolean isApprovedFor(DisclosureLevel requestedLevel) {
    Objects.requireNonNull(requestedLevel, "requestedLevel must not be null");
    return status == DisclosureStatus.APPROVED
        && disclosureLevel == requestedLevel
        && redactionLevel == RedactionLevel.L4_IDENTITY_DISCLOSED;
  }

  private static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
