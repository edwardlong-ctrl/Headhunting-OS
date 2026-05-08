package com.recruitingtransactionos.coreapi.observability;

public record ObservabilityWorkflowEventResponse(
    String workflowEventId,
    String entityType,
    String entityId,
    String actionCode,
    String actorType,
    String actorId,
    String aiInvolvement,
    String riskTier,
    String reason,
    String correlationId,
    String causationId,
    String occurredAt) {}
