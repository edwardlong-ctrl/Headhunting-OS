package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CanonicalWriteAttemptAppendCommand(
    UUID organizationId,
    EntityRef targetEntity,
    Integer targetEntityVersion,
    String targetFieldPath,
    String proposedValueRef,
    String sourceSpanRef,
    ClaimId claimLedgerItemId,
    ReviewEventId reviewEventId,
    String decision,
    List<String> reasonCodes,
    ActorRef actor,
    AITaskRunId aiTaskRunId,
    WorkflowIdempotencyKey idempotencyKey,
    WorkflowCorrelationId correlationId,
    WorkflowCausationId causationId,
    WorkflowEventId workflowEventId,
    Instant occurredAt) {

  public CanonicalWriteAttemptAppendCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = PortContractGuards.requireNonBlank(targetFieldPath, "targetFieldPath");
    proposedValueRef = PortContractGuards.requireNonBlank(proposedValueRef, "proposedValueRef");
    sourceSpanRef = optionalNonBlank(sourceSpanRef, "sourceSpanRef");
    decision = PortContractGuards.requireNonBlank(decision, "decision");
    Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
    reasonCodes = List.copyOf(reasonCodes);
    Objects.requireNonNull(actor, "actor must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
  }

  public CanonicalWriteAttemptAppendCommand withWorkflowEventId(WorkflowEventId newWorkflowEventId) {
    return new CanonicalWriteAttemptAppendCommand(
        organizationId,
        targetEntity,
        targetEntityVersion,
        targetFieldPath,
        proposedValueRef,
        sourceSpanRef,
        claimLedgerItemId,
        reviewEventId,
        decision,
        reasonCodes,
        actor,
        aiTaskRunId,
        idempotencyKey,
        correlationId,
        causationId,
        newWorkflowEventId,
        occurredAt);
  }

  private static String optionalNonBlank(String value, String name) {
    if (value == null) {
      return null;
    }
    return PortContractGuards.requireNonBlank(value, name);
  }
}
