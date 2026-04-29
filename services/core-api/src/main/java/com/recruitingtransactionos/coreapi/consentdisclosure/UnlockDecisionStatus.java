package com.recruitingtransactionos.coreapi.consentdisclosure;

public enum UnlockDecisionStatus {
  REQUESTED("requested"),
  REQUIRES_REVIEW("requires_review"),
  APPROVED("approved"),
  DENIED("denied"),
  EXPIRED("expired"),
  REVOKED("revoked"),
  INVALID("invalid");

  private final String wireValue;

  UnlockDecisionStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
