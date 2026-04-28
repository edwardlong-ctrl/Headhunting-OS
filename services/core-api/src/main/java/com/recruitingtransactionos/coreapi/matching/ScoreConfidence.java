package com.recruitingtransactionos.coreapi.matching;

public enum ScoreConfidence {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high");

  private final String wireValue;

  ScoreConfidence(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
