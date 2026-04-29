package com.recruitingtransactionos.coreapi.consentdisclosure;

public enum DisclosureReviewStatus {
  NOT_REQUIRED("not_required"),
  REQUIRED("required"),
  PENDING("pending"),
  HUMAN_APPROVED("human_approved"),
  REJECTED("rejected"),
  EXPIRED("expired"),
  INVALID("invalid");

  private final String wireValue;

  DisclosureReviewStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static DisclosureReviewStatus fromWireValue(String wireValue) {
    for (DisclosureReviewStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("unknown disclosure review status: " + wireValue);
  }
}
