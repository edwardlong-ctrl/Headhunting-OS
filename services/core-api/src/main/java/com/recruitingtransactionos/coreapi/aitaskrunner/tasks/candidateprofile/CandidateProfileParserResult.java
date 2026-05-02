package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.shared.AITaskClaimCandidate;
import java.util.List;
import java.util.Objects;

public record CandidateProfileParserResult(
    AITaskExecutionResult execution,
    CandidateProfileParserOutput output,
    List<AITaskClaimCandidate> claimCandidates) {

  public CandidateProfileParserResult {
    Objects.requireNonNull(execution, "execution must not be null");
    Objects.requireNonNull(output, "output must not be null");
    claimCandidates = List.copyOf(Objects.requireNonNull(
        claimCandidates,
        "claimCandidates must not be null"));
  }
}
