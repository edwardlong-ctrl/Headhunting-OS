package com.recruitingtransactionos.coreapi.governedintake;

import java.util.Objects;

public record IntakeExtractedField(
    String fieldName,
    String fieldValue,
    SourceItemId sourceItemId,
    double deterministicConfidence,
    IntakeExtractedFieldStatus valueStatus,
    String notes) {

  public IntakeExtractedField {
    fieldName = GovernedIntakeGuards.requireNonBlank(fieldName, "fieldName");
    Objects.requireNonNull(fieldValue, "fieldValue must not be null");
    if (deterministicConfidence < 0.0d || deterministicConfidence > 1.0d) {
      throw new IllegalArgumentException(
          "deterministicConfidence must be between 0.0 and 1.0");
    }
    Objects.requireNonNull(valueStatus, "valueStatus must not be null");
    notes = GovernedIntakeGuards.optionalNonBlank(notes, "notes");
  }
}
