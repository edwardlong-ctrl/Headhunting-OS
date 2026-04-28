package com.recruitingtransactionos.coreapi.candidateprofile;

import java.time.Instant;
import java.util.Objects;

public record CandidateProfileFieldStaleness(
    boolean stale,
    String staleReason,
    Instant observedAt,
    Instant lastConfirmedAt,
    Instant reviewBy,
    Instant detectedAt) {

  public CandidateProfileFieldStaleness {
    staleReason = CandidateProfileGuards.optionalNonBlank(staleReason, "staleReason");
    if (stale && staleReason == null) {
      throw new IllegalArgumentException("stale field requires staleReason");
    }
    Objects.requireNonNull(detectedAt, "detectedAt must not be null");
    if (observedAt != null
        && lastConfirmedAt != null
        && lastConfirmedAt.isBefore(observedAt)) {
      throw new IllegalArgumentException("lastConfirmedAt must not be before observedAt");
    }
    if (lastConfirmedAt != null && detectedAt.isBefore(lastConfirmedAt)) {
      throw new IllegalArgumentException("detectedAt must not be before lastConfirmedAt");
    }
    if (reviewBy != null && reviewBy.isBefore(detectedAt)) {
      throw new IllegalArgumentException("reviewBy must not be before detectedAt");
    }
  }
}
