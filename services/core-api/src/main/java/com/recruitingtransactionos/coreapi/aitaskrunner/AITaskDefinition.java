package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import java.util.Objects;

public record AITaskDefinition(
    String taskKey,
    String taskVersion,
    String promptVersion,
    String promptResourcePath,
    String inputSchemaResourcePath,
    String outputSchemaResourcePath,
    AITaskWriteBackTarget writeBackTarget,
    AITaskHumanReviewStatus humanReviewStatus) {

  public AITaskDefinition {
    taskKey = requireNonBlank(taskKey, "taskKey");
    taskVersion = requireNonBlank(taskVersion, "taskVersion");
    promptVersion = requireNonBlank(promptVersion, "promptVersion");
    promptResourcePath = requireNonBlank(promptResourcePath, "promptResourcePath");
    inputSchemaResourcePath = requireNonBlank(inputSchemaResourcePath, "inputSchemaResourcePath");
    outputSchemaResourcePath = requireNonBlank(outputSchemaResourcePath, "outputSchemaResourcePath");
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
