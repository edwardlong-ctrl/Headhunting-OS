package com.recruitingtransactionos.coreapi.truthlayer;

public enum AssertionStrength {
  EXPLICIT("explicit"),
  IMPLIED("implied"),
  WEAK_SIGNAL("weak_signal"),
  CONTRADICTION("contradiction"),
  UNKNOWN("unknown");

  private final String wireValue;

  AssertionStrength(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
