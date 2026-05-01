package com.recruitingtransactionos.coreapi.aitaskrunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class AITaskPromptRegistry {

  public String loadPrompt(AITaskDefinition definition) {
    Objects.requireNonNull(definition, "definition must not be null");
    try (InputStream stream = getClass().getResourceAsStream(definition.promptResourcePath())) {
      if (stream == null) {
        throw new IllegalArgumentException("missing_ai_task_prompt");
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read AI task prompt", exception);
    }
  }
}
