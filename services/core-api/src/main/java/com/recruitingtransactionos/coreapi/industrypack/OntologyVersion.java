package com.recruitingtransactionos.coreapi.industrypack;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OntologyVersion(
    UUID ontologyVersionId,
    IndustryPackId industryPackId,
    String versionKey,
    String source,
    String owner,
    Instant effectiveFrom,
    Instant reviewBy,
    Instant deprecatedAt) {

  public OntologyVersion {
    Objects.requireNonNull(ontologyVersionId, "ontologyVersionId must not be null");
    Objects.requireNonNull(industryPackId, "industryPackId must not be null");
    versionKey = requireNonBlank(versionKey, "versionKey");
    source = requireNonBlank(source, "source");
    owner = requireNonBlank(owner, "owner");
    Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
    Objects.requireNonNull(reviewBy, "reviewBy must not be null");
  }

  public boolean isStale(Instant asOf) {
    Instant comparison = asOf == null ? Instant.now() : asOf;
    return !reviewBy.isAfter(comparison) || deprecatedAt != null;
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    String normalized = value.strip();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
