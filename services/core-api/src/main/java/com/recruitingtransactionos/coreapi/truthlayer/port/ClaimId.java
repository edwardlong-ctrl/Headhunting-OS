package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;
import java.util.UUID;

public record ClaimId(UUID value) {

  public ClaimId {
    Objects.requireNonNull(value, "claimId must not be null");
  }
}
