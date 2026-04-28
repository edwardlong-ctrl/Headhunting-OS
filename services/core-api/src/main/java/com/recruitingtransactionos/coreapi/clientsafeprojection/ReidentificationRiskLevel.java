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
}
