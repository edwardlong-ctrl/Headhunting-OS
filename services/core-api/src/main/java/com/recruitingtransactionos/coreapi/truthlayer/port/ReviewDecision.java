package com.recruitingtransactionos.coreapi.truthlayer.port;

public enum ReviewDecision {
  APPROVED("approved"),
  REJECTED("rejected"),
  ESCALATED("escalated"),
  NEEDS_CONFIRMATION("needs_confirmation");

  private final String wireValue;

  ReviewDecision(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
