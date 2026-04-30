package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantCompanySummaryResponse(
    String companyId,
    String name,
    String status,
    int contactCount,
    int jobCount,
    String createdAt) implements ApiSafeResponseBody {

  public ConsultantCompanySummaryResponse {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    name = ApiBoundaryContractRules.requireApiSafeExternalText(name, "name");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    if (contactCount < 0) {
      throw new IllegalArgumentException("contactCount must be >= 0");
    }
    if (jobCount < 0) {
      throw new IllegalArgumentException("jobCount must be >= 0");
    }
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
