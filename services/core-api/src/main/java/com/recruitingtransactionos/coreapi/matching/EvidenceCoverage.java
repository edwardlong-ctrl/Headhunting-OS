package com.recruitingtransactionos.coreapi.matching;

import java.util.Objects;

public record EvidenceCoverage(
    double coverageRatio,
    EvidenceCoverageLevel coverageLevel,
    int independentEvidenceCount,
    int independentHighTrustEvidenceCount) {

  public EvidenceCoverage {
    if (Double.isNaN(coverageRatio)
        || Double.isInfinite(coverageRatio)
        || coverageRatio < 0.0
        || coverageRatio > 1.0) {
      throw new IllegalArgumentException("evidence coverage ratio must be between 0.0 and 1.0");
    }
    Objects.requireNonNull(coverageLevel, "coverageLevel must not be null");
    if (independentEvidenceCount < 0) {
      throw new IllegalArgumentException("independentEvidenceCount must not be negative");
    }
    if (independentHighTrustEvidenceCount < 0) {
      throw new IllegalArgumentException("independentHighTrustEvidenceCount must not be negative");
    }
    if (independentHighTrustEvidenceCount > independentEvidenceCount) {
      throw new IllegalArgumentException(
          "independentHighTrustEvidenceCount must not exceed independentEvidenceCount");
    }
  }
}
