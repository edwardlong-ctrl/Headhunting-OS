package com.recruitingtransactionos.coreapi.governedintake;

public enum IntakeExtractionStatus {
  CREATED("CREATED"),
  SUCCEEDED("SUCCEEDED"),
  FAILED("FAILED");

  private final String wireValue;

  IntakeExtractionStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static IntakeExtractionStatus fromWireValue(String wireValue) {
    for (IntakeExtractionStatus value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown intake extraction status: " + wireValue);
  }
}
