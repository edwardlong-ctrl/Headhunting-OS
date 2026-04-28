package com.recruitingtransactionos.coreapi.matching;

public enum EvidenceAssertionStrength {
  EXPLICIT("explicit"),
  IMPLIED("implied"),
  WEAK_SIGNAL("weak_signal"),
  CONTRADICTION("contradiction"),
  UNKNOWN("unknown");

  private final String wireValue;

  EvidenceAssertionStrength(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
