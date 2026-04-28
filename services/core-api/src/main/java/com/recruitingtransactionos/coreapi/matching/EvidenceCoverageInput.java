package com.recruitingtransactionos.coreapi.matching;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EvidenceCoverageInput(
    Set<MatchDimension> requiredDimensions,
    List<MatchEvidenceSignal> evidenceSignals) {

  public EvidenceCoverageInput {
    requiredDimensions = copyRequiredDimensions(requiredDimensions);
    evidenceSignals = copyEvidenceSignals(evidenceSignals, requiredDimensions);
  }

  private static Set<MatchDimension> copyRequiredDimensions(Set<MatchDimension> dimensions) {
    Objects.requireNonNull(dimensions, "requiredDimensions must not be null");
    if (dimensions.isEmpty()) {
      throw new IllegalArgumentException("requiredDimensions must not be empty");
    }
    EnumSet<MatchDimension> copied = EnumSet.noneOf(MatchDimension.class);
    for (MatchDimension dimension : dimensions) {
      copied.add(Objects.requireNonNull(dimension, "requiredDimensions must not contain null"));
    }
    return Collections.unmodifiableSet(copied);
  }

  private static List<MatchEvidenceSignal> copyEvidenceSignals(
      List<MatchEvidenceSignal> signals,
      Set<MatchDimension> requiredDimensions) {
    Objects.requireNonNull(signals, "evidenceSignals must not be null");
    List<MatchEvidenceSignal> copied = List.copyOf(signals);
    for (MatchEvidenceSignal signal : copied) {
      Objects.requireNonNull(signal, "evidenceSignals must not contain null");
      if (!requiredDimensions.contains(signal.dimension())) {
        throw new IllegalArgumentException("evidence signal dimension is not required");
      }
    }
    return copied;
  }
}
