package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AITaskRunUpdateCommand(
    UUID organizationId,
    AITaskRunId aiTaskRunId,
    AITaskRunStatus status,
    Instant completedAt,
    String failureReason,
    String outputPayloadJson,
    String toolCallsJson,
    BigDecimal costUnits,
    String traceRef,
    String errorCode,
    String metadataJson) {

  public AITaskRunUpdateCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(aiTaskRunId, "aiTaskRunId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    if (status == AITaskRunStatus.CREATED) {
      throw new IllegalArgumentException("update status must not be CREATED");
    }
    outputPayloadJson =
        PortContractGuards.normalizedJsonValue(outputPayloadJson, "outputPayloadJson", true);
    toolCallsJson = PortContractGuards.normalizedJsonArray(toolCallsJson, "toolCallsJson", true);
    costUnits = PortContractGuards.requireNonNegative(costUnits, "costUnits");
    traceRef = PortContractGuards.safeTraceRef(traceRef);
    errorCode = PortContractGuards.safeReasonCode(errorCode, "errorCode");
    metadataJson = PortContractGuards.normalizedJsonObject(metadataJson, "metadataJson", true);
    failureReason = PortContractGuards.safeFailureReason(failureReason, status);
  }
}
