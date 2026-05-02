package com.recruitingtransactionos.coreapi.industrypack;

import java.util.Objects;
import java.util.UUID;

public record IndustryPackId(UUID value) {
  public IndustryPackId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
