package com.recruitingtransactionos.coreapi.matching;

public enum EvidenceCoverageLevel {
  NONE("none"),
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  COMPLETE("complete");

  private final String wireValue;

  EvidenceCoverageLevel(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
