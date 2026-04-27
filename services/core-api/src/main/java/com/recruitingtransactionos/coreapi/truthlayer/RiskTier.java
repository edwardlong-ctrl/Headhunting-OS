package com.recruitingtransactionos.coreapi.truthlayer;

public enum RiskTier {
  T0_AUTO_CLEANUP("T0_AUTO_CLEANUP"),
  T1_LOW("T1_LOW"),
  T2_MEDIUM("T2_MEDIUM"),
  T3_HIGH("T3_HIGH"),
  T4_TRANSACTION_LEGAL("T4_TRANSACTION_LEGAL");

  private final String wireValue;

  RiskTier(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
