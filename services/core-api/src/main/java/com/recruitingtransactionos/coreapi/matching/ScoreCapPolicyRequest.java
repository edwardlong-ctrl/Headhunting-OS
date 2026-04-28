package com.recruitingtransactionos.coreapi.matching;

import java.util.Objects;

public record ScoreCapPolicyRequest(
    MatchScore proposedScore,
    EvidenceCoverage evidenceCoverage,
    IndustryPackMaturity industryPackMaturity,
    boolean keywordOnlyEvidence,
    boolean projectEvidencePresent,
    EvidenceAssertionStrength candidateIntentSignalStrength,
    boolean ontologyStale,
    boolean industryPackVersionStale,
    AuthenticityRiskLevel authenticityRisk,
    ReidentificationRiskSignal reidentificationRiskSignal) {

  public ScoreCapPolicyRequest {
    Objects.requireNonNull(proposedScore, "proposedScore must not be null");
    Objects.requireNonNull(evidenceCoverage, "evidenceCoverage must not be null");
    Objects.requireNonNull(industryPackMaturity, "industryPackMaturity must not be null");
    Objects.requireNonNull(
        candidateIntentSignalStrength, "candidateIntentSignalStrength must not be null");
    Objects.requireNonNull(authenticityRisk, "authenticityRisk must not be null");
    Objects.requireNonNull(
        reidentificationRiskSignal, "reidentificationRiskSignal must not be null");
  }

  public static Builder builder(MatchScore proposedScore) {
    return new Builder(proposedScore);
  }

  public static final class Builder {
    private final MatchScore proposedScore;
    private EvidenceCoverage evidenceCoverage;
    private IndustryPackMaturity industryPackMaturity;
    private boolean keywordOnlyEvidence;
    private boolean projectEvidencePresent;
    private EvidenceAssertionStrength candidateIntentSignalStrength;
    private boolean ontologyStale;
    private boolean industryPackVersionStale;
    private AuthenticityRiskLevel authenticityRisk;
    private ReidentificationRiskSignal reidentificationRiskSignal;

    private Builder(MatchScore proposedScore) {
      this.proposedScore = Objects.requireNonNull(proposedScore, "proposedScore must not be null");
    }

    public Builder evidenceCoverage(EvidenceCoverage evidenceCoverage) {
      this.evidenceCoverage = evidenceCoverage;
      return this;
    }

    public Builder industryPackMaturity(IndustryPackMaturity industryPackMaturity) {
      this.industryPackMaturity = industryPackMaturity;
      return this;
    }

    public Builder keywordOnlyEvidence(boolean keywordOnlyEvidence) {
      this.keywordOnlyEvidence = keywordOnlyEvidence;
      return this;
    }

    public Builder projectEvidencePresent(boolean projectEvidencePresent) {
      this.projectEvidencePresent = projectEvidencePresent;
      return this;
    }

    public Builder candidateIntentSignalStrength(
        EvidenceAssertionStrength candidateIntentSignalStrength) {
      this.candidateIntentSignalStrength = candidateIntentSignalStrength;
      return this;
    }

    public Builder ontologyStale(boolean ontologyStale) {
      this.ontologyStale = ontologyStale;
      return this;
    }

    public Builder industryPackVersionStale(boolean industryPackVersionStale) {
      this.industryPackVersionStale = industryPackVersionStale;
      return this;
    }

    public Builder authenticityRisk(AuthenticityRiskLevel authenticityRisk) {
      this.authenticityRisk = authenticityRisk;
      return this;
    }

    public Builder reidentificationRiskSignal(
        ReidentificationRiskSignal reidentificationRiskSignal) {
      this.reidentificationRiskSignal = reidentificationRiskSignal;
      return this;
    }

    public ScoreCapPolicyRequest build() {
      return new ScoreCapPolicyRequest(
          proposedScore,
          evidenceCoverage,
          industryPackMaturity,
          keywordOnlyEvidence,
          projectEvidencePresent,
          candidateIntentSignalStrength,
          ontologyStale,
          industryPackVersionStale,
          authenticityRisk,
          reidentificationRiskSignal);
    }
  }
}
