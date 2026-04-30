package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record ShortlistUpdateRequest(
    String jobId,
    String title,
    String status,
    String ownerConsultantId,
    String metadata,
    int version) {

  public ShortlistUpdateRequest {
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }
}
