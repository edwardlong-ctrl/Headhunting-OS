package com.recruitingtransactionos.coreapi.governedintake;

public enum SourceItemStatus {
  RECEIVED("RECEIVED"),
  REGISTERED("REGISTERED"),
  ATTACHED_TO_PACKET("ATTACHED_TO_PACKET"),
  SUPERSEDED("SUPERSEDED"),
  REJECTED("REJECTED");

  private final String wireValue;

  SourceItemStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static SourceItemStatus fromWireValue(String wireValue) {
    for (SourceItemStatus value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown source item status: " + wireValue);
  }
}
