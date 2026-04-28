package com.recruitingtransactionos.coreapi.matching;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record MatchReportGenerationRequest(
    MatchReportId matchReportId,
    MatchJobRef jobRef,
    MatchSubjectRef candidateCardRef,
    MatchScore requestedOverallScore,
    Map<MatchDimension, MatchScore> requestedDimensionScores,
    EvidenceCoverageInput evidenceCoverageInput,
    IndustryPackMaturity industryPackMaturity,
    boolean keywordOnlyEvidence,
    boolean projectEvidencePresent,
    EvidenceAssertionStrength candidateIntentSignalStrength,
    boolean ontologyStale,
    boolean industryPackVersionStale,
    AuthenticityRiskLevel authenticityRisk,
    ReidentificationRiskSignal reidentificationRiskSignal,
    String ontologyVersion,
    String industryPackVersion,
    Instant generatedAt) {

  public MatchReportGenerationRequest {
    Objects.requireNonNull(matchReportId, "matchReportId must not be null");
    Objects.requireNonNull(jobRef, "jobRef must not be null");
    Objects.requireNonNull(candidateCardRef, "candidateCardRef must not be null");
    Objects.requireNonNull(requestedOverallScore, "requestedOverallScore must not be null");
    requestedDimensionScores = copyDimensionScores(requestedDimensionScores);
    Objects.requireNonNull(evidenceCoverageInput, "evidenceCoverageInput must not be null");
    Objects.requireNonNull(industryPackMaturity, "industryPackMaturity must not be null");
    Objects.requireNonNull(
        candidateIntentSignalStrength, "candidateIntentSignalStrength must not be null");
    if (candidateIntentSignalStrength == EvidenceAssertionStrength.UNKNOWN) {
      throw new IllegalArgumentException(
          "candidateIntentSignalStrength UNKNOWN is not allowed for generation");
    }
    Objects.requireNonNull(authenticityRisk, "authenticityRisk must not be null");
    if (authenticityRisk == AuthenticityRiskLevel.UNKNOWN) {
      throw new IllegalArgumentException("authenticityRisk UNKNOWN is not allowed for generation");
    }
    Objects.requireNonNull(
        reidentificationRiskSignal, "reidentificationRiskSignal must not be null");
    if (reidentificationRiskSignal == ReidentificationRiskSignal.UNKNOWN) {
      throw new IllegalArgumentException(
          "reidentificationRiskSignal UNKNOWN is not allowed for generation");
    }
    ontologyVersion = MatchingGuards.requireNonBlank(ontologyVersion, "ontologyVersion");
    industryPackVersion =
        MatchingGuards.requireNonBlank(industryPackVersion, "industryPackVersion");
    Objects.requireNonNull(generatedAt, "generatedAt must not be null");
  }

  private static Map<MatchDimension, MatchScore> copyDimensionScores(
      Map<MatchDimension, MatchScore> requestedDimensionScores) {
    Objects.requireNonNull(requestedDimensionScores, "requestedDimensionScores must not be null");
    EnumMap<MatchDimension, MatchScore> copied = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      MatchScore score = requestedDimensionScores.get(dimension);
      if (score == null) {
        throw new IllegalArgumentException(
            "requestedDimensionScores must include " + dimension.name());
      }
      copied.put(dimension, score);
    }
    return Collections.unmodifiableMap(copied);
  }
}
