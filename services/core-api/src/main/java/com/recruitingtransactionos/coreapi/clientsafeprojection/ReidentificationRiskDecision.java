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
}
