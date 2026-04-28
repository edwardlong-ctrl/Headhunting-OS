package com.recruitingtransactionos.coreapi.matching;

public record ProvenanceWeight(double value) {

  public ProvenanceWeight {
    if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException("provenance weight must be between 0.0 and 1.0");
    }
  }

  public static ProvenanceWeight of(double value) {
    return new ProvenanceWeight(value);
  }
}
