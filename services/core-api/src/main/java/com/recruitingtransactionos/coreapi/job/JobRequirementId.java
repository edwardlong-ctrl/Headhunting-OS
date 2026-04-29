package com.recruitingtransactionos.coreapi.job;

import java.util.Objects;
import java.util.UUID;

public record JobRequirementId(UUID value) {

  public JobRequirementId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
