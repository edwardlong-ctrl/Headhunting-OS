package com.recruitingtransactionos.coreapi.shortlist;

public enum ShortlistStatus {
  DRAFT("draft"),
  READY_FOR_REVIEW("ready_for_review"),
  SENT_TO_CLIENT("sent_to_client"),
  CLIENT_VIEWED("client_viewed"),
  CLIENT_FEEDBACK_PENDING("client_feedback_pending"),
  CANDIDATE_SELECTED("candidate_selected"),
  CONTACT_UNLOCKED("contact_unlocked"),
  INTERVIEWING("interviewing"),
  CLOSED("closed");

  private final String wireValue;

  ShortlistStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static ShortlistStatus fromWireValue(String wireValue) {
    for (ShortlistStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown shortlist status: " + wireValue);
  }
}
