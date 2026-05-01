package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public record AITaskProviderRequest(
    String taskKey,
    String promptVersion,
    String modelName,
    String systemPrompt,
    JsonNode inputPayload) {

  public AITaskProviderRequest {
    taskKey = requireNonBlank(taskKey, "taskKey");
    promptVersion = requireNonBlank(promptVersion, "promptVersion");
    modelName = requireNonBlank(modelName, "modelName");
    systemPrompt = requireNonBlank(systemPrompt, "systemPrompt");
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
