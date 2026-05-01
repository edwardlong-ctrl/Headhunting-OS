package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AITaskExecutionRequest(
    UUID organizationId,
    String taskKey,
    String taskVersion,
    ActorRef requestedBy,
    EntityRef targetEntity,
    List<UUID> sourceReferenceIds,
    JsonNode inputPayload,
    WorkflowCorrelationId correlationId,
    WorkflowCausationId causationId,
    AITaskRunId replayedFromAiTaskRunId) {

  public AITaskExecutionRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    taskKey = requireNonBlank(taskKey, "taskKey");
    taskVersion = requireNonBlank(taskVersion, "taskVersion");
    Objects.requireNonNull(requestedBy, "requestedBy must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    sourceReferenceIds = List.copyOf(sourceReferenceIds == null ? List.of() : sourceReferenceIds);
    sourceReferenceIds.forEach(value -> Objects.requireNonNull(value, "sourceReferenceIds must not contain null values"));
    Objects.requireNonNull(inputPayload, "inputPayload must not be null");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
