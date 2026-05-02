package com.recruitingtransactionos.coreapi.workflowaudit;

import java.util.Objects;

public record WorkflowTransitionBlocker(
    String code,
    String safeReason) {

  public WorkflowTransitionBlocker {
    code = Objects.requireNonNull(code, "code must not be null").strip();
    safeReason = Objects.requireNonNull(safeReason, "safeReason must not be null").strip();
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (safeReason.isBlank()) {
      throw new IllegalArgumentException("safeReason must not be blank");
    }
  }
}
