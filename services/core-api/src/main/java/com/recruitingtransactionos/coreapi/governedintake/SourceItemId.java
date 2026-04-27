package com.recruitingtransactionos.coreapi.governedintake;

import java.util.Objects;
import java.util.UUID;

public record SourceItemId(UUID value) {

  public SourceItemId {
    Objects.requireNonNull(value, "sourceItemId must not be null");
  }
}
