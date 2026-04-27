package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;

public record WorkflowEventIdempotencyRecord(
    WorkflowEventId workflowEventId,
    WorkflowEventAppendCommand command) {

  public WorkflowEventIdempotencyRecord {
    Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    Objects.requireNonNull(command, "command must not be null");
  }
}
