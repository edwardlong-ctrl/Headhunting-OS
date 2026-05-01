package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import java.time.Duration;
import java.util.Objects;

public record AITaskExecutionResult(
    AITaskRunRecord runRecord,
    JsonNode outputPayload,
    Duration latency) {

  public AITaskExecutionResult {
    Objects.requireNonNull(runRecord, "runRecord must not be null");
    Objects.requireNonNull(outputPayload, "outputPayload must not be null");
    Objects.requireNonNull(latency, "latency must not be null");
  }
}
