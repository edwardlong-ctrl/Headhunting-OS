package com.recruitingtransactionos.coreapi.matching;

public enum ProvenanceCategory {
  CUSTOMER_FEEDBACK("customer_feedback"),
  VERIFIED_WORK_SAMPLE("verified_work_sample"),
  CONSULTANT_DEEP_DIVE("consultant_deep_dive"),
  CANDIDATE_FORM("candidate_form"),
  CV_OR_LINKEDIN("cv_or_linkedin"),
  AI_OPTIMIZED_TEXT("ai_optimized_text"),
  UNKNOWN("unknown");

  private final String wireValue;

  ProvenanceCategory(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
