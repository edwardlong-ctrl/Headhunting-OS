package com.recruitingtransactionos.coreapi.apiboundary;

public record ClientCompanyProfileResponse(
    String companyId,
    long version,
    String name,
    String displayName,
    String industry,
    String website,
    String headquartersLocation,
    String sizeBand,
    String paymentReliability,
    String status,
    String updatedAt) implements ApiSafeResponseBody {

  public ClientCompanyProfileResponse {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    name = ApiBoundaryContractRules.requireApiSafeExternalText(name, "name");
    displayName = ApiBoundaryContractRules.sanitizeExternalText(displayName, null);
    industry = ApiBoundaryContractRules.sanitizeExternalText(industry, null);
    website = ApiBoundaryContractRules.sanitizeExternalText(website, null);
    headquartersLocation = ApiBoundaryContractRules.sanitizeExternalText(headquartersLocation, null);
    sizeBand = ApiBoundaryContractRules.sanitizeExternalText(sizeBand, null);
    paymentReliability = ApiBoundaryContractRules.sanitizeExternalText(paymentReliability, null);
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
  }
}
