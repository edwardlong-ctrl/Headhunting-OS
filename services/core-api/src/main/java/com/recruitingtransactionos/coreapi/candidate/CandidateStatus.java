package com.recruitingtransactionos.coreapi.candidate;

public enum CandidateStatus {
  NEW("new"),
  PROFILE_PARSED("profile_parsed"),
  CONSULTANT_REVIEW("consultant_review"),
  AVAILABLE("available"),
  MATCHED_TO_JOB("matched_to_job"),
  OUTREACH("outreach"),
  INTERESTED("interested"),
  CONSENT_PENDING("consent_pending"),
  CONSENT_CONFIRMED("consent_confirmed"),
  SHORTLISTED("shortlisted"),
  CLIENT_REVIEW("client_review"),
  IDENTITY_DISCLOSED("identity_disclosed"),
  INTERVIEWING("interviewing"),
  OFFER_PENDING("offer_pending"),
  PLACED("placed"),
  REJECTED("rejected"),
  ARCHIVED("archived"),
  DO_NOT_CONTACT("do_not_contact"),
  MERGED("merged");

  private final String wireValue;

  CandidateStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static CandidateStatus fromWireValue(String wireValue) {
    for (CandidateStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown candidate status: " + wireValue);
  }
}
