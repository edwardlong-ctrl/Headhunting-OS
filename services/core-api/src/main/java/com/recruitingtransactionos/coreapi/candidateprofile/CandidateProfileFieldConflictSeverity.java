package com.recruitingtransactionos.coreapi.candidateprofile;

public enum CandidateProfileFieldConflictSeverity {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  BLOCKING("blocking");

  private final String wireValue;

  CandidateProfileFieldConflictSeverity(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
