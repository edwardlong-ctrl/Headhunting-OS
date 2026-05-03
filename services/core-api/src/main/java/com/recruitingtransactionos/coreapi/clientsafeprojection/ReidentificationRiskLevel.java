package com.recruitingtransactionos.coreapi.clientsafeprojection;

public enum ReidentificationRiskLevel {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high");

  private final String wireValue;

  ReidentificationRiskLevel(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static ReidentificationRiskLevel fromWireValue(String wireValue) {
    if (wireValue == null) {
      throw new NullPointerException("wireValue must not be null");
    }
    for (ReidentificationRiskLevel level : values()) {
      if (level.wireValue.equals(wireValue)) {
        return level;
      }
    }
    throw new IllegalArgumentException("unknown re-identification risk level: " + wireValue);
  }
}
