package com.recruitingtransactionos.coreapi.placement;

public enum PlacementStatus {
  OFFER_PENDING("offer_pending"),
  OFFER_ACCEPTED("offer_accepted"),
  ONBOARDED("onboarded"),
  INVOICE_READY("invoice_ready"),
  INVOICE_SENT("invoice_sent"),
  PAID("paid"),
  GUARANTEE_ACTIVE("guarantee_active"),
  GUARANTEE_COMPLETED("guarantee_completed"),
  REPLACEMENT_REQUIRED("replacement_required"),
  CANCELLED("cancelled");

  private final String wireValue;

  PlacementStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static PlacementStatus fromWireValue(String wireValue) {
    for (PlacementStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown placement status: " + wireValue);
  }
}
