package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import java.util.Objects;
import java.util.UUID;

public record IntakeReviewBridgeRequest(
    UUID organizationId,
    ClaimId claimLedgerItemId,
    ActorRole reviewerActorType,
    UUID reviewerActorId,
    ReviewDecision reviewDecision,
    RiskTier riskTier,
    boolean bulkFlag,
    String reason,
    UUID correlationId,
    UUID causationId,
    IntakeReviewBridgePolicy reviewPolicy) {

  public IntakeReviewBridgeRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(claimLedgerItemId, "claimLedgerItemId must not be null");
    Objects.requireNonNull(reviewerActorType, "reviewerActorType must not be null");
    Objects.requireNonNull(reviewerActorId, "reviewerActorId must not be null");
    Objects.requireNonNull(reviewDecision, "reviewDecision must not be null");
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    reason = GovernedIntakeGuards.requireNonBlank(reason, "reason");
    Objects.requireNonNull(reviewPolicy, "reviewPolicy must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID organizationId;
    private ClaimId claimLedgerItemId;
    private ActorRole reviewerActorType;
    private UUID reviewerActorId;
    private ReviewDecision reviewDecision;
    private RiskTier riskTier;
    private boolean bulkFlag;
    private String reason;
    private UUID correlationId;
    private UUID causationId;
    private IntakeReviewBridgePolicy reviewPolicy;

    private Builder() {
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder claimLedgerItemId(ClaimId claimLedgerItemId) {
      this.claimLedgerItemId = claimLedgerItemId;
      return this;
    }

    public Builder reviewerActorType(ActorRole reviewerActorType) {
      this.reviewerActorType = reviewerActorType;
      return this;
    }

    public Builder reviewerActorId(UUID reviewerActorId) {
      this.reviewerActorId = reviewerActorId;
      return this;
    }

    public Builder reviewDecision(ReviewDecision reviewDecision) {
      this.reviewDecision = reviewDecision;
      return this;
    }

    public Builder riskTier(RiskTier riskTier) {
      this.riskTier = riskTier;
      return this;
    }

    public Builder bulkFlag(boolean bulkFlag) {
      this.bulkFlag = bulkFlag;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder correlationId(UUID correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder causationId(UUID causationId) {
      this.causationId = causationId;
      return this;
    }

    public Builder reviewPolicy(IntakeReviewBridgePolicy reviewPolicy) {
      this.reviewPolicy = reviewPolicy;
      return this;
    }

    public IntakeReviewBridgeRequest build() {
      return new IntakeReviewBridgeRequest(
          organizationId,
          claimLedgerItemId,
          reviewerActorType,
          reviewerActorId,
          reviewDecision,
          riskTier,
          bulkFlag,
          reason,
          correlationId,
          causationId,
          reviewPolicy);
    }
  }
}
