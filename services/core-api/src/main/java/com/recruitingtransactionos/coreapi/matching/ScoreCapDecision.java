package com.recruitingtransactionos.coreapi.matching;

import java.util.Objects;

public record ScoreCapDecision(
    MatchScore proposedScore,
    MatchScore cappedScore,
    boolean capApplied,
    ScoreCapReason reasonCode,
    String safeExplanation,
    boolean humanReviewRequired,
    boolean additionalEvidenceRequired,
    boolean clientDeliveryBlocked) {

  public ScoreCapDecision {
    Objects.requireNonNull(proposedScore, "proposedScore must not be null");
    Objects.requireNonNull(cappedScore, "cappedScore must not be null");
    if (cappedScore.value() > proposedScore.value()) {
      throw new IllegalArgumentException("cappedScore must not exceed proposedScore");
    }
    capApplied = cappedScore.value() < proposedScore.value();
    Objects.requireNonNull(reasonCode, "reasonCode must not be null");
    safeExplanation = MatchingGuards.requireSafeExplanation(safeExplanation);
  }
}
