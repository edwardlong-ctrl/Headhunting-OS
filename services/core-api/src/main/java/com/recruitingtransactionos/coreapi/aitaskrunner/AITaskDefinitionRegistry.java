package com.recruitingtransactionos.coreapi.aitaskrunner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

public final class AITaskDefinitionRegistry {

  private final Map<String, AITaskDefinition> definitions;

  public AITaskDefinitionRegistry(List<AITaskDefinition> definitions) {
    Objects.requireNonNull(definitions, "definitions must not be null");
    Map<String, AITaskDefinition> byKey = new LinkedHashMap<>();
    for (AITaskDefinition definition : definitions) {
      Objects.requireNonNull(definition, "definitions must not contain null values");
      String compositeKey = compositeKey(definition.taskKey(), definition.taskVersion());
      if (byKey.put(compositeKey, definition) != null) {
        throw new IllegalArgumentException("duplicate AI task definition: " + compositeKey);
      }
    }
    this.definitions = Collections.unmodifiableMap(byKey);
  }

  public AITaskDefinition findRequired(String taskKey, String taskVersion) {
    AITaskDefinition definition = definitions.get(compositeKey(taskKey, taskVersion));
    if (definition == null) {
      throw new IllegalArgumentException("unknown_ai_task_definition");
    }
    return definition;
  }

  public List<AITaskDefinition> definitions() {
    return List.copyOf(definitions.values());
  }

  private static String compositeKey(String taskKey, String taskVersion) {
    return taskKey.strip() + "::" + taskVersion.strip();
  }
}
