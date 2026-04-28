package com.recruitingtransactionos.coreapi.candidateprofile;

import java.util.Objects;
import java.util.UUID;

public record CandidateId(UUID value) {

  public CandidateId {
    Objects.requireNonNull(value, "candidateId must not be null");
  }
}
