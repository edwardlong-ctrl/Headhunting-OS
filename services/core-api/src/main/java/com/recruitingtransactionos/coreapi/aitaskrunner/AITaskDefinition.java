package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import java.util.Objects;

public record AITaskDefinition(
    String registryTaskId,
    String taskKey,
    String displayName,
    String registryGroup,
    String taskVersion,
    String promptVersion,
    String promptResourcePath,
    String inputSchemaResourcePath,
    String outputSchemaResourcePath,
    String evalSuiteResourcePath,
    AITaskWriteBackTarget writeBackTarget,
    AITaskHumanReviewStatus humanReviewStatus) {

  public AITaskDefinition(
      String taskKey,
      String taskVersion,
      String promptVersion,
      String promptResourcePath,
      String inputSchemaResourcePath,
      String outputSchemaResourcePath,
      AITaskWriteBackTarget writeBackTarget,
      AITaskHumanReviewStatus humanReviewStatus) {
    this(
        taskKey,
        taskKey,
        taskKey,
        "legacy",
        taskVersion,
        promptVersion,
        promptResourcePath,
        inputSchemaResourcePath,
        outputSchemaResourcePath,
        "/ai/evals/" + taskKey + "-eval-cases.json",
        writeBackTarget,
        humanReviewStatus);
  }

  public AITaskDefinition {
    registryTaskId = requireNonBlank(registryTaskId, "registryTaskId");
    taskKey = requireNonBlank(taskKey, "taskKey");
    displayName = requireNonBlank(displayName, "displayName");
    registryGroup = requireNonBlank(registryGroup, "registryGroup");
    taskVersion = requireNonBlank(taskVersion, "taskVersion");
    promptVersion = requireNonBlank(promptVersion, "promptVersion");
    promptResourcePath = requireNonBlank(promptResourcePath, "promptResourcePath");
    inputSchemaResourcePath = requireNonBlank(inputSchemaResourcePath, "inputSchemaResourcePath");
    outputSchemaResourcePath = requireNonBlank(outputSchemaResourcePath, "outputSchemaResourcePath");
    evalSuiteResourcePath = requireNonBlank(evalSuiteResourcePath, "evalSuiteResourcePath");
    Objects.requireNonNull(writeBackTarget, "writeBackTarget must not be null");
    Objects.requireNonNull(humanReviewStatus, "humanReviewStatus must not be null");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
