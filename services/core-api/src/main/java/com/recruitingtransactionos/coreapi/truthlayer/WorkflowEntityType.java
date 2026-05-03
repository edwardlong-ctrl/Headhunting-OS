package com.recruitingtransactionos.coreapi.truthlayer;

import java.util.Arrays;
import java.util.Locale;

public enum WorkflowEntityType {
  JOB,
  CANDIDATE,
  SHORTLIST,
  CONSENT,
  DISCLOSURE,
  PLACEMENT,
  COMMISSION,
  CLAIM_LEDGER_ITEM,
  REVIEW_EVENT,
  AI_TASK_RUN,
  CANONICAL_WRITE,
  INFORMATION_PACKET,
  SOURCE_ITEM,
  REIDENTIFICATION_ASSESSMENT;

  public String wireValue() {
    return name();
  }

  public static WorkflowEntityType fromWireValue(String value) {
    if (value == null) {
      throw new NullPointerException("workflow entity type must not be null");
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException("workflow entity type must not be blank");
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(entityType -> entityType.wireValue().equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "unknown workflow entity type: " + value));
  }
}
