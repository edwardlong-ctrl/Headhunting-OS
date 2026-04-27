package com.recruitingtransactionos.coreapi.governedintake;

public record IntakeExtractionFinding(
    String code,
    String message,
    SourceItemId sourceItemId) {

  public IntakeExtractionFinding {
    code = GovernedIntakeGuards.requireNonBlank(code, "code");
    message = GovernedIntakeGuards.requireNonBlank(message, "message");
  }
}
