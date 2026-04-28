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
  }
}
