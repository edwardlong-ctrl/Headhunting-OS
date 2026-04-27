package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WorkflowEventAppendCommand(
    UUID organizationId,
    String entityNamespace,
    EntityRef entity,
    Integer entityVersion,
    String action,
    WorkflowStateSnapshot beforeState,
    WorkflowStateSnapshot afterState,
    ActorRef actor,
    String sourceType,
    UUID sourceRefId,
    AITaskRunId aiTaskRunId,
    ReviewEventId reviewEventId,
    String reason,
    WorkflowIdempotencyKey idempotencyKey,
    WorkflowCorrelationId correlationId,
    WorkflowCausationId causationId,
    Instant occurredAt) {

  public WorkflowEventAppendCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    entityNamespace = PortContractGuards.requireNonBlank(entityNamespace, "entityNamespace");
    Objects.requireNonNull(entity, "entity must not be null");
    action = PortContractGuards.requireNonBlank(action, "action");
    Objects.requireNonNull(beforeState, "beforeState must not be null");
    Objects.requireNonNull(afterState, "afterState must not be null");
    Objects.requireNonNull(actor, "actor must not be null");
    sourceType = PortContractGuards.requireNonBlank(sourceType, "sourceType");
    reason = PortContractGuards.requireNonBlank(reason, "reason");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
  }
}
