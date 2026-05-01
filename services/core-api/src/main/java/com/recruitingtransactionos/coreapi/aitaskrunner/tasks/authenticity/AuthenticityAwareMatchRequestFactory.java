package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity;

import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationRequest;
import java.util.Objects;

public final class AuthenticityAwareMatchRequestFactory {

  public MatchReportGenerationRequest create(
      AuthenticityAwareMatchRequestSeed seed,
      AuthenticityRiskAssessorOutput assessment) {
    Objects.requireNonNull(seed, "seed must not be null");
    Objects.requireNonNull(assessment, "assessment must not be null");
    return new MatchReportGenerationRequest(
        seed.matchReportId(),
        seed.jobRef(),
        seed.candidateCardRef(),
        seed.requestedOverallScore(),
        seed.requestedDimensionScores(),
        seed.evidenceCoverageInput(),
        seed.industryPackMaturity(),
        seed.keywordOnlyEvidence(),
        seed.projectEvidencePresent(),
        seed.candidateIntentSignalStrength(),
        seed.ontologyStale(),
        seed.industryPackVersionStale(),
        assessment.toRiskLevel(),
        seed.reidentificationRiskSignal(),
        seed.ontologyVersion(),
        seed.industryPackVersion(),
        seed.generatedAt());
  }
}
