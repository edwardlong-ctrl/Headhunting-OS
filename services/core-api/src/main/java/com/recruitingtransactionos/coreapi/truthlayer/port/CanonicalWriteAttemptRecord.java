package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CanonicalWriteAttemptRecord(
    CanonicalWriteAttemptId attemptId,
    UUID organizationId,
    String entityType,
    UUID entityId,
    Integer entityVersion,
    String targetFieldPath,
    String proposedValueRef,
    String sourceSpanRef,
    ClaimId claimLedgerItemId,
    ReviewEventId reviewEventId,
    String decision,
    List<String> reasonCodes,
    UUID actorUserId,
    ActorRole actorRole,
    AITaskRunId aiTaskRunId,
    WorkflowIdempotencyKey idempotencyKey,
    WorkflowCorrelationId correlationId,
    WorkflowCausationId causationId,
    WorkflowEventId workflowEventId,
    Instant occurredAt,
    Instant createdAt) {

  public CanonicalWriteAttemptRecord {
    Objects.requireNonNull(attemptId, "attemptId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    entityType = PortContractGuards.requireNonBlank(entityType, "entityType");
    Objects.requireNonNull(entityId, "entityId must not be null");
    targetFieldPath = PortContractGuards.requireNonBlank(targetFieldPath, "targetFieldPath");
    proposedValueRef = PortContractGuards.requireNonBlank(proposedValueRef, "proposedValueRef");
    decision = PortContractGuards.requireNonBlank(decision, "decision");
    Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
    reasonCodes = List.copyOf(reasonCodes);
    Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}
