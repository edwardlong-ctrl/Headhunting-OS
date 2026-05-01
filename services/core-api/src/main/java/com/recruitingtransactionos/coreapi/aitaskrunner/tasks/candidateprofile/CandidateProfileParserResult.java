package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import java.util.Objects;

public record CandidateProfileParserResult(
    AITaskExecutionResult execution,
    CandidateProfileParserOutput output) {

  public CandidateProfileParserResult {
    Objects.requireNonNull(execution, "execution must not be null");
    Objects.requireNonNull(output, "output must not be null");
  }
}
