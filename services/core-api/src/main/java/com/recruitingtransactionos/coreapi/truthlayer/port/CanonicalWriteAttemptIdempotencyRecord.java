package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;

public record CanonicalWriteAttemptIdempotencyRecord(
    CanonicalWriteAttemptId attemptId) {

  public CanonicalWriteAttemptIdempotencyRecord {
    Objects.requireNonNull(attemptId, "attemptId must not be null");
  }
}
