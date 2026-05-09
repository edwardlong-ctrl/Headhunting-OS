package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantJobSummaryResponse(
    String jobId,
    String title,
    String companyId,
    String status,
    String industryPackKey,
    String industryPackLabel,
    String createdAt) implements ApiSafeResponseBody {

  public ConsultantJobSummaryResponse {
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    title = ApiBoundaryContractRules.requireBusinessVisibleText(title, "title");
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    industryPackKey = ApiBoundaryContractRules.sanitizeBusinessVisibleText(industryPackKey, null);
    industryPackLabel = ApiBoundaryContractRules.sanitizeBusinessVisibleText(industryPackLabel, null);
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
