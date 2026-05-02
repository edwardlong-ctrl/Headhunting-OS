package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record ClientJobSubmissionStatusResponse(
    String jobId,
    String companyId,
    String title,
    String status,
    String createdAt,
    String updatedAt,
    List<String> clarificationQuestions,
    List<String> clarificationAnswers,
    List<String> blockerReasons,
    boolean activationAllowed) implements ApiSafeResponseBody {

  public ClientJobSubmissionStatusResponse {
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    title = ApiBoundaryContractRules.requireApiSafeExternalText(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    clarificationQuestions = List.copyOf(Objects.requireNonNull(
        clarificationQuestions, "clarificationQuestions must not be null"));
    clarificationAnswers = List.copyOf(Objects.requireNonNull(
        clarificationAnswers, "clarificationAnswers must not be null"));
    blockerReasons = List.copyOf(Objects.requireNonNull(
        blockerReasons, "blockerReasons must not be null"));
  }
}
