package com.recruitingtransactionos.coreapi.governedintake;

public enum SourceItemOrigin {
  CONSULTANT_UPLOAD("CONSULTANT_UPLOAD"),
  CANDIDATE_UPLOAD("CANDIDATE_UPLOAD"),
  CLIENT_UPLOAD("CLIENT_UPLOAD"),
  EMAIL_IMPORT("EMAIL_IMPORT"),
  SYSTEM_IMPORT("SYSTEM_IMPORT"),
  ADMIN_IMPORT("ADMIN_IMPORT"),
  OTHER("OTHER");

  private final String wireValue;

  SourceItemOrigin(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static SourceItemOrigin fromWireValue(String wireValue) {
    for (SourceItemOrigin value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown source item origin: " + wireValue);
  }
}
