package com.recruitingtransactionos.coreapi.truthlayer;

import java.util.Objects;

public record CanonicalWriteRequest(
    ClaimInput claim,
    VerificationStatus targetVerificationStatus,
    RiskTier targetRiskTier,
    boolean clientVisible,
    boolean conflictsWithCanonical,
    boolean explicitReviewApproved) {

  public CanonicalWriteRequest {
    Objects.requireNonNull(claim, "claim must not be null");
    Objects.requireNonNull(targetVerificationStatus, "targetVerificationStatus must not be null");
    Objects.requireNonNull(targetRiskTier, "targetRiskTier must not be null");
  }
}
