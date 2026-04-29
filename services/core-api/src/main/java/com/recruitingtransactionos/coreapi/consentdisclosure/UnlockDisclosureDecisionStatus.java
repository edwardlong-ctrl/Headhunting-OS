package com.recruitingtransactionos.coreapi.consentdisclosure;

public enum UnlockDisclosureDecisionStatus {
  ALLOWED("allowed"),
  DENIED("denied"),
  REQUIRES_REVIEW("requires_review");

  private final String wireValue;

  UnlockDisclosureDecisionStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
