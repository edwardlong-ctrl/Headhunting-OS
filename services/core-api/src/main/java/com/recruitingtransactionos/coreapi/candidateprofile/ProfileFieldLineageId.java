package com.recruitingtransactionos.coreapi.candidateprofile;

import java.util.Objects;
import java.util.UUID;

public record ProfileFieldLineageId(UUID value) {

  public ProfileFieldLineageId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
