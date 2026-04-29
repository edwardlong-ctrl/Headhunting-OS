package com.recruitingtransactionos.coreapi.candidatedocument;

public enum CandidateDocumentStatus {
  ACTIVE("active"),
  SUPERSEDED("superseded"),
  ARCHIVED("archived");

  private final String wireValue;

  CandidateDocumentStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static CandidateDocumentStatus fromWireValue(String wireValue) {
    for (CandidateDocumentStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown candidate document status: " + wireValue);
  }
}
