package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;
import java.util.UUID;

public record WorkflowCausationId(UUID value) {

  public static final int MAX_WIRE_LENGTH = 64;

  public WorkflowCausationId {
    Objects.requireNonNull(value, "causationId must not be null");
  }

  public static WorkflowCausationId fromWireValue(String value) {
    return new WorkflowCausationId(
        PortContractGuards.requireUuidWireValue(value, "causationId", MAX_WIRE_LENGTH));
  }

  public String wireValue() {
    return value.toString();
  }
}
