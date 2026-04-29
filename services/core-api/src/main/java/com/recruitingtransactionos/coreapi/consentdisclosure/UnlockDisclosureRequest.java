package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record UnlockDisclosureRequest(
    UUID organizationId,
    String candidateRef,
    String candidateProfileRef,
    String jobRef,
    String clientRef,
    PortalRole requestedByRole,
    DisclosureLevel requestedLevel,
    Optional<ConsentRecord> consentRecord,
    Optional<DisclosureRecord> disclosureRecord,
    Optional<UnlockDecision> unlockDecision,
    Optional<DisclosureAuditBoundary> auditBoundary,
    Instant requestedAt) {

  public UnlockDisclosureRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    requireNonBlank(candidateRef, "candidateRef");
    requireNonBlank(candidateProfileRef, "candidateProfileRef");
    requireNonBlank(jobRef, "jobRef");
    requireNonBlank(clientRef, "clientRef");
    Objects.requireNonNull(requestedByRole, "requestedByRole must not be null");
    Objects.requireNonNull(requestedLevel, "requestedLevel must not be null");
    consentRecord = Objects.requireNonNull(consentRecord, "consentRecord must not be null");
    disclosureRecord = Objects.requireNonNull(disclosureRecord,
        "disclosureRecord must not be null");
    unlockDecision = Objects.requireNonNull(unlockDecision, "unlockDecision must not be null");
    auditBoundary = Objects.requireNonNull(auditBoundary, "auditBoundary must not be null");
    Objects.requireNonNull(requestedAt, "requestedAt must not be null");
  }

  private static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
