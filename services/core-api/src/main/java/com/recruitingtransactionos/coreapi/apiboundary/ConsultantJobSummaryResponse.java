package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantJobSummaryResponse(
    String jobId,
    String title,
    String companyId,
    String status,
    String createdAt) implements ApiSafeResponseBody {

  public ConsultantJobSummaryResponse {
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    title = ApiBoundaryContractRules.requireApiSafeExternalText(title, "title");
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
