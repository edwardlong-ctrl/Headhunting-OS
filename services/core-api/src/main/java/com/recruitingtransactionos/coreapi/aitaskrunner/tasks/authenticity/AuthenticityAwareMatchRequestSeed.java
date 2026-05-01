package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity;

import com.recruitingtransactionos.coreapi.matching.EvidenceAssertionStrength;
import com.recruitingtransactionos.coreapi.matching.EvidenceCoverageInput;
import com.recruitingtransactionos.coreapi.matching.IndustryPackMaturity;
import com.recruitingtransactionos.coreapi.matching.MatchDimension;
import com.recruitingtransactionos.coreapi.matching.MatchJobRef;
import com.recruitingtransactionos.coreapi.matching.MatchReportId;
import com.recruitingtransactionos.coreapi.matching.MatchScore;
import com.recruitingtransactionos.coreapi.matching.MatchSubjectRef;
import com.recruitingtransactionos.coreapi.matching.ReidentificationRiskSignal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AuthenticityAwareMatchRequestSeed(
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
    ReidentificationRiskSignal reidentificationRiskSignal,
    String ontologyVersion,
    String industryPackVersion,
    Instant generatedAt) {

  public AuthenticityAwareMatchRequestSeed {
    Objects.requireNonNull(matchReportId, "matchReportId must not be null");
    Objects.requireNonNull(jobRef, "jobRef must not be null");
    Objects.requireNonNull(candidateCardRef, "candidateCardRef must not be null");
    Objects.requireNonNull(requestedOverallScore, "requestedOverallScore must not be null");
    Objects.requireNonNull(requestedDimensionScores, "requestedDimensionScores must not be null");
    Objects.requireNonNull(evidenceCoverageInput, "evidenceCoverageInput must not be null");
    Objects.requireNonNull(industryPackMaturity, "industryPackMaturity must not be null");
    Objects.requireNonNull(candidateIntentSignalStrength, "candidateIntentSignalStrength must not be null");
    Objects.requireNonNull(reidentificationRiskSignal, "reidentificationRiskSignal must not be null");
    Objects.requireNonNull(ontologyVersion, "ontologyVersion must not be null");
    Objects.requireNonNull(industryPackVersion, "industryPackVersion must not be null");
    Objects.requireNonNull(generatedAt, "generatedAt must not be null");
  }
}
