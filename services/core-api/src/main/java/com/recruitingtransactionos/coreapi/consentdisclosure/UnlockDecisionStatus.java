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

  public static UnlockDecisionStatus fromWireValue(String wireValue) {
    for (UnlockDecisionStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("unknown unlock decision status: " + wireValue);
  }
}
