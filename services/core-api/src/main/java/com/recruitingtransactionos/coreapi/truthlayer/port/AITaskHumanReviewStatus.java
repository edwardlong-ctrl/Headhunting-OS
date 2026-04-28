package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Locale;
import java.util.Optional;

public enum AITaskHumanReviewStatus {
  NOT_REQUIRED("not_required"),
  REQUIRED("required"),
  PENDING("pending"),
  APPROVED("approved"),
  REJECTED("rejected"),
  NEEDS_REVISION("needs_revision"),
  EXPIRED("expired");

  private final String wireValue;

  AITaskHumanReviewStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static Optional<AITaskHumanReviewStatus> fromWireValue(String wireValue) {
    if (wireValue == null || wireValue.isBlank()) {
      return Optional.empty();
    }
    String normalized = wireValue.strip().toLowerCase(Locale.ROOT);
    for (AITaskHumanReviewStatus status : values()) {
      if (status.wireValue.equals(normalized)) {
        return Optional.of(status);
      }
    }
    return Optional.empty();
  }
}
