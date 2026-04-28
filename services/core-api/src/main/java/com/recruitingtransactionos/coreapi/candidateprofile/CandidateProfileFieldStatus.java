package com.recruitingtransactionos.coreapi.candidateprofile;

public enum CandidateProfileFieldStatus {
  AI_EXTRACTED("ai_extracted"),
  HUMAN_ACKNOWLEDGED("human_acknowledged"),
  CONSULTANT_ATTESTED("consultant_attested"),
  CANDIDATE_CONFIRMED("candidate_confirmed"),
  EXTERNAL_VERIFIED("external_verified"),
  SYSTEM_INFERENCE("system_inference"),
  CONFLICTING("conflicting"),
  NEEDS_CONFIRMATION("needs_confirmation"),
  STALE("stale"),
  UNVERIFIED("unverified"),
  LIKELY_CURRENT("likely_current");

  private final String wireValue;

  CandidateProfileFieldStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
