package com.recruitingtransactionos.coreapi.commission;

public enum CommissionStatus {
  PENDING("pending"),
  CALCULATED("calculated"),
  PAID("paid"),
  WITHHELD("withheld");

  private final String wireValue;

  CommissionStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static CommissionStatus fromWireValue(String wireValue) {
    for (CommissionStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown commission status: " + wireValue);
  }
}
