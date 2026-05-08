package com.recruitingtransactionos.coreapi.observability;

import java.math.BigDecimal;

public record ObservabilityAITaskRunResponse(
    String aiTaskRunId,
    String taskName,
    String taskVersion,
    String inputSchemaVersion,
    String outputSchemaVersion,
    String promptVersion,
    String status,
    String humanReviewStatus,
    String writeBackTarget,
    String modelProvider,
    String modelName,
    BigDecimal costUnits,
    Long latencyMs,
    String traceRef,
    String errorCode,
    String replayedFromAiTaskRunId,
    String requestedByRole,
    String requestedByUserId,
    String targetEntityType,
    String targetEntityId,
    String correlationId,
    String causationId,
    String startedAt,
    String completedAt) {}
