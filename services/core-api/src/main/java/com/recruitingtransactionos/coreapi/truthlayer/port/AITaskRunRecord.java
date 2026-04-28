package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AITaskRunRecord(
    AITaskRunId aiTaskRunId,
    UUID organizationId,
    String taskName,
    String taskVersion,
    String inputSchemaVersion,
    String outputSchemaVersion,
    String promptVersion,
    ModelRef model,
    AITaskRunStatus status,
    String humanReviewStatus,
    WriteBackTarget writeBackTarget,
    ActorRef requestedBy,
    WorkflowCorrelationId correlationId,
    WorkflowCausationId causationId,
    EntityRef targetEntity,
    List<UUID> sourceReferenceIds,
    Instant startedAt,
    Instant completedAt,
    String failureReason,
    Instant createdAt) {

  public AITaskRunRecord {
    Objects.requireNonNull(aiTaskRunId, "aiTaskRunId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    taskName = PortContractGuards.requireNonBlank(taskName, "taskName");
    taskVersion = PortContractGuards.requireNonBlank(taskVersion, "taskVersion");
    inputSchemaVersion = PortContractGuards.requireNonBlank(inputSchemaVersion,
        "inputSchemaVersion");
    outputSchemaVersion = PortContractGuards.requireNonBlank(outputSchemaVersion,
        "outputSchemaVersion");
    promptVersion = PortContractGuards.requireNonBlank(promptVersion, "promptVersion");
    Objects.requireNonNull(model, "model must not be null");
    Objects.requireNonNull(status, "status must not be null");
    if (humanReviewStatus != null) {
      humanReviewStatus = PortContractGuards.requireNonBlank(humanReviewStatus,
          "humanReviewStatus");
    }
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    sourceReferenceIds = PortContractGuards.copyUuidList(sourceReferenceIds, "sourceReferenceIds");
    Objects.requireNonNull(startedAt, "startedAt must not be null");
    if (completedAt != null && completedAt.isBefore(startedAt)) {
      throw new IllegalArgumentException("completedAt must not be before startedAt");
    }
    failureReason = PortContractGuards.safeFailureReason(failureReason, status);
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}
