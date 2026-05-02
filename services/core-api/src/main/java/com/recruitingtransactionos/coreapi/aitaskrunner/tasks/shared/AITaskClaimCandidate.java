package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.shared;

import java.util.Objects;

public record AITaskClaimCandidate(
    String fieldName,
    String fieldValue,
    String rationale,
    String evidenceQuote) {

  public AITaskClaimCandidate {
    fieldName = requireNonBlank(fieldName, "fieldName");
    fieldValue = requireNonBlank(fieldValue, "fieldValue");
    rationale = optionalNonBlank(rationale, "rationale");
    evidenceQuote = optionalNonBlank(evidenceQuote, "evidenceQuote");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static String optionalNonBlank(String value, String fieldName) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
