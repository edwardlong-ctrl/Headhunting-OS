package com.recruitingtransactionos.coreapi.governedintake;

public enum IntakeExtractionMode {
  DETERMINISTIC_PLACEHOLDER("DETERMINISTIC_PLACEHOLDER"),
  DOCUMENT_INTELLIGENCE_V1("DOCUMENT_INTELLIGENCE_V1");

  private final String wireValue;

  IntakeExtractionMode(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static IntakeExtractionMode fromWireValue(String wireValue) {
    for (IntakeExtractionMode value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown intake extraction mode: " + wireValue);
  }
}
