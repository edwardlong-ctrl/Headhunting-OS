package com.recruitingtransactionos.coreapi.workflowautomation;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Duration;

public record WorkflowAutomationRule(
    String key,
    String label,
    String workflowFamily,
    String actionCode,
    PortalRole ownerRole,
    Duration dueAfter,
    Duration reminderAfter,
    Duration escalationAfter,
    String nextBestAction,
    String blockerCode) {

  public WorkflowAutomationRule {
    key = requireNonBlank(key, "key");
    label = requireNonBlank(label, "label");
    workflowFamily = requireNonBlank(workflowFamily, "workflowFamily");
    actionCode = requireNonBlank(actionCode, "actionCode");
    if (ownerRole == null) {
      throw new NullPointerException("ownerRole must not be null");
    }
    if (dueAfter == null || dueAfter.isNegative() || dueAfter.isZero()) {
      throw new IllegalArgumentException("dueAfter must be positive");
    }
    if (reminderAfter == null || reminderAfter.isNegative() || reminderAfter.isZero()) {
      throw new IllegalArgumentException("reminderAfter must be positive");
    }
    if (reminderAfter.compareTo(dueAfter) > 0) {
      throw new IllegalArgumentException("reminderAfter must be on or before dueAfter");
    }
    if (escalationAfter == null || escalationAfter.compareTo(dueAfter) < 0) {
      throw new IllegalArgumentException("escalationAfter must be on or after dueAfter");
    }
    nextBestAction = requireNonBlank(nextBestAction, "nextBestAction");
    blockerCode = requireNonBlank(blockerCode, "blockerCode");
  }

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
