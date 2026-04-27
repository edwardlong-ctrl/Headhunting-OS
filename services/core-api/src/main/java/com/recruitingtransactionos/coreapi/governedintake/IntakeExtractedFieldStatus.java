package com.recruitingtransactionos.coreapi.governedintake;

public enum IntakeExtractedFieldStatus {
  PLACEHOLDER("PLACEHOLDER"),
  EMPTY("EMPTY"),
  UNSUPPORTED_SOURCE_TYPE("UNSUPPORTED_SOURCE_TYPE"),
  NEEDS_FUTURE_AI("NEEDS_FUTURE_AI"),
  INVALID_SOURCE_REFERENCE("INVALID_SOURCE_REFERENCE");

  private final String wireValue;

  IntakeExtractedFieldStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static IntakeExtractedFieldStatus fromWireValue(String wireValue) {
    for (IntakeExtractedFieldStatus value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown intake extracted field status: " + wireValue);
  }
}
