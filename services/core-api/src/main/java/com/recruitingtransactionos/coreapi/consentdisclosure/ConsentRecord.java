package com.recruitingtransactionos.coreapi.consentdisclosure;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ConsentRecord(
    String consentRecordRef,
    UUID organizationId,
    String candidateRef,
    String candidateProfileRef,
    String jobRef,
    String profileVersion,
    String consentTextVersion,
    ConsentStatus status,
    Set<DisclosureLevel> permittedDisclosureLevels,
    Instant confirmedAt,
    Instant expiresAt,
    boolean revoked) {

  public ConsentRecord {
    requireNonBlank(consentRecordRef, "consentRecordRef");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    requireNonBlank(candidateRef, "candidateRef");
    requireNonBlank(candidateProfileRef, "candidateProfileRef");
    requireNonBlank(jobRef, "jobRef");
    requireNonBlank(profileVersion, "profileVersion");
    requireNonBlank(consentTextVersion, "consentTextVersion");
    Objects.requireNonNull(status, "status must not be null");
    permittedDisclosureLevels = Set.copyOf(
        Objects.requireNonNull(permittedDisclosureLevels,
            "permittedDisclosureLevels must not be null"));
    Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
  }

  public boolean isConfirmedFor(DisclosureLevel requestedLevel, Instant requestedAt) {
    Objects.requireNonNull(requestedLevel, "requestedLevel must not be null");
    Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    return status == ConsentStatus.CONFIRMED
        && !revoked
        && !isExpiredAt(requestedAt)
        && permittedDisclosureLevels.contains(requestedLevel);
  }

  public boolean isExpiredAt(Instant requestedAt) {
    Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    return status == ConsentStatus.EXPIRED
        || (expiresAt != null && !expiresAt.isAfter(requestedAt));
  }

  private static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
