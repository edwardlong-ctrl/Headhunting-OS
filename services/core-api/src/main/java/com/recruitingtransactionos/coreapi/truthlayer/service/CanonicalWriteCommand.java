package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.ClaimInput;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CanonicalWriteCommand(
    UUID organizationId,
    EntityRef targetEntity,
    Integer targetEntityVersion,
    String targetFieldPath,
    String proposedValueRef,
    ClaimId claimId,
    ClaimInput claim,
    CanonicalWriteReviewEvidence reviewEvidence,
    VerificationStatus targetVerificationStatus,
    RiskTier targetRiskTier,
    boolean clientVisible,
    boolean conflictsWithCanonical,
    ActorRef actor,
    AITaskRunId aiTaskRunId,
    String reason,
    String idempotencyKey,
    UUID correlationId,
    Instant occurredAt) {

  public CanonicalWriteCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = requireNonBlank(targetFieldPath, "targetFieldPath");
    proposedValueRef = requireNonBlank(proposedValueRef, "proposedValueRef");
    Objects.requireNonNull(claimId, "claimId must not be null");
    Objects.requireNonNull(claim, "claim must not be null");
    Objects.requireNonNull(reviewEvidence, "reviewEvidence must not be null");
    Objects.requireNonNull(targetVerificationStatus,
        "targetVerificationStatus must not be null");
    Objects.requireNonNull(targetRiskTier, "targetRiskTier must not be null");
    Objects.requireNonNull(actor, "actor must not be null");
    reason = requireNonBlank(reason, "reason");
    if (idempotencyKey != null) {
      idempotencyKey = requireNonBlank(idempotencyKey, "idempotencyKey");
    }
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  public static final class Builder {
    private UUID organizationId;
    private EntityRef targetEntity;
    private Integer targetEntityVersion;
    private String targetFieldPath;
    private String proposedValueRef;
    private ClaimId claimId;
    private ClaimInput claim;
    private CanonicalWriteReviewEvidence reviewEvidence;
    private VerificationStatus targetVerificationStatus;
    private RiskTier targetRiskTier;
    private boolean clientVisible;
    private boolean conflictsWithCanonical;
    private ActorRef actor;
    private AITaskRunId aiTaskRunId;
    private String reason;
    private String idempotencyKey;
    private UUID correlationId;
    private Instant occurredAt;

    private Builder() {
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder targetEntity(EntityRef targetEntity) {
      this.targetEntity = targetEntity;
      return this;
    }

    public Builder targetEntityVersion(Integer targetEntityVersion) {
      this.targetEntityVersion = targetEntityVersion;
      return this;
    }

    public Builder targetFieldPath(String targetFieldPath) {
      this.targetFieldPath = targetFieldPath;
      return this;
    }

    public Builder proposedValueRef(String proposedValueRef) {
      this.proposedValueRef = proposedValueRef;
      return this;
    }

    public Builder claimId(ClaimId claimId) {
      this.claimId = claimId;
      return this;
    }

    public Builder claim(ClaimInput claim) {
      this.claim = claim;
      return this;
    }

    public Builder reviewEvidence(CanonicalWriteReviewEvidence reviewEvidence) {
      this.reviewEvidence = reviewEvidence;
      return this;
    }

    public Builder targetVerificationStatus(VerificationStatus targetVerificationStatus) {
      this.targetVerificationStatus = targetVerificationStatus;
      return this;
    }

    public Builder targetRiskTier(RiskTier targetRiskTier) {
      this.targetRiskTier = targetRiskTier;
      return this;
    }

    public Builder clientVisible(boolean clientVisible) {
      this.clientVisible = clientVisible;
      return this;
    }

    public Builder conflictsWithCanonical(boolean conflictsWithCanonical) {
      this.conflictsWithCanonical = conflictsWithCanonical;
      return this;
    }

    public Builder actor(ActorRef actor) {
      this.actor = actor;
      return this;
    }

    public Builder aiTaskRunId(AITaskRunId aiTaskRunId) {
      this.aiTaskRunId = aiTaskRunId;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder idempotencyKey(String idempotencyKey) {
      this.idempotencyKey = idempotencyKey;
      return this;
    }

    public Builder correlationId(UUID correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public CanonicalWriteCommand build() {
      return new CanonicalWriteCommand(
          organizationId,
          targetEntity,
          targetEntityVersion,
          targetFieldPath,
          proposedValueRef,
          claimId,
          claim,
          reviewEvidence,
          targetVerificationStatus,
          targetRiskTier,
          clientVisible,
          conflictsWithCanonical,
          actor,
          aiTaskRunId,
          reason,
          idempotencyKey,
          correlationId,
          occurredAt);
    }
  }
}
