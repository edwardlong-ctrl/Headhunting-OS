package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecisionType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.util.Objects;
import java.util.UUID;

public record IntakeCanonicalWriteBridgeResult(
    UUID organizationId,
    ClaimId claimLedgerItemId,
    ReviewEventId reviewEventId,
    WorkflowEventId workflowEventId,
    CanonicalWriteDecisionType gateDecision,
    boolean canonicalPersistencePerformed,
    String canonicalPersistenceStatus,
    IntakeCanonicalWriteBridgeStatus status,
    String blockedReason,
    String summary) {

  public IntakeCanonicalWriteBridgeResult {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(claimLedgerItemId, "claimLedgerItemId must not be null");
    Objects.requireNonNull(reviewEventId, "reviewEventId must not be null");
    if (status != IntakeCanonicalWriteBridgeStatus.FAILED) {
      Objects.requireNonNull(gateDecision, "gateDecision must not be null");
    }
    canonicalPersistenceStatus = GovernedIntakeGuards.requireNonBlank(
        canonicalPersistenceStatus,
        "canonicalPersistenceStatus");
    Objects.requireNonNull(status, "status must not be null");
    blockedReason = GovernedIntakeGuards.optionalNonBlank(blockedReason, "blockedReason");
    summary = GovernedIntakeGuards.requireNonBlank(summary, "summary");
  }
}
