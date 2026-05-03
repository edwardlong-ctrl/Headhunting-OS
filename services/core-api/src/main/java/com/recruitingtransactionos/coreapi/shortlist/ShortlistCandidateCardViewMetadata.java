package com.recruitingtransactionos.coreapi.shortlist;

import java.util.List;
import java.util.Objects;

public record ShortlistCandidateCardViewMetadata(
    String anonymousCandidateRef,
    String projectionVersion,
    String redactionLevel,
    String generalizedHeadline,
    String generalizedRoleFamily,
    String generalizedSeniorityBand,
    String generalizedLocationRegion,
    String safeSummary,
    String safeSkillSummary,
    List<String> safeEvidenceSummaries,
    List<String> safeMatchNarratives,
    Integer overallScore,
    String confidence,
    String reidentificationRiskSignal,
    List<DimensionScoreItem> dimensionScores) {

  public ShortlistCandidateCardViewMetadata {
    anonymousCandidateRef = requireNonBlank(anonymousCandidateRef, "anonymousCandidateRef");
    projectionVersion = requireNonBlank(projectionVersion, "projectionVersion");
    redactionLevel = requireNonBlank(redactionLevel, "redactionLevel");
    generalizedHeadline = requireNonBlank(generalizedHeadline, "generalizedHeadline");
    generalizedRoleFamily = requireNonBlank(generalizedRoleFamily, "generalizedRoleFamily");
    generalizedSeniorityBand = requireNonBlank(generalizedSeniorityBand, "generalizedSeniorityBand");
    generalizedLocationRegion = requireNonBlank(generalizedLocationRegion, "generalizedLocationRegion");
    safeSummary = requireNonBlank(safeSummary, "safeSummary");
    safeSkillSummary = requireNonBlank(safeSkillSummary, "safeSkillSummary");
    safeEvidenceSummaries = List.copyOf(
        Objects.requireNonNull(safeEvidenceSummaries, "safeEvidenceSummaries must not be null"));
    safeMatchNarratives = List.copyOf(
        Objects.requireNonNull(safeMatchNarratives, "safeMatchNarratives must not be null"));
    confidence = requireNonBlank(confidence, "confidence");
    reidentificationRiskSignal =
        requireNonBlank(reidentificationRiskSignal, "reidentificationRiskSignal");
    dimensionScores =
        List.copyOf(Objects.requireNonNull(dimensionScores, "dimensionScores must not be null"));
  }

  public record DimensionScoreItem(String dimension, int score) {

    public DimensionScoreItem {
      dimension = requireNonBlank(dimension, "dimension");
    }
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
