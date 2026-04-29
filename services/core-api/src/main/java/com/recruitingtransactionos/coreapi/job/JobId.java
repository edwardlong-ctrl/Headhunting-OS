package com.recruitingtransactionos.coreapi.job;

import java.util.Objects;
import java.util.UUID;

public record JobId(UUID value) {

  public JobId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
