package com.recruitingtransactionos.coreapi.company;

public enum CompanyStatus {
  NEW("new"),
  ACTIVE("active"),
  INACTIVE("inactive"),
  ARCHIVED("archived");

  private final String wireValue;

  CompanyStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static CompanyStatus fromWireValue(String wireValue) {
    for (CompanyStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown company status: " + wireValue);
  }
}
