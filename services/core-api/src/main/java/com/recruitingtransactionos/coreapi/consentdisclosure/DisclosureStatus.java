package com.recruitingtransactionos.coreapi.consentdisclosure;

public enum DisclosureStatus {
  NOT_DISCLOSED("not_disclosed"),
  CONSENT_CONFIRMED("consent_confirmed"),
  REQUESTED("requested"),
  CONSULTANT_APPROVED("consultant_approved"),
  APPROVED("approved"),
  IDENTITY_DISCLOSED("identity_disclosed"),
  FEE_PROTECTION_ACTIVE("fee_protection_active"),
  DECLINED("declined"),
  EXPIRED("expired"),
  REVOKED("revoked"),
  INVALID("invalid");

  private final String wireValue;

  DisclosureStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static DisclosureStatus fromWireValue(String wireValue) {
    for (DisclosureStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("unknown disclosure status: " + wireValue);
  }
}
