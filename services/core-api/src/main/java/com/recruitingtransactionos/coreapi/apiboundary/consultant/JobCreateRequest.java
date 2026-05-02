package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record JobCreateRequest(
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
    String industryPackKey) {

  public JobCreateRequest {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
  }

  public JobCreateRequest(
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
      String metadata) {
    this(
        companyId,
        title,
        description,
        location,
        seniorityBand,
        roleFamily,
        employmentType,
        compensation,
        status,
        commercialTerms,
        ownerConsultantId,
        metadata,
        null);
  }
}
