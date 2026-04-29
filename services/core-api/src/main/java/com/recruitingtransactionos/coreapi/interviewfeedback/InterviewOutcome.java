package com.recruitingtransactionos.coreapi.interviewfeedback;

public enum InterviewOutcome {
  STRONG_YES("strong_yes"),
  YES("yes"),
  MAYBE("maybe"),
  WEAK_NO("weak_no"),
  STRONG_NO("strong_no");

  private final String wireValue;

  InterviewOutcome(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InterviewOutcome fromWireValue(String wireValue) {
    for (InterviewOutcome outcome : values()) {
      if (outcome.wireValue.equals(wireValue)) {
        return outcome;
      }
    }
    throw new IllegalArgumentException("Unknown interview outcome: " + wireValue);
  }
}
