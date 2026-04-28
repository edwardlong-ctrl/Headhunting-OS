package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.util.Objects;

public record CanonicalWriteResult(
    CanonicalWriteDecision decision,
    boolean workflowEventAppended,
    WorkflowEventId workflowEventId,
    boolean canonicalPersistencePerformed,
    String canonicalPersistenceStatus) {

  public CanonicalWriteResult {
    Objects.requireNonNull(decision, "decision must not be null");
    if (workflowEventAppended) {
      Objects.requireNonNull(workflowEventId,
          "workflowEventId must not be null when workflowEventAppended is true");
    }
    canonicalPersistenceStatus = requireNonBlank(
        canonicalPersistenceStatus,
        "canonicalPersistenceStatus");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
