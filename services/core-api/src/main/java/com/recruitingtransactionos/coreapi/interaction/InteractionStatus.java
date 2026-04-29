package com.recruitingtransactionos.coreapi.interaction;

public enum InteractionStatus {
  ACTIVE("active"),
  COMPLETED("completed"),
  CANCELLED("cancelled");

  private final String wireValue;

  InteractionStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InteractionStatus fromWireValue(String wireValue) {
    for (InteractionStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown interaction status: " + wireValue);
  }
}
