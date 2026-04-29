package com.recruitingtransactionos.coreapi.consentdisclosure;

public enum ConsentStatus {
  NOT_REQUESTED("not_requested"),
  REQUESTED("requested"),
  VIEWED_BY_CANDIDATE("viewed_by_candidate"),
  CONFIRMED("confirmed"),
  DECLINED("declined"),
  EXPIRED("expired"),
  REVOKED("revoked"),
  INVALID("invalid");

  private final String wireValue;

  ConsentStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static ConsentStatus fromWireValue(String wireValue) {
    for (ConsentStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("unknown consent status: " + wireValue);
  }
}
