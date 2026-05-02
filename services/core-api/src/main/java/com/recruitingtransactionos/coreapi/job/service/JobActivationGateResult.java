package com.recruitingtransactionos.coreapi.job.service;

import java.util.List;
import java.util.Objects;

public record JobActivationGateResult(
    boolean activationAllowed,
    List<String> clarificationQuestions,
    List<String> blockerReasons,
    boolean hasScorecard,
    boolean hasRequirements,
    boolean hasCommercialTermsPlaceholder) {

  public JobActivationGateResult {
    clarificationQuestions = List.copyOf(
        Objects.requireNonNull(clarificationQuestions, "clarificationQuestions must not be null"));
    blockerReasons = List.copyOf(
        Objects.requireNonNull(blockerReasons, "blockerReasons must not be null"));
  }
}
