package com.recruitingtransactionos.coreapi.industrypack;

import com.recruitingtransactionos.coreapi.matching.IndustryPackMaturity;
import java.util.Objects;

public record IndustryPack(
    IndustryPackId industryPackId,
    IndustryPackKey packKey,
    String displayName,
    IndustryPackMaturity maturity,
    boolean active) {

  public IndustryPack {
    Objects.requireNonNull(industryPackId, "industryPackId must not be null");
    Objects.requireNonNull(packKey, "packKey must not be null");
    displayName = requireNonBlank(displayName, "displayName");
    Objects.requireNonNull(maturity, "maturity must not be null");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    String normalized = value.strip();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
