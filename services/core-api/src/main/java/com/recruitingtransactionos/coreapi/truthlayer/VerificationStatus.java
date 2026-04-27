package com.recruitingtransactionos.coreapi.truthlayer;

public enum VerificationStatus {
  AI_EXTRACTED("ai_extracted"),
  HUMAN_ACKNOWLEDGED("human_acknowledged"),
  CONSULTANT_ATTESTED("consultant_attested"),
  CANDIDATE_CONFIRMED("candidate_confirmed"),
  EXTERNAL_VERIFIED("external_verified"),
  SYSTEM_INFERENCE("system_inference"),
  CONFLICTING("conflicting"),
  NEEDS_CONFIRMATION("needs_confirmation"),
  REJECTED("rejected"),
  RETRACTED("retracted");

  private final String wireValue;

  VerificationStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
