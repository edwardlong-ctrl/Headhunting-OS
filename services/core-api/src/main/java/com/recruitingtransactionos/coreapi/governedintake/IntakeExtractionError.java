package com.recruitingtransactionos.coreapi.governedintake;

public record IntakeExtractionError(
    String code,
    String message,
    SourceItemId sourceItemId) {

  public IntakeExtractionError {
    code = GovernedIntakeGuards.requireNonBlank(code, "code");
    message = GovernedIntakeGuards.requireNonBlank(message, "message");
  }
}
