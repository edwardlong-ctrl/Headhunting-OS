package com.recruitingtransactionos.coreapi.supportops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskReplayRequest;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskReplayService;
import java.util.Objects;

public final class AITaskReplaySupportAdapter implements AITaskSupportReplayPort {

  private final AITaskReplayService aiTaskReplayService;
  private final ObjectMapper objectMapper;

  public AITaskReplaySupportAdapter(AITaskReplayService aiTaskReplayService) {
    this(aiTaskReplayService, new ObjectMapper());
  }

  AITaskReplaySupportAdapter(
      AITaskReplayService aiTaskReplayService,
      ObjectMapper objectMapper) {
    this.aiTaskReplayService = Objects.requireNonNull(
        aiTaskReplayService, "aiTaskReplayService must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public AITaskSupportReplayOutcome replay(AITaskSupportReplayRequest command) {
    Objects.requireNonNull(command, "command must not be null");
    AITaskExecutionResult result = aiTaskReplayService.replay(new AITaskReplayRequest(
        command.organizationId(),
        command.aiTaskRunId(),
        command.supportActor()));
    return new AITaskSupportReplayOutcome(
        result.runRecord().aiTaskRunId(),
        canonicalFactsWritten(result),
        "ai_replay_created");
  }

  private boolean canonicalFactsWritten(AITaskExecutionResult result) {
    String metadataJson = result.runRecord().metadataJson();
    if (metadataJson == null || metadataJson.isBlank()) {
      return false;
    }
    try {
      JsonNode node = objectMapper.readTree(metadataJson);
      return node.path("canonicalPersistencePerformed").asBoolean(false);
    } catch (Exception exception) {
      return false;
    }
  }
}
