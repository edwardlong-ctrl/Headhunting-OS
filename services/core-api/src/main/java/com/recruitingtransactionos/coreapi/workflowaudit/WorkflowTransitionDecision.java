package com.recruitingtransactionos.coreapi.workflowaudit;

import java.util.List;
import java.util.Objects;

public record WorkflowTransitionDecision(
    String actionCode,
    String currentStatus,
    String targetStatus,
    boolean allowed,
    List<WorkflowTransitionBlocker> blockers) {

  public WorkflowTransitionDecision {
    actionCode = Objects.requireNonNull(actionCode, "actionCode must not be null").strip();
    if (actionCode.isBlank()) {
      throw new IllegalArgumentException("actionCode must not be blank");
    }
    blockers = List.copyOf(blockers == null ? List.of() : blockers);
  }

  public static WorkflowTransitionDecision allowed(
      String actionCode,
      String currentStatus,
      String targetStatus) {
    return new WorkflowTransitionDecision(actionCode, currentStatus, targetStatus, true, List.of());
  }

  public static WorkflowTransitionDecision blocked(
      String actionCode,
      String currentStatus,
      String targetStatus,
      List<WorkflowTransitionBlocker> blockers) {
    return new WorkflowTransitionDecision(actionCode, currentStatus, targetStatus, false, blockers);
  }

  public boolean hasBlocker(String code) {
    return blockers.stream().anyMatch(blocker -> blocker.code().equals(code));
  }
}
