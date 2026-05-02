package com.recruitingtransactionos.coreapi.industrypack;

import java.util.Locale;
import java.util.Objects;

public record IndustryPackKey(String value) {
  public IndustryPackKey {
    Objects.requireNonNull(value, "value must not be null");
    String normalized = value.strip().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
    value = normalized;
  }
}
