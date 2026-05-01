package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

public record AITaskProviderResponse(
    JsonNode outputPayload,
    String toolCallsJson,
    BigDecimal costUnits,
    String traceRef,
    Duration latency) {

  public AITaskProviderResponse {
    Objects.requireNonNull(outputPayload, "outputPayload must not be null");
    Objects.requireNonNull(latency, "latency must not be null");
  }
}
