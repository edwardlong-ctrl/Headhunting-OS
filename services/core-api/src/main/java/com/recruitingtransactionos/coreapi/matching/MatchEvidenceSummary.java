package com.recruitingtransactionos.coreapi.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record MatchEvidenceSummary(
    EvidenceCoverage evidenceCoverage,
    Set<MatchDimension> requiredDimensions,
    Set<MatchDimension> coveredDimensions,
    Set<MatchDimension> missingEvidenceDimensions,
    Set<MatchDimension> weakSignalOnlyDimensions,
    int independentEvidenceCount,
    int independentHighTrustEvidenceCount) {

  public MatchEvidenceSummary {
    Objects.requireNonNull(evidenceCoverage, "evidenceCoverage must not be null");
    requiredDimensions = copyDimensions(requiredDimensions, "requiredDimensions");
    coveredDimensions = copyDimensions(coveredDimensions, "coveredDimensions");
    missingEvidenceDimensions =
        copyDimensions(missingEvidenceDimensions, "missingEvidenceDimensions");
    weakSignalOnlyDimensions = copyDimensions(weakSignalOnlyDimensions, "weakSignalOnlyDimensions");
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

  static MatchEvidenceSummary from(
      EvidenceCoverageInput input,
      ProvenanceWeightingPolicy provenancePolicy) {
    Objects.requireNonNull(input, "evidenceCoverageInput must not be null");
    Objects.requireNonNull(provenancePolicy, "provenancePolicy must not be null");

    Set<MatchDimension> required = input.requiredDimensions();
    EnumSet<MatchDimension> covered = EnumSet.noneOf(MatchDimension.class);
    Map<MatchDimension, List<MatchEvidenceSignal>> signalsByDimension =
        new EnumMap<>(MatchDimension.class);
    int independentEvidenceCount = 0;
    int independentHighTrustEvidenceCount = 0;

    for (MatchEvidenceSignal signal : input.evidenceSignals()) {
      provenancePolicy.requireKnown(signal);
      covered.add(signal.dimension());
      signalsByDimension.computeIfAbsent(signal.dimension(), ignored -> new ArrayList<>())
          .add(signal);
      if (signal.independent()) {
        independentEvidenceCount++;
        if (provenancePolicy.isHighTrust(signal.provenanceCategory())) {
          independentHighTrustEvidenceCount++;
        }
      }
    }

    EnumSet<MatchDimension> missing = EnumSet.copyOf(required);
    missing.removeAll(covered);

    EnumSet<MatchDimension> weakOnly = EnumSet.noneOf(MatchDimension.class);
    for (Map.Entry<MatchDimension, List<MatchEvidenceSignal>> entry : signalsByDimension.entrySet()) {
      boolean onlyWeakSignals = entry.getValue().stream()
          .allMatch(provenancePolicy::isWeakSignalOnlySupport);
      if (onlyWeakSignals) {
        weakOnly.add(entry.getKey());
      }
    }

    double coverageRatio = ((double) covered.size()) / required.size();
    EvidenceCoverage evidenceCoverage = new EvidenceCoverage(
        coverageRatio,
        coverageLevel(coverageRatio),
        independentEvidenceCount,
        independentHighTrustEvidenceCount);

    return new MatchEvidenceSummary(
        evidenceCoverage,
        required,
        covered,
        missing,
        weakOnly,
        independentEvidenceCount,
        independentHighTrustEvidenceCount);
  }

  private static EvidenceCoverageLevel coverageLevel(double coverageRatio) {
    if (coverageRatio == 0.0) {
      return EvidenceCoverageLevel.NONE;
    }
    if (coverageRatio <= 0.33) {
      return EvidenceCoverageLevel.LOW;
    }
    if (coverageRatio < 0.67) {
      return EvidenceCoverageLevel.MEDIUM;
    }
    if (coverageRatio < 1.0) {
      return EvidenceCoverageLevel.HIGH;
    }
    return EvidenceCoverageLevel.COMPLETE;
  }

  private static Set<MatchDimension> copyDimensions(
      Set<MatchDimension> dimensions,
      String name) {
    Objects.requireNonNull(dimensions, name + " must not be null");
    EnumSet<MatchDimension> copied = EnumSet.noneOf(MatchDimension.class);
    for (MatchDimension dimension : dimensions) {
      copied.add(Objects.requireNonNull(dimension, name + " must not contain null"));
    }
    return Collections.unmodifiableSet(copied);
  }
}
