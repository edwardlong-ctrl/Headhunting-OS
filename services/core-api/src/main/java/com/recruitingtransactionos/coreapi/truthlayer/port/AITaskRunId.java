package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;
import java.util.UUID;

public record AITaskRunId(UUID value) {

  public AITaskRunId {
    Objects.requireNonNull(value, "aiTaskRunId must not be null");
  }
}
