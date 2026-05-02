package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import java.util.Objects;

public record JobIntakeResult(
    AITaskExecutionResult execution,
    JobIntakeOutput output) {

  public JobIntakeResult {
    Objects.requireNonNull(execution, "execution must not be null");
    Objects.requireNonNull(output, "output must not be null");
  }
}
