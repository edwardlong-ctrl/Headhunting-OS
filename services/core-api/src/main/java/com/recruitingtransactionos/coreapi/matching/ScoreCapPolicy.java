package com.recruitingtransactionos.coreapi.matching;

public final class ScoreCapPolicy {

  public ScoreCapDecision decide(ScoreCapPolicyRequest request) {
    if (request == null) {
      return new ScoreCapDecision(
          MatchScore.of(1),
          MatchScore.of(1),
          false,
          ScoreCapReason.POLICY_INPUT_REQUIRED,
          ScoreCapReason.POLICY_INPUT_REQUIRED.safeExplanation(),
          true,
          true,
          true);
    }

    DecisionState state = new DecisionState(request.proposedScore());

    if (request.evidenceCoverage().independentHighTrustEvidenceCount() < 2) {
      state.applyCap(
          4,
          ScoreCapReason.INSUFFICIENT_INDEPENDENT_HIGH_TRUST_EVIDENCE,
          false,
          true);
    }
    if (request.industryPackMaturity() == IndustryPackMaturity.COLD) {
      state.applyCap(3, ScoreCapReason.COLD_INDUSTRY_PACK, false, true);
    }
    if (request.keywordOnlyEvidence() && !request.projectEvidencePresent()) {
      state.applyCap(
          3,
          ScoreCapReason.KEYWORD_ONLY_WITHOUT_PROJECT_EVIDENCE,
          false,
          true);
    }
    if (request.candidateIntentSignalStrength() == EvidenceAssertionStrength.WEAK_SIGNAL) {
      state.applyCap(3, ScoreCapReason.WEAK_SIGNAL_INTENT_ONLY, false, true);
    }
    if (request.ontologyStale() || request.industryPackVersionStale()) {
      state.applyCap(4, ScoreCapReason.STALE_ONTOLOGY_OR_INDUSTRY_PACK, false, true);
    }
    if (request.authenticityRisk() == AuthenticityRiskLevel.HIGH
        || request.authenticityRisk() == AuthenticityRiskLevel.UNKNOWN) {
      state.applyCap(4, ScoreCapReason.HIGH_AUTHENTICITY_RISK, true, true);
    }
    if (request.reidentificationRiskSignal() == ReidentificationRiskSignal.HIGH
        || request.reidentificationRiskSignal() == ReidentificationRiskSignal.UNKNOWN) {
      state.blockClientDelivery(ScoreCapReason.HIGH_REIDENTIFICATION_RISK);
    }

    return state.toDecision();
  }

  private static final class DecisionState {
    private final MatchScore proposedScore;
    private int maxAllowedScore = 5;
    private ScoreCapReason reasonCode = ScoreCapReason.NONE;
    private boolean humanReviewRequired;
    private boolean additionalEvidenceRequired;
    private boolean clientDeliveryBlocked;

    private DecisionState(MatchScore proposedScore) {
      this.proposedScore = proposedScore;
    }

    private void applyCap(
        int maxScore,
        ScoreCapReason reason,
        boolean humanReviewRequired,
        boolean additionalEvidenceRequired) {
      if (maxScore < maxAllowedScore) {
        maxAllowedScore = maxScore;
        reasonCode = reason;
      }
      this.humanReviewRequired = this.humanReviewRequired || humanReviewRequired;
      this.additionalEvidenceRequired =
          this.additionalEvidenceRequired || additionalEvidenceRequired;
    }

    private void blockClientDelivery(ScoreCapReason reason) {
      clientDeliveryBlocked = true;
      humanReviewRequired = true;
      if (reasonCode == ScoreCapReason.NONE) {
        reasonCode = reason;
      }
    }

    private ScoreCapDecision toDecision() {
      MatchScore cappedScore = MatchScore.of(Math.min(proposedScore.value(), maxAllowedScore));
      return new ScoreCapDecision(
          proposedScore,
          cappedScore,
          cappedScore.value() < proposedScore.value(),
          reasonCode,
          reasonCode.safeExplanation(),
          humanReviewRequired,
          additionalEvidenceRequired,
          clientDeliveryBlocked);
    }
  }
}
