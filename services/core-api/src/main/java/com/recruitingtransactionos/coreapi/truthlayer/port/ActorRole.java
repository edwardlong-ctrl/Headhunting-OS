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
}
