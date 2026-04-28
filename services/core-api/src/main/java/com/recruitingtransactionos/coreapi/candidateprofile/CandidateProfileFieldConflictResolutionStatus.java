package com.recruitingtransactionos.coreapi.candidateprofile;

public enum CandidateProfileFieldConflictResolutionStatus {
  UNRESOLVED("unresolved"),
  NEEDS_REVIEW("needs_review"),
  RESOLVED("resolved"),
  IGNORED("ignored");

  private final String wireValue;

  CandidateProfileFieldConflictResolutionStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
