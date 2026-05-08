package com.recruitingtransactionos.coreapi.observability;

import java.time.Instant;
import java.util.UUID;

public record ObservabilityAITaskRunQuery(
    UUID organizationId,
    String taskName,
    String status,
    String targetEntityType,
    UUID targetEntityId,
    UUID correlationId,
    UUID causationId,
    Instant startedFrom,
    Instant startedTo,
    int limit,
    int offset) {}
