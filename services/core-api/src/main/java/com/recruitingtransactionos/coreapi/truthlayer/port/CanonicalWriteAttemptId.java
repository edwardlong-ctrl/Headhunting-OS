package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;
import java.util.UUID;

public record CanonicalWriteAttemptId(UUID value) {

  public CanonicalWriteAttemptId {
    Objects.requireNonNull(value, "canonicalWriteAttemptId must not be null");
  }
}
