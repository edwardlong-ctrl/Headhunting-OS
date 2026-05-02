package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record ConsultantJobActivationGateResponse(
    String jobId,
    boolean activationAllowed,
    List<String> clarificationQuestions,
    List<String> blockerReasons,
    boolean hasScorecard,
    boolean hasRequirements,
    boolean hasCommercialTermsPlaceholder) implements ApiSafeResponseBody {

  public ConsultantJobActivationGateResponse {
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    clarificationQuestions = List.copyOf(Objects.requireNonNull(
        clarificationQuestions, "clarificationQuestions must not be null"));
    blockerReasons = List.copyOf(Objects.requireNonNull(
        blockerReasons, "blockerReasons must not be null"));
  }
}
