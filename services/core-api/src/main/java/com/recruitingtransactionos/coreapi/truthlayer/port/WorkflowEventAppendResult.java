package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;

public record WorkflowEventAppendResult(WorkflowEventId workflowEventId) {

  public WorkflowEventAppendResult {
    Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
  }
}
