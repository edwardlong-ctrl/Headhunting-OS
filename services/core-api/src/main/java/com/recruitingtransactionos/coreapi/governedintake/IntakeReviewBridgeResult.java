package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import java.util.Objects;
import java.util.UUID;

public record IntakeReviewBridgeResult(
    UUID organizationId,
    ClaimId claimLedgerItemId,
    ReviewEventId reviewEventId,
    ReviewEventId existingReviewEventId,
    IntakeReviewBridgeStatus status,
    String skippedReason,
    String blockedReason,
    String summary) {

  public IntakeReviewBridgeResult {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(claimLedgerItemId, "claimLedgerItemId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    skippedReason = GovernedIntakeGuards.optionalNonBlank(skippedReason, "skippedReason");
    blockedReason = GovernedIntakeGuards.optionalNonBlank(blockedReason, "blockedReason");
    summary = GovernedIntakeGuards.requireNonBlank(summary, "summary");
    if (reviewEventId != null && existingReviewEventId != null) {
      throw new IllegalArgumentException(
          "reviewEventId and existingReviewEventId cannot both be present");
    }
  }
}
