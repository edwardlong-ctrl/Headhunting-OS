package com.recruitingtransactionos.coreapi.candidateprofile;

import java.util.Objects;
import java.util.UUID;

public record CandidateProfileId(UUID value) {

  public CandidateProfileId {
    Objects.requireNonNull(value, "candidateProfileId must not be null");
  }
}
