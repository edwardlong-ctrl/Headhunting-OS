package com.recruitingtransactionos.coreapi.documentintelligence;

public enum DocumentProcessingStatus {
  PENDING_EXTERNAL_PROCESSING("PENDING_EXTERNAL_PROCESSING"),
  SUCCEEDED("SUCCEEDED"),
  FAILED("FAILED"),
  UNSUPPORTED_FOR_V1("UNSUPPORTED_FOR_V1");

  private final String wireValue;

  DocumentProcessingStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static DocumentProcessingStatus fromWireValue(String wireValue) {
    for (DocumentProcessingStatus value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown document processing status: " + wireValue);
  }
}
