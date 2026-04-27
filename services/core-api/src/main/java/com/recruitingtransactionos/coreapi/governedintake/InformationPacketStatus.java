package com.recruitingtransactionos.coreapi.governedintake;

public enum InformationPacketStatus {
  CREATED("CREATED"),
  SOURCES_ATTACHED("SOURCES_ATTACHED"),
  READY_FOR_EXTRACTION("READY_FOR_EXTRACTION"),
  EXTRACTION_PENDING("EXTRACTION_PENDING"),
  EXTRACTION_COMPLETE("EXTRACTION_COMPLETE"),
  REVIEW_PENDING("REVIEW_PENDING"),
  APPROVED("APPROVED"),
  PUBLISHED("PUBLISHED"),
  REJECTED("REJECTED");

  private final String wireValue;

  InformationPacketStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InformationPacketStatus fromWireValue(String wireValue) {
    for (InformationPacketStatus value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown information packet status: " + wireValue);
  }
}
