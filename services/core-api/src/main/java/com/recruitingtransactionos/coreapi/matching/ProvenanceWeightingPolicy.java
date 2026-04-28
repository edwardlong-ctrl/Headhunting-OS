package com.recruitingtransactionos.coreapi.matching;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ProvenanceWeightingPolicy {

  public ProvenanceSummary summarize(
      List<MatchEvidenceSignal> evidenceSignals,
      AuthenticityRiskLevel authenticityRisk) {
    Objects.requireNonNull(evidenceSignals, "evidenceSignals must not be null");
    Objects.requireNonNull(authenticityRisk, "authenticityRisk must not be null");
    if (evidenceSignals.isEmpty()) {
      throw new IllegalArgumentException("evidenceSignals must not be empty for generation");
    }
    if (authenticityRisk == AuthenticityRiskLevel.UNKNOWN) {
      throw new IllegalArgumentException("authenticityRisk UNKNOWN is not allowed for generation");
    }

    for (MatchEvidenceSignal signal : evidenceSignals) {
      requireKnown(signal);
    }

    MatchEvidenceSignal strongestSignal = evidenceSignals.stream()
        .max(Comparator
            .comparingDouble((MatchEvidenceSignal signal) -> weightFor(signal.provenanceCategory()))
            .thenComparingInt(signal -> assertionRank(signal.assertionStrength())))
        .orElseThrow();

    EvidenceAssertionStrength strongestAssertion = evidenceSignals.stream()
        .map(MatchEvidenceSignal::assertionStrength)
        .max(Comparator.comparingInt(this::assertionRank))
        .orElseThrow();

    return new ProvenanceSummary(
        strongestSignal.provenanceCategory(),
        sourceStrengthFor(strongestSignal.provenanceCategory()),
        ProvenanceWeight.of(weightFor(strongestSignal.provenanceCategory())),
        strongestAssertion,
        authenticityRisk);
  }

  boolean isHighTrust(ProvenanceCategory provenanceCategory) {
    return sourceStrengthFor(provenanceCategory) == ProvenanceSourceStrength.HIGH_TRUST;
  }

  boolean isWeakSignalOnlySupport(MatchEvidenceSignal signal) {
    requireKnown(signal);
    return signal.assertionStrength() == EvidenceAssertionStrength.WEAK_SIGNAL
        || signal.provenanceCategory() == ProvenanceCategory.AI_EXTRACTED
        || signal.provenanceCategory() == ProvenanceCategory.SYSTEM_INFERENCE
        || signal.provenanceCategory() == ProvenanceCategory.WEAK_SIGNAL
        || signal.provenanceCategory() == ProvenanceCategory.AI_OPTIMIZED_TEXT;
  }

  void requireKnown(MatchEvidenceSignal signal) {
    Objects.requireNonNull(signal, "evidence signal must not be null");
    if (signal.provenanceCategory() == ProvenanceCategory.UNKNOWN) {
      throw new IllegalArgumentException("provenanceCategory UNKNOWN is not allowed for generation");
    }
    if (signal.assertionStrength() == EvidenceAssertionStrength.UNKNOWN) {
      throw new IllegalArgumentException("assertionStrength UNKNOWN is not allowed for generation");
    }
  }

  private ProvenanceSourceStrength sourceStrengthFor(ProvenanceCategory provenanceCategory) {
    return switch (provenanceCategory) {
      case EXTERNAL_VERIFIED, CANDIDATE_CONFIRMED, VERIFIED_WORK_SAMPLE, CUSTOMER_FEEDBACK ->
          ProvenanceSourceStrength.HIGH_TRUST;
      case CONSULTANT_ATTESTED, HUMAN_ACKNOWLEDGED, CONSULTANT_DEEP_DIVE, CANDIDATE_FORM ->
          ProvenanceSourceStrength.MEDIUM_TRUST;
      case AI_EXTRACTED, SYSTEM_INFERENCE, WEAK_SIGNAL, CV_OR_LINKEDIN, AI_OPTIMIZED_TEXT ->
          ProvenanceSourceStrength.LOW_TRUST;
      case UNKNOWN -> ProvenanceSourceStrength.UNKNOWN;
    };
  }

  private double weightFor(ProvenanceCategory provenanceCategory) {
    return switch (provenanceCategory) {
      case EXTERNAL_VERIFIED -> 0.95;
      case VERIFIED_WORK_SAMPLE -> 0.90;
      case CANDIDATE_CONFIRMED -> 0.85;
      case CUSTOMER_FEEDBACK -> 0.80;
      case CONSULTANT_ATTESTED, CONSULTANT_DEEP_DIVE -> 0.70;
      case HUMAN_ACKNOWLEDGED -> 0.60;
      case CANDIDATE_FORM -> 0.55;
      case CV_OR_LINKEDIN -> 0.45;
      case AI_EXTRACTED -> 0.35;
      case SYSTEM_INFERENCE -> 0.25;
      case WEAK_SIGNAL, AI_OPTIMIZED_TEXT -> 0.15;
      case UNKNOWN -> 0.0;
    };
  }

  private int assertionRank(EvidenceAssertionStrength assertionStrength) {
    return switch (assertionStrength) {
      case EXPLICIT -> 4;
      case IMPLIED -> 3;
      case WEAK_SIGNAL -> 2;
      case CONTRADICTION -> 1;
      case UNKNOWN -> 0;
    };
  }
}
