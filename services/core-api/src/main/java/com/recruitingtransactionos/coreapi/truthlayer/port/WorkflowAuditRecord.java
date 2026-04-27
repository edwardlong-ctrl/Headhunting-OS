package com.recruitingtransactionos.coreapi.truthlayer.port;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WorkflowAuditRecord(
    WorkflowEventId workflowEventId,
    UUID organizationId,
    String entityNamespace,
    String entityType,
    UUID entityId,
    String actionCode,
    ActorRole actorType,
    UUID actorId,
    WorkflowAiInvolvement aiInvolvement,
    RiskTier riskTier,
    WorkflowStateSnapshot beforeState,
    WorkflowStateSnapshot afterState,
    String reason,
    WorkflowIdempotencyKey idempotencyKey,
    WorkflowCorrelationId correlationId,
    WorkflowCausationId causationId,
    WorkflowEventId previousEventId,
    String sourceType,
    UUID sourceRefId,
    Instant occurredAt,
    Instant createdAt) {

  public WorkflowAuditRecord {
    Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    entityNamespace = PortContractGuards.requireNonBlank(entityNamespace, "entityNamespace");
    entityType = PortContractGuards.requireNonBlank(entityType, "entityType");
    Objects.requireNonNull(entityId, "entityId must not be null");
    actionCode = PortContractGuards.requireNonBlank(actionCode, "actionCode");
    Objects.requireNonNull(actorType, "actorType must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(aiInvolvement, "aiInvolvement must not be null");
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    Objects.requireNonNull(beforeState, "beforeState must not be null");
    Objects.requireNonNull(afterState, "afterState must not be null");
    reason = PortContractGuards.requireNonBlank(reason, "reason");
    sourceType = PortContractGuards.requireNonBlank(sourceType, "sourceType");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}
