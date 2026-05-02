package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake;

import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.shared.AITaskClaimCandidate;
import java.util.List;
import java.util.Objects;

public record JobIntakeOutput(
    String roleSummary,
    List<AITaskClaimCandidate> claimCandidates,
    List<String> followUpQuestions) {

  public JobIntakeOutput {
    roleSummary = requireNonBlank(roleSummary, "roleSummary");
    claimCandidates = safeClaimCandidates(claimCandidates);
    followUpQuestions = safeStrings(followUpQuestions, "followUpQuestions");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static List<AITaskClaimCandidate> safeClaimCandidates(List<AITaskClaimCandidate> values) {
    Objects.requireNonNull(values, "claimCandidates must not be null");
    return List.copyOf(values);
  }

  private static List<String> safeStrings(List<String> values, String name) {
    Objects.requireNonNull(values, name + " must not be null");
    List<String> copy = List.copyOf(values);
    copy.forEach(value -> {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(name + " must not contain blank values");
      }
    });
    return copy;
  }
}
