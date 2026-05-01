package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import java.util.Objects;

public record AuthenticityRiskAssessorResult(
    AITaskExecutionResult execution,
    AuthenticityRiskAssessorOutput output) {

  public AuthenticityRiskAssessorResult {
    Objects.requireNonNull(execution, "execution must not be null");
    Objects.requireNonNull(output, "output must not be null");
  }
}
