package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import java.util.Objects;
import java.util.UUID;

public record ReviewEventForCanonicalWrite(
    ReviewEventId reviewEventId,
    UUID organizationId,
    EntityRef targetEntity,
    String targetFieldPath,
    ClaimId claimLedgerItemId,
    String sourceSpanReference,
    ReviewDecision decision,
    RiskTier riskTier,
    boolean bulkApproval,
    UUID reviewerId,
    String reason) {

  public ReviewEventForCanonicalWrite {
    Objects.requireNonNull(reviewEventId, "reviewEventId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = requireNonBlank(targetFieldPath, "targetFieldPath");
    Objects.requireNonNull(claimLedgerItemId, "claimLedgerItemId must not be null");
    sourceSpanReference = optionalNonBlank(sourceSpanReference, "sourceSpanReference");
    Objects.requireNonNull(decision, "decision must not be null");
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    Objects.requireNonNull(reviewerId, "reviewerId must not be null");
    reason = requireNonBlank(reason, "reason");
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String optionalNonBlank(String value, String name) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  public static final class Builder {
    private ReviewEventId reviewEventId;
    private UUID organizationId;
    private EntityRef targetEntity;
    private String targetFieldPath;
    private ClaimId claimLedgerItemId;
    private String sourceSpanReference;
    private ReviewDecision decision;
    private RiskTier riskTier;
    private boolean bulkApproval;
    private UUID reviewerId;
    private String reason;

    private Builder() {
    }

    public Builder reviewEventId(ReviewEventId reviewEventId) {
      this.reviewEventId = reviewEventId;
      return this;
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder targetEntity(EntityRef targetEntity) {
      this.targetEntity = targetEntity;
      return this;
    }

    public Builder targetFieldPath(String targetFieldPath) {
      this.targetFieldPath = targetFieldPath;
      return this;
    }

    public Builder claimLedgerItemId(ClaimId claimLedgerItemId) {
      this.claimLedgerItemId = claimLedgerItemId;
      return this;
    }

    public Builder sourceSpanReference(String sourceSpanReference) {
      this.sourceSpanReference = sourceSpanReference;
      return this;
    }

    public Builder decision(ReviewDecision decision) {
      this.decision = decision;
      return this;
    }

    public Builder riskTier(RiskTier riskTier) {
      this.riskTier = riskTier;
      return this;
    }

    public Builder bulkApproval(boolean bulkApproval) {
      this.bulkApproval = bulkApproval;
      return this;
    }

    public Builder reviewerId(UUID reviewerId) {
      this.reviewerId = reviewerId;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public ReviewEventForCanonicalWrite build() {
      return new ReviewEventForCanonicalWrite(
          reviewEventId,
          organizationId,
          targetEntity,
          targetFieldPath,
          claimLedgerItemId,
          sourceSpanReference,
          decision,
          riskTier,
          bulkApproval,
          reviewerId,
          reason);
    }
  }
}
