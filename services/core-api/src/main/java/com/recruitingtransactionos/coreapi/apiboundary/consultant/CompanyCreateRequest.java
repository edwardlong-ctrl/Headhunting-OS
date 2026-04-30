package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record CompanyCreateRequest(
    String name,
    String displayName,
    String industry,
    String website,
    String headquartersLocation,
    String sizeBand,
    String status,
    String paymentReliability,
    String ownerConsultantId,
    String metadata) {

  public CompanyCreateRequest {
    name = ApiBoundaryContractRules.requireNonBlank(name, "name");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    if (displayName != null && displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank if provided");
    }
  }
}
