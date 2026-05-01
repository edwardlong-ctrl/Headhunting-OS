package com.recruitingtransactionos.coreapi.aitaskrunner;

import java.util.Objects;

public final class AITaskSchemaValidationException extends RuntimeException {

  private final String label;
  private final String errorCode;
  private final String safeSummary;

  public AITaskSchemaValidationException(String label, String safeSummary) {
    super(buildMessage(label, safeSummary));
    this.label = Objects.requireNonNull(label, "label must not be null");
    this.errorCode = label + "_schema_validation_failed";
    this.safeSummary = Objects.requireNonNull(safeSummary, "safeSummary must not be null");
  }

  public String label() {
    return label;
  }

  public String errorCode() {
    return errorCode;
  }

  public String safeSummary() {
    return safeSummary;
  }

  private static String buildMessage(String label, String safeSummary) {
    Objects.requireNonNull(label, "label must not be null");
    Objects.requireNonNull(safeSummary, "safeSummary must not be null");
    return label + "_schema_validation_failed: " + safeSummary;
  }
}
