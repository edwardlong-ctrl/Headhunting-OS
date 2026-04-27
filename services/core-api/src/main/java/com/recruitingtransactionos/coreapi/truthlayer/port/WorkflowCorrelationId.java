package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;
import java.util.UUID;

public record WorkflowCorrelationId(UUID value) {

  public static final int MAX_WIRE_LENGTH = 64;

  public WorkflowCorrelationId {
    Objects.requireNonNull(value, "correlationId must not be null");
  }

  public static WorkflowCorrelationId fromWireValue(String value) {
    return new WorkflowCorrelationId(
        PortContractGuards.requireUuidWireValue(value, "correlationId", MAX_WIRE_LENGTH));
  }

  public String wireValue() {
    return value.toString();
  }
}
