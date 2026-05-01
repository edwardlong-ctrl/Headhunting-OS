package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import java.util.Objects;

public final class AITaskReplayService {

  private final AITaskRunService aiTaskRunService;
  private final AITaskRunnerService aiTaskRunnerService;
  private final ObjectMapper objectMapper;

  public AITaskReplayService(
      AITaskRunService aiTaskRunService,
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper) {
    this.aiTaskRunService = Objects.requireNonNull(aiTaskRunService, "aiTaskRunService must not be null");
    this.aiTaskRunnerService = Objects.requireNonNull(aiTaskRunnerService, "aiTaskRunnerService must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  public AITaskExecutionResult replay(AITaskReplayRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    AITaskRunRecord original = aiTaskRunService.findById(
            request.organizationId(),
            new AITaskRunId(request.aiTaskRunId()))
        .orElseThrow(() -> new IllegalArgumentException("ai_task_run_not_found"));
    return aiTaskRunnerService.execute(new AITaskExecutionRequest(
        request.organizationId(),
        original.taskName(),
        original.taskVersion(),
        request.replayRequestedBy(),
        original.targetEntity(),
        original.sourceReferenceIds(),
        parseInputPayload(original.inputPayloadJson()),
        original.correlationId(),
        original.causationId(),
        original.aiTaskRunId()));
  }

  private com.fasterxml.jackson.databind.JsonNode parseInputPayload(String inputPayloadJson) {
    try {
      return objectMapper.readTree(inputPayloadJson);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Stored AI task input payload is not valid JSON", exception);
    }
  }
}
