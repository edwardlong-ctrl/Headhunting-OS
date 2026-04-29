package com.recruitingtransactionos.coreapi.shortlist;

import java.util.Objects;
import java.util.UUID;

public record ShortlistId(UUID value) {

  public ShortlistId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
