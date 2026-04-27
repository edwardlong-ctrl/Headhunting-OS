package com.recruitingtransactionos.coreapi.governedintake;

public enum InformationPacketType {
  CANDIDATE("CANDIDATE"),
  COMPANY("COMPANY"),
  JOB("JOB"),
  CALL_NOTE("CALL_NOTE"),
  FEEDBACK("FEEDBACK"),
  MIXED("MIXED");

  private final String wireValue;

  InformationPacketType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InformationPacketType fromWireValue(String wireValue) {
    for (InformationPacketType value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown information packet type: " + wireValue);
  }
}
