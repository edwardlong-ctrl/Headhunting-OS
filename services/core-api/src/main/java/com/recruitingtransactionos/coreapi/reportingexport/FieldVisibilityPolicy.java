package com.recruitingtransactionos.coreapi.reportingexport;

public enum FieldVisibilityPolicy {
  OWNER_INTERNAL("owner_internal"),
  CONSULTANT_INTERNAL("consultant_internal"),
  CLIENT_SAFE("client_safe"),
  GENERALIZED("generalized"),
  CANDIDATE_SELF("candidate_self"),
  SYSTEM_GOVERNANCE("system_governance"),
  COMMERCIAL_READ_ONLY("commercial_read_only"),
  RETENTION_EVIDENCE("retention_evidence"),
  RAW_PII("raw_pii");

  private final String wireValue;

  FieldVisibilityPolicy(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  boolean canAppearUnder(FieldVisibilityPolicy exportPolicy) {
    return switch (exportPolicy) {
      case CLIENT_SAFE -> this == CLIENT_SAFE || this == GENERALIZED;
      case GENERALIZED -> this == GENERALIZED;
      case CANDIDATE_SELF -> this == CANDIDATE_SELF || this == CLIENT_SAFE || this == GENERALIZED;
      case OWNER_INTERNAL -> this == OWNER_INTERNAL
          || this == COMMERCIAL_READ_ONLY
          || this == SYSTEM_GOVERNANCE
          || this == RETENTION_EVIDENCE;
      case CONSULTANT_INTERNAL -> this == CONSULTANT_INTERNAL
          || this == OWNER_INTERNAL
          || this == COMMERCIAL_READ_ONLY
          || this == SYSTEM_GOVERNANCE;
      case SYSTEM_GOVERNANCE -> this == SYSTEM_GOVERNANCE || this == RETENTION_EVIDENCE;
      case COMMERCIAL_READ_ONLY -> this == COMMERCIAL_READ_ONLY;
      case RETENTION_EVIDENCE -> this == RETENTION_EVIDENCE || this == SYSTEM_GOVERNANCE;
      case RAW_PII -> false;
    };
  }
}
