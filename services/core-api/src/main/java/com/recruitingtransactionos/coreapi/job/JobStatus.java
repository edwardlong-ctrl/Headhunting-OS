package com.recruitingtransactionos.coreapi.job;

public enum JobStatus {
  DRAFT("draft"),
  SUBMITTED("submitted"),
  INTAKE_REVIEW("intake_review"),
  NEEDS_MORE_INFO("needs_more_info"),
  COMMERCIAL_PENDING("commercial_pending"),
  CONTRACT_PENDING("contract_pending"),
  ACTIVATED("activated"),
  SHORTLIST_IN_PROGRESS("shortlist_in_progress"),
  SHORTLIST_SENT("shortlist_sent"),
  INTERVIEWING("interviewing"),
  OFFER_PENDING("offer_pending"),
  CLOSED("closed"),
  PAUSED("paused"),
  CANCELLED("cancelled");

  private final String wireValue;

  JobStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static JobStatus fromWireValue(String wireValue) {
    for (JobStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown job status: " + wireValue);
  }
}
