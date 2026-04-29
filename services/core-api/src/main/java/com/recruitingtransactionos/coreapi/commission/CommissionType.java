package com.recruitingtransactionos.coreapi.commission;

public enum CommissionType {
  FULL_FEE("full_fee"),
  SPLIT("split"),
  REFERRAL("referral"),
  OVERRIDE("override");

  private final String wireValue;

  CommissionType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static CommissionType fromWireValue(String wireValue) {
    for (CommissionType type : values()) {
      if (type.wireValue.equals(wireValue)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown commission type: " + wireValue);
  }
}
