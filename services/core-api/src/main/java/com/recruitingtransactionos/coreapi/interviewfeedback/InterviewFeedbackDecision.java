package com.recruitingtransactionos.coreapi.interviewfeedback;

public enum InterviewFeedbackDecision {
  PROCEED("proceed"),
  HOLD("hold"),
  REJECT("reject");

  private final String wireValue;

  InterviewFeedbackDecision(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InterviewFeedbackDecision fromWireValue(String wireValue) {
    for (InterviewFeedbackDecision decision : values()) {
      if (decision.wireValue.equals(wireValue)) {
        return decision;
      }
    }
    throw new IllegalArgumentException("Unknown interview feedback decision: " + wireValue);
  }
}
