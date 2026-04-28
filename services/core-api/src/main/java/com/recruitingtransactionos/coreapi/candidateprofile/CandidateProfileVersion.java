package com.recruitingtransactionos.coreapi.candidateprofile;

public record CandidateProfileVersion(int value) {

  public CandidateProfileVersion {
    if (value <= 0) {
      throw new IllegalArgumentException("profileVersion must be greater than zero");
    }
  }
}
