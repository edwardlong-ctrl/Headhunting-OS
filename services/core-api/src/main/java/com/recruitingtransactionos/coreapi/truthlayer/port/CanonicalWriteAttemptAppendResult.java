package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;

public record CanonicalWriteAttemptAppendResult(CanonicalWriteAttemptId attemptId) {

  public CanonicalWriteAttemptAppendResult {
    Objects.requireNonNull(attemptId, "attemptId must not be null");
  }
}
