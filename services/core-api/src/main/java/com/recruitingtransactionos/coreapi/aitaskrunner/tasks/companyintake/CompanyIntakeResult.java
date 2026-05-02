package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import java.util.Objects;

public record CompanyIntakeResult(
    AITaskExecutionResult execution,
    CompanyIntakeOutput output) {

  public CompanyIntakeResult {
    Objects.requireNonNull(execution, "execution must not be null");
    Objects.requireNonNull(output, "output must not be null");
  }
}
