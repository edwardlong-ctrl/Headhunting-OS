package com.recruitingtransactionos.coreapi.matching;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class MatchReportGenerationService {

  private final ScoreCapPolicy scoreCapPolicy;
  private final ProvenanceWeightingPolicy provenancePolicy;

  public MatchReportGenerationService() {
    this(new ScoreCapPolicy(), new ProvenanceWeightingPolicy());
  }

  MatchReportGenerationService(
      ScoreCapPolicy scoreCapPolicy,
      ProvenanceWeightingPolicy provenancePolicy) {
    this.scoreCapPolicy = Objects.requireNonNull(scoreCapPolicy, "scoreCapPolicy must not be null");
    this.provenancePolicy =
        Objects.requireNonNull(provenancePolicy, "provenancePolicy must not be null");
  }

  public MatchReportGenerationResult generate(MatchReportGenerationRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    MatchEvidenceSummary evidenceSummary =
        MatchEvidenceSummary.from(request.evidenceCoverageInput(), provenancePolicy);
    ProvenanceSummary provenanceSummary = provenancePolicy.summarize(
        request.evidenceCoverageInput().evidenceSignals(),
        request.authenticityRisk());
    ScoreCapDecision scoreCapDecision = scoreCapPolicy.decide(ScoreCapPolicyRequest
        .builder(request.requestedOverallScore())
        .evidenceCoverage(evidenceSummary.evidenceCoverage())
        .industryPackMaturity(request.industryPackMaturity())
        .keywordOnlyEvidence(request.keywordOnlyEvidence())
        .projectEvidencePresent(request.projectEvidencePresent())
        .candidateIntentSignalStrength(request.candidateIntentSignalStrength())
        .ontologyStale(request.ontologyStale())
        .industryPackVersionStale(request.industryPackVersionStale())
        .authenticityRisk(provenanceSummary.authenticityRisk())
        .reidentificationRiskSignal(request.reidentificationRiskSignal())
        .build());

    MatchReport report = new MatchReport(
        request.matchReportId(),
        request.jobRef(),
        request.candidateCardRef(),
        scoreCapDecision.cappedScore(),
        cappedDimensionScores(request.requestedDimensionScores(), scoreCapDecision.cappedScore()),
        confidence(evidenceSummary, provenanceSummary, scoreCapDecision),
        evidenceSummary.evidenceCoverage(),
        provenanceSummary,
        scoreCapDecision,
        request.ontologyVersion(),
        request.industryPackVersion(),
        request.generatedAt());

    return new MatchReportGenerationResult(report, evidenceSummary);
  }

  private static Map<MatchDimension, MatchScore> cappedDimensionScores(
      Map<MatchDimension, MatchScore> requestedDimensionScores,
      MatchScore cap) {
    EnumMap<MatchDimension, MatchScore> cappedScores = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      MatchScore requested = requestedDimensionScores.get(dimension);
      cappedScores.put(dimension, MatchScore.of(Math.min(requested.value(), cap.value())));
    }
    return Collections.unmodifiableMap(cappedScores);
  }

  private static ScoreConfidence confidence(
      MatchEvidenceSummary evidenceSummary,
      ProvenanceSummary provenanceSummary,
      ScoreCapDecision scoreCapDecision) {
    if (scoreCapDecision.clientDeliveryBlocked()
        || !evidenceSummary.missingEvidenceDimensions().isEmpty()
        || !evidenceSummary.weakSignalOnlyDimensions().isEmpty()
        || provenanceSummary.strongestSourceStrength() == ProvenanceSourceStrength.LOW_TRUST
        || provenanceSummary.assertionStrength() != EvidenceAssertionStrength.EXPLICIT
        || provenanceSummary.authenticityRisk() != AuthenticityRiskLevel.LOW
        || scoreCapDecision.cappedScore().value() <= 3) {
      return ScoreConfidence.LOW;
    }
    if (!scoreCapDecision.capApplied()
        && !scoreCapDecision.humanReviewRequired()
        && evidenceSummary.evidenceCoverage().coverageLevel() == EvidenceCoverageLevel.COMPLETE
        && evidenceSummary.independentHighTrustEvidenceCount() >= 2
        && provenanceSummary.strongestSourceStrength() == ProvenanceSourceStrength.HIGH_TRUST) {
      return ScoreConfidence.HIGH;
    }
    return ScoreConfidence.MEDIUM;
  }
}
