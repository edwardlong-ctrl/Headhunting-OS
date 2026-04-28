package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record IntakeCanonicalWriteBridgeRequest(
    UUID organizationId,
    ClaimId claimLedgerItemId,
    ReviewEventId reviewEventId,
    ActorRole requestedByActorType,
    UUID requestedByActorId,
    CandidateProfileId candidateProfileId,
    String targetEntityType,
    UUID targetEntityId,
    String targetFieldPath,
    String requestedCanonicalValue,
    VerificationStatus targetVerificationStatus,
    RiskTier riskTier,
    boolean clientVisible,
    boolean conflictsWithCanonical,
    boolean transactionLegalApproval,
    String reason,
    UUID correlationId,
    UUID causationId,
    Instant occurredAt,
    IntakeCanonicalWriteBridgePolicy bridgePolicy) {

  public IntakeCanonicalWriteBridgeRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(claimLedgerItemId, "claimLedgerItemId must not be null");
    Objects.requireNonNull(reviewEventId, "reviewEventId must not be null");
    Objects.requireNonNull(requestedByActorType, "requestedByActorType must not be null");
    Objects.requireNonNull(requestedByActorId, "requestedByActorId must not be null");
    targetEntityType = GovernedIntakeGuards.requireNonBlank(
        targetEntityType,
        "targetEntityType");
    Objects.requireNonNull(targetEntityId, "targetEntityId must not be null");
    targetFieldPath = GovernedIntakeGuards.requireNonBlank(targetFieldPath, "targetFieldPath");
    requestedCanonicalValue = GovernedIntakeGuards.optionalNonBlank(
        requestedCanonicalValue,
        "requestedCanonicalValue");
    Objects.requireNonNull(targetVerificationStatus,
        "targetVerificationStatus must not be null");
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    reason = GovernedIntakeGuards.requireNonBlank(reason, "reason");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(bridgePolicy, "bridgePolicy must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID organizationId;
    private ClaimId claimLedgerItemId;
    private ReviewEventId reviewEventId;
    private ActorRole requestedByActorType;
    private UUID requestedByActorId;
    private CandidateProfileId candidateProfileId;
    private String targetEntityType;
    private UUID targetEntityId;
    private String targetFieldPath;
    private String requestedCanonicalValue;
    private VerificationStatus targetVerificationStatus;
    private RiskTier riskTier;
    private boolean clientVisible;
    private boolean conflictsWithCanonical;
    private boolean transactionLegalApproval;
    private String reason;
    private UUID correlationId;
    private UUID causationId;
    private Instant occurredAt;
    private IntakeCanonicalWriteBridgePolicy bridgePolicy;

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

    public Builder reviewEventId(ReviewEventId reviewEventId) {
      this.reviewEventId = reviewEventId;
      return this;
    }

    public Builder requestedByActorType(ActorRole requestedByActorType) {
      this.requestedByActorType = requestedByActorType;
      return this;
    }

    public Builder requestedByActorId(UUID requestedByActorId) {
      this.requestedByActorId = requestedByActorId;
      return this;
    }

    public Builder candidateProfileId(CandidateProfileId candidateProfileId) {
      this.candidateProfileId = candidateProfileId;
      return this;
    }

    public Builder targetEntityType(String targetEntityType) {
      this.targetEntityType = targetEntityType;
      return this;
    }

    public Builder targetEntityId(UUID targetEntityId) {
      this.targetEntityId = targetEntityId;
      return this;
    }

    public Builder targetFieldPath(String targetFieldPath) {
      this.targetFieldPath = targetFieldPath;
      return this;
    }

    public Builder requestedCanonicalValue(String requestedCanonicalValue) {
      this.requestedCanonicalValue = requestedCanonicalValue;
      return this;
    }

    public Builder targetVerificationStatus(VerificationStatus targetVerificationStatus) {
      this.targetVerificationStatus = targetVerificationStatus;
      return this;
    }

    public Builder riskTier(RiskTier riskTier) {
      this.riskTier = riskTier;
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

    public Builder transactionLegalApproval(boolean transactionLegalApproval) {
      this.transactionLegalApproval = transactionLegalApproval;
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

    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public Builder bridgePolicy(IntakeCanonicalWriteBridgePolicy bridgePolicy) {
      this.bridgePolicy = bridgePolicy;
      return this;
    }

    public IntakeCanonicalWriteBridgeRequest build() {
      return new IntakeCanonicalWriteBridgeRequest(
          organizationId,
          claimLedgerItemId,
          reviewEventId,
          requestedByActorType,
          requestedByActorId,
          candidateProfileId,
          targetEntityType,
          targetEntityId,
          targetFieldPath,
          requestedCanonicalValue,
          targetVerificationStatus,
          riskTier,
          clientVisible,
          conflictsWithCanonical,
          transactionLegalApproval,
          reason,
          correlationId,
          causationId,
          occurredAt,
          bridgePolicy);
    }
  }
}
