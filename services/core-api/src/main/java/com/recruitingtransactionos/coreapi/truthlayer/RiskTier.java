package com.recruitingtransactionos.coreapi.truthlayer;

public enum RiskTier {
  T0_AUTOMATED_CLEANUP("T0_AUTOMATED_CLEANUP"),
  T1_LOW_RISK("T1_LOW_RISK"),
  T2_MEDIUM_RISK("T2_MEDIUM_RISK"),
  T3_HIGH_RISK("T3_HIGH_RISK"),
  T4_TRANSACTION_LEGAL_BLOCKING("T4_TRANSACTION_LEGAL_BLOCKING");

  private final String wireValue;

  RiskTier(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public boolean requiresHumanFinalActor() {
    return this == T3_HIGH_RISK || this == T4_TRANSACTION_LEGAL_BLOCKING;
  }
}
