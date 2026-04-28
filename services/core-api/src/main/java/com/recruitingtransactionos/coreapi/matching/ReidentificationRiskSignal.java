package com.recruitingtransactionos.coreapi.matching;

public enum ReidentificationRiskSignal {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  UNKNOWN("unknown");

  private final String wireValue;

  ReidentificationRiskSignal(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
