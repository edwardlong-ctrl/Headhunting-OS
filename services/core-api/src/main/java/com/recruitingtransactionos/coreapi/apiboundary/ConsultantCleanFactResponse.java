package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.Objects;

public record ConsultantCleanFactResponse(
    String claimId,
    String claimFieldName,
    String targetEntityType,
    String targetFieldPath,
    String proposedValue,
    String suggestedVerificationStatus,
    String suggestedRiskTier,
    String entityResolutionStatus,
    String latestReviewDecision,
    String latestDecisionId,
    boolean conflictsWithCanonical,
    String rationale,
    ConsultantSourceHighlightResponse sourceHighlight) {

  public ConsultantCleanFactResponse {
    Objects.requireNonNull(sourceHighlight, "sourceHighlight must not be null");
  }
}
