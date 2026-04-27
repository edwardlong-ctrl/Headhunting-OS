package com.recruitingtransactionos.coreapi.truthlayer;

public enum ClaimType {
  FACT("fact"),
  PREFERENCE("preference"),
  INTENT("intent"),
  RISK("risk"),
  INFERENCE("inference"),
  PREDICTION("prediction");

  private final String wireValue;

  ClaimType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
