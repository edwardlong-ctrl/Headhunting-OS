package com.recruitingtransactionos.coreapi.truthlayer;

public enum CanonicalWriteDecisionType {
  ALLOW("allow"),
  BLOCK("block"),
  REQUIRE_REVIEW("require_review");

  private final String wireValue;

  CanonicalWriteDecisionType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
