package com.recruitingtransactionos.coreapi.observability;

import java.time.Instant;
import java.util.UUID;

public record ObservabilityWorkflowEventQuery(
    UUID organizationId,
    UUID workflowEventId,
    String entityType,
    UUID entityId,
    String actionCode,
    String actorType,
    UUID actorId,
    UUID correlationId,
    UUID causationId,
    Instant occurredFrom,
    Instant occurredTo,
    int limit,
    int offset) {}
