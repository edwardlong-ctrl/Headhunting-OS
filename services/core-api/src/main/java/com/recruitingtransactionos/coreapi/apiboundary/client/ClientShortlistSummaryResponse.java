package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;

public record ClientShortlistSummaryResponse(
    String shortlistId,
    String jobId,
    String title,
    String status,
    int candidateCount,
    String sentAt,
    String clientViewedAt,
    String createdAt) implements ApiSafeResponseBody {

  public ClientShortlistSummaryResponse {
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
