package com.recruitingtransactionos.coreapi.clientsafeprojection;

public enum ReidentificationRiskDecision {
  ALLOW("allow"),
  GENERALIZE("generalize"),
  REVIEW("review"),
  BLOCK("block");

  private final String wireValue;

  ReidentificationRiskDecision(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static ReidentificationRiskDecision fromWireValue(String wireValue) {
    if (wireValue == null) {
      throw new NullPointerException("wireValue must not be null");
    }
    for (ReidentificationRiskDecision decision : values()) {
      if (decision.wireValue.equals(wireValue)) {
        return decision;
      }
    }
    throw new IllegalArgumentException("unknown re-identification risk decision: " + wireValue);
  }
}
