package com.recruitingtransactionos.coreapi.matching;

public enum AuthenticityRiskLevel {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  UNKNOWN("unknown");

  private final String wireValue;

  AuthenticityRiskLevel(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
