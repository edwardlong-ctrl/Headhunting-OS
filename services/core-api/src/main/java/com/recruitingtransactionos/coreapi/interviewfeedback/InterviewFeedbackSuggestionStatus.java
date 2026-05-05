package com.recruitingtransactionos.coreapi.interviewfeedback;

public enum InterviewFeedbackSuggestionStatus {
  PENDING_REVIEW("pending_review"),
  APPROVED("approved"),
  REJECTED("rejected"),
  DEFERRED("deferred");

  private final String wireValue;

  InterviewFeedbackSuggestionStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InterviewFeedbackSuggestionStatus fromWireValue(String wireValue) {
    for (InterviewFeedbackSuggestionStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown interview feedback suggestion status: " + wireValue);
  }
}
