package com.recruitingtransactionos.coreapi.matching;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record MatchReport(
    MatchReportId matchReportId,
    MatchJobRef jobRef,
    MatchSubjectRef candidateCardRef,
    MatchScore overallScore,
    Map<MatchDimension, MatchScore> dimensionScores,
    ScoreConfidence scoreConfidence,
    EvidenceCoverage evidenceCoverage,
    ProvenanceSummary provenanceSummary,
    ScoreCapDecision scoreCapDecision,
    String ontologyVersion,
    String industryPackVersion,
    Instant generatedAt) {

  public MatchReport {
    Objects.requireNonNull(matchReportId, "matchReportId must not be null");
    Objects.requireNonNull(jobRef, "jobRef must not be null");
    Objects.requireNonNull(candidateCardRef, "candidateCardRef must not be null");
    Objects.requireNonNull(overallScore, "overallScore must not be null");
    dimensionScores = copyDimensionScores(dimensionScores);
    Objects.requireNonNull(scoreConfidence, "scoreConfidence must not be null");
    Objects.requireNonNull(evidenceCoverage, "evidenceCoverage must not be null");
    Objects.requireNonNull(provenanceSummary, "provenanceSummary must not be null");
    Objects.requireNonNull(scoreCapDecision, "scoreCapDecision must not be null");
    ontologyVersion = MatchingGuards.requireNonBlank(ontologyVersion, "ontologyVersion");
    industryPackVersion =
        MatchingGuards.requireNonBlank(industryPackVersion, "industryPackVersion");
    Objects.requireNonNull(generatedAt, "generatedAt must not be null");
  }

  public boolean isCanonicalFact() {
    return false;
  }

  public boolean isClientSafeApiOutput() {
    return false;
  }

  private static Map<MatchDimension, MatchScore> copyDimensionScores(
      Map<MatchDimension, MatchScore> dimensionScores) {
    Objects.requireNonNull(dimensionScores, "dimensionScores must not be null");
    EnumMap<MatchDimension, MatchScore> copied = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      MatchScore score = dimensionScores.get(dimension);
      if (score == null) {
        throw new IllegalArgumentException("dimensionScores must include " + dimension.name());
      }
      copied.put(dimension, score);
    }
    return Collections.unmodifiableMap(copied);
  }
}
