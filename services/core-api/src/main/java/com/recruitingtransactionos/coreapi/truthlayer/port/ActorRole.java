package com.recruitingtransactionos.coreapi.truthlayer.port;

public enum ActorRole {
  OWNER("owner"),
  CONSULTANT("consultant"),
  CLIENT("client"),
  CANDIDATE("candidate"),
  ADMIN("admin"),
  SYSTEM("system"),
  AI("ai");

  private final String wireValue;

  ActorRole(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static ActorRole fromWireValue(String wireValue) {
    for (ActorRole role : values()) {
      if (role.wireValue.equals(wireValue)) {
        return role;
      }
    }
    throw new IllegalArgumentException("unknown actor role: " + wireValue);
  }
}
