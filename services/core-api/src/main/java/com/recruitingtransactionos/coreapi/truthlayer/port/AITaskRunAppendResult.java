package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;

public record AITaskRunAppendResult(AITaskRunId aiTaskRunId) {

  public AITaskRunAppendResult {
    Objects.requireNonNull(aiTaskRunId, "aiTaskRunId must not be null");
  }
}
