package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantMatchReportResponse(
    String matchReportId,
    String subjectType,
    String subjectRef,
    int finalScore,
    boolean capApplied,
    String capReason,
    String capSafeExplanation,
    String confidence,
    String authenticityRisk,
    String reidentificationRiskSignal,
    String industryPackKey,
    String industryPackMaturity,
    Boolean ontologyStale,
    String selectionReason,
    String ontologyVersion,
    String industryPackVersion,
    String generatedAt,
    List<DimensionScore> dimensionScores,
    EvidenceCoverageSummary evidenceCoverage,
    ProvenanceSummaryResponse provenanceSummary,
    List<String> antiPatternWarnings,
    List<String> explanations,
    List<String> interviewQuestions
) implements ApiSafeResponseBody {

  public ConsultantMatchReportResponse(
      String matchReportId,
      String subjectType,
      String subjectRef,
      int finalScore,
      boolean capApplied,
      String capReason,
      String capSafeExplanation,
      String confidence,
      String authenticityRisk,
      String reidentificationRiskSignal,
      String ontologyVersion,
      String industryPackVersion,
      String generatedAt,
      List<DimensionScore> dimensionScores,
      EvidenceCoverageSummary evidenceCoverage,
      ProvenanceSummaryResponse provenanceSummary,
      List<String> explanations,
      List<String> interviewQuestions) {
    this(
        matchReportId,
        subjectType,
        subjectRef,
        finalScore,
        capApplied,
        capReason,
        capSafeExplanation,
        confidence,
        authenticityRisk,
        reidentificationRiskSignal,
        null,
        null,
        null,
        null,
        ontologyVersion,
        industryPackVersion,
        generatedAt,
        dimensionScores,
        evidenceCoverage,
        provenanceSummary,
        List.of(),
        explanations,
        interviewQuestions);
  }

  public record DimensionScore(
      String dimension,
      int score
  ) {}

  public record EvidenceCoverageSummary(
      double coverageRatio,
      String coverageLevel,
      int independentEvidenceCount,
      int independentHighTrustEvidenceCount
  ) {}

  public record ProvenanceSummaryResponse(
      String strongestProvenanceCategory,
      String strongestSourceStrength,
      double provenanceWeight,
      String assertionStrength
  ) {}
}
