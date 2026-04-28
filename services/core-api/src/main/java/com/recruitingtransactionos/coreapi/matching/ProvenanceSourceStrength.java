package com.recruitingtransactionos.coreapi.matching;

public enum ProvenanceSourceStrength {
  HIGH_TRUST("high_trust"),
  MEDIUM_TRUST("medium_trust"),
  LOW_TRUST("low_trust"),
  UNKNOWN("unknown");

  private final String wireValue;

  ProvenanceSourceStrength(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
