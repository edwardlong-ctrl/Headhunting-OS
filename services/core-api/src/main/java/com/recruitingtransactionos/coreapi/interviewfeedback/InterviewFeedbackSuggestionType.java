package com.recruitingtransactionos.coreapi.interviewfeedback;

public enum InterviewFeedbackSuggestionType {
  OUTCOME_LABEL("outcome_label"),
  PROFILE_UPDATE("profile_update"),
  COMPANY_PREFERENCE_UPDATE("company_preference_update");

  private final String wireValue;

  InterviewFeedbackSuggestionType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InterviewFeedbackSuggestionType fromWireValue(String wireValue) {
    for (InterviewFeedbackSuggestionType type : values()) {
      if (type.wireValue.equals(wireValue)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown interview feedback suggestion type: " + wireValue);
  }
}
