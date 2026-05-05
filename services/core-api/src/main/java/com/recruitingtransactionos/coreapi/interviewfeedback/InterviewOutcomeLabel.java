package com.recruitingtransactionos.coreapi.interviewfeedback;

public enum InterviewOutcomeLabel {
  STRONG_FIT("strong_fit"),
  COMPENSATION_MISMATCH("compensation_mismatch"),
  SKILL_GAP("skill_gap"),
  EXPERIENCE_GAP("experience_gap"),
  COMMUNICATION_CONCERN("communication_concern"),
  CULTURE_MISMATCH("culture_mismatch"),
  PROCESS_HOLD("process_hold"),
  REJECTED_OTHER("rejected_other");

  private final String wireValue;

  InterviewOutcomeLabel(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InterviewOutcomeLabel fromWireValue(String wireValue) {
    for (InterviewOutcomeLabel label : values()) {
      if (label.wireValue.equals(wireValue)) {
        return label;
      }
    }
    throw new IllegalArgumentException("Unknown interview outcome label: " + wireValue);
  }
}
