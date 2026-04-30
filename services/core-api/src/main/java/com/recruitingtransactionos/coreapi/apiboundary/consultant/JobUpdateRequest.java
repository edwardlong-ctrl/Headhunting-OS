package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record JobUpdateRequest(
    String companyId,
    String title,
    String description,
    String location,
    String seniorityBand,
    String roleFamily,
    String employmentType,
    String compensation,
    String status,
    String commercialTerms,
    String ownerConsultantId,
    String metadata,
    int version) {

  public JobUpdateRequest {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }
}
