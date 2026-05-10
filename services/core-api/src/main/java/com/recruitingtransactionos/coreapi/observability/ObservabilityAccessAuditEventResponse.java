package com.recruitingtransactionos.coreapi.observability;

public record ObservabilityAccessAuditEventResponse(
    String auditLogId,
    String actorUserId,
    String actorRole,
    String action,
    String targetEntityType,
    String targetEntityId,
    String result,
    String reason,
    String sensitivityLevel,
    String occurredAt) {}
