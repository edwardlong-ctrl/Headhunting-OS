package com.recruitingtransactionos.coreapi.interaction;

import java.util.Objects;
import java.util.UUID;

public record CandidateCompanyInteractionId(UUID value) {

  public CandidateCompanyInteractionId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
