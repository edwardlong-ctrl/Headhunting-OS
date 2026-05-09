package com.recruitingtransactionos.coreapi.industrypack;

import com.recruitingtransactionos.coreapi.matching.IndustryPackMaturity;
import java.time.Instant;
import java.util.Objects;

public record IndustryPackReviewQueueItem(
    IndustryPackKey packKey,
    IndustryPackMaturity maturity,
    String severity,
    String reason,
    Instant reviewBy) {

  public IndustryPackReviewQueueItem {
    Objects.requireNonNull(packKey, "packKey must not be null");
    Objects.requireNonNull(maturity, "maturity must not be null");
    severity = requireNonBlank(severity, "severity");
    reason = requireNonBlank(reason, "reason");
    Objects.requireNonNull(reviewBy, "reviewBy must not be null");
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
