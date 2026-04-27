package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;
import java.util.UUID;

public record WorkflowEventId(UUID value) {

  public WorkflowEventId {
    Objects.requireNonNull(value, "workflowEventId must not be null");
  }
}
