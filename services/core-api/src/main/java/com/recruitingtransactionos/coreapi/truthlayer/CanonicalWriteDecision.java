package com.recruitingtransactionos.coreapi.truthlayer;

import java.util.List;
import java.util.Objects;

public record CanonicalWriteDecision(
    CanonicalWriteDecisionType type,
    List<String> reasons) {

  public CanonicalWriteDecision {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(reasons, "reasons must not be null");
    reasons = List.copyOf(reasons);
    if (reasons.isEmpty()) {
      throw new IllegalArgumentException("canonical write decisions must include at least one reason");
    }
  }
}
