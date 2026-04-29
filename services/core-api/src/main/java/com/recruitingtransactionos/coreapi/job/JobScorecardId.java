package com.recruitingtransactionos.coreapi.job;

import java.util.Objects;
import java.util.UUID;

public record JobScorecardId(UUID value) {

  public JobScorecardId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
