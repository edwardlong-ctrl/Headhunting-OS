package com.recruitingtransactionos.coreapi.shortlist;

import java.util.Objects;
import java.util.UUID;

public record ShortlistCandidateCardId(UUID value) {

  public ShortlistCandidateCardId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
