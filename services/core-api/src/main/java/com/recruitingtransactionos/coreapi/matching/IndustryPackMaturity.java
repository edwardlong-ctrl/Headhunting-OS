package com.recruitingtransactionos.coreapi.matching;

public enum IndustryPackMaturity {
  COLD("cold"),
  SEEDED("seeded"),
  CALIBRATED("calibrated"),
  PRODUCTION("production");

  private final String wireValue;

  IndustryPackMaturity(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
