package com.recruitingtransactionos.coreapi.identityaccess;

public enum PortalRole {
  UNKNOWN("unknown"),
  OWNER("owner"),
  CONSULTANT("consultant"),
  CLIENT("client"),
  CANDIDATE("candidate"),
  ADMIN("admin"),
  SYSTEM("system"),
  AI_ASSISTANT("ai_assistant");

  private final String wireValue;

  PortalRole(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  boolean isGovernanceOrAutomationRole() {
    return this == ADMIN || this == SYSTEM || this == AI_ASSISTANT;
  }
}
