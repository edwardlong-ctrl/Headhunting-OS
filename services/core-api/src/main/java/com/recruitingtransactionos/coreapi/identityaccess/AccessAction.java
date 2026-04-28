package com.recruitingtransactionos.coreapi.identityaccess;

public enum AccessAction {
  UNKNOWN("unknown"),
  READ("read"),
  CREATE("create"),
  UPDATE("update"),
  APPROVE("approve"),
  DISCLOSE("disclose"),
  UNLOCK("unlock"),
  AUDIT("audit"),
  EXPORT("export");

  private final String wireValue;

  AccessAction(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
