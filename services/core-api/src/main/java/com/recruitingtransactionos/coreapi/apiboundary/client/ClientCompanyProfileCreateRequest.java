package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record ClientCompanyProfileCreateRequest(
    String companyId,
    String name,
    String displayName,
    String industry,
    String website,
    String headquartersLocation,
    String sizeBand,
    String paymentReliability) {

  public ClientCompanyProfileCreateRequest {
    name = ApiBoundaryContractRules.requireNonBlank(name, "name");
  }
}
