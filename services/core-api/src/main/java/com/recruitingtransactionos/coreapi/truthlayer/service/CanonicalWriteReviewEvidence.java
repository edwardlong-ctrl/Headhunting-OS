package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import java.util.Objects;

public record CanonicalWriteReviewEvidence(
    ReviewEventId reviewEventId,
    ReviewDecision decision,
    boolean bulkApproval,
    boolean transactionLegalApproval,
    String reason) {

  public CanonicalWriteReviewEvidence {
    Objects.requireNonNull(reviewEventId, "reviewEventId must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    reason = requireNonBlank(reason, "reviewEvidence.reason");
  }

  boolean isApproved() {
    return decision == ReviewDecision.APPROVED;
  }

  boolean isExplicitApprovalFor(RiskTier riskTier) {
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    if (!isApproved() || bulkApproval) {
      return false;
    }
    if (riskTier == RiskTier.T4_TRANSACTION_LEGAL) {
      return transactionLegalApproval;
    }
    return true;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
