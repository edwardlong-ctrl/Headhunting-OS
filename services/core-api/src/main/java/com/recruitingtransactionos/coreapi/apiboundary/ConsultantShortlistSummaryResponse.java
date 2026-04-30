package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantShortlistSummaryResponse(
    String shortlistId,
    String title,
    String jobId,
    String status,
    int candidateCount,
    String createdAt) implements ApiSafeResponseBody {

  public ConsultantShortlistSummaryResponse {
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    title = ApiBoundaryContractRules.requireApiSafeExternalText(title, "title");
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    if (candidateCount < 0) {
      throw new IllegalArgumentException("candidateCount must be >= 0");
    }
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
