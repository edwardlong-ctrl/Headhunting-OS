package com.recruitingtransactionos.coreapi.consentdisclosure;

public enum ClientUnlockRequestStatus {
  REQUESTED("requested"),
  UNDER_REVIEW("under_review"),
  APPROVED("approved"),
  REJECTED("rejected"),
  CANCELLED("cancelled");

  private final String wireValue;

  ClientUnlockRequestStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static ClientUnlockRequestStatus fromWireValue(String wireValue) {
    for (ClientUnlockRequestStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown client unlock request status: " + wireValue);
  }
}
