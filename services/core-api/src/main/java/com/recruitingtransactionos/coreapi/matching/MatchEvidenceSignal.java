package com.recruitingtransactionos.coreapi.matching;

import java.util.Objects;

public record MatchEvidenceSignal(
    MatchDimension dimension,
    ProvenanceCategory provenanceCategory,
    EvidenceAssertionStrength assertionStrength,
    boolean independent) {

  public MatchEvidenceSignal {
    Objects.requireNonNull(dimension, "dimension must not be null");
    Objects.requireNonNull(provenanceCategory, "provenanceCategory must not be null");
    Objects.requireNonNull(assertionStrength, "assertionStrength must not be null");
  }
}
