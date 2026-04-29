package com.recruitingtransactionos.coreapi.candidatedocument;

import java.util.Objects;
import java.util.UUID;

public record CandidateDocumentId(UUID value) {

  public CandidateDocumentId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
