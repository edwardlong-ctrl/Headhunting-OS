package com.recruitingtransactionos.coreapi.shortlist;

public enum ShortlistCandidateCardStatus {
  DRAFT("draft"),
  INCLUDED("included"),
  REMOVED("removed"),
  SELECTED("selected"),
  UNLOCKED("unlocked"),
  REJECTED("rejected");

  private final String wireValue;

  ShortlistCandidateCardStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static ShortlistCandidateCardStatus fromWireValue(String wireValue) {
    for (ShortlistCandidateCardStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown shortlist candidate card status: " + wireValue);
  }
}
