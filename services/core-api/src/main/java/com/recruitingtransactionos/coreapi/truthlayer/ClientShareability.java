package com.recruitingtransactionos.coreapi.truthlayer;

public enum ClientShareability {
  INTERNAL_ONLY("internal_only"),
  CLIENT_SAFE("client_safe"),
  CONSENT_REQUIRED("consent_required"),
  FORBIDDEN("forbidden");

  private final String wireValue;

  ClientShareability(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
