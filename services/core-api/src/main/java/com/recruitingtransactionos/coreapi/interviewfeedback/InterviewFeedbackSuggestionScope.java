package com.recruitingtransactionos.coreapi.interviewfeedback;

public enum InterviewFeedbackSuggestionScope {
  INTERACTION("interaction"),
  CANDIDATE_PROFILE("candidate_profile"),
  COMPANY_PREFERENCE("company_preference"),
  JOB_OUTCOME("job_outcome");

  private final String wireValue;

  InterviewFeedbackSuggestionScope(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InterviewFeedbackSuggestionScope fromWireValue(String wireValue) {
    for (InterviewFeedbackSuggestionScope scope : values()) {
      if (scope.wireValue.equals(wireValue)) {
        return scope;
      }
    }
    throw new IllegalArgumentException("Unknown interview feedback suggestion scope: " + wireValue);
  }
}
