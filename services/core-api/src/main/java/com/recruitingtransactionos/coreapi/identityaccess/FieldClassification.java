package com.recruitingtransactionos.coreapi.identityaccess;

public enum FieldClassification {
  UNKNOWN("unknown"),
  CLIENT_SAFE("client_safe"),
  GENERALIZED("generalized"),
  INTERNAL("internal"),
  PII("pii"),
  RAW_SOURCE("raw_source"),
  CONSULTANT_PRIVATE("consultant_private"),
  AUDIT("audit"),
  COMMERCIAL("commercial"),
  CONSENT_DISCLOSURE("consent_disclosure"),
  SYSTEM_GOVERNANCE("system_governance");

  private final String wireValue;

  FieldClassification(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
