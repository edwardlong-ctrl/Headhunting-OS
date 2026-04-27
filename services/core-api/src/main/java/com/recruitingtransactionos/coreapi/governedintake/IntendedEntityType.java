package com.recruitingtransactionos.coreapi.governedintake;

public enum IntendedEntityType {
  CANDIDATE("CANDIDATE"),
  COMPANY("COMPANY"),
  JOB("JOB"),
  INTERVIEW("INTERVIEW"),
  SHORTLIST("SHORTLIST"),
  UNKNOWN("UNKNOWN");

  private final String wireValue;

  IntendedEntityType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static IntendedEntityType fromWireValue(String wireValue) {
    for (IntendedEntityType value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown intended entity type: " + wireValue);
  }
}
