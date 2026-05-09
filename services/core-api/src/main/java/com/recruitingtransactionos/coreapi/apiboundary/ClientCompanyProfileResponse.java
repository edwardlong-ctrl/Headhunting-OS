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
    name = ApiBoundaryContractRules.requireBusinessVisibleText(name, "name");
    displayName = ApiBoundaryContractRules.sanitizeBusinessVisibleText(displayName, null);
    industry = ApiBoundaryContractRules.sanitizeBusinessVisibleText(industry, null);
    website = ApiBoundaryContractRules.sanitizeBusinessVisibleText(website, null);
    headquartersLocation = ApiBoundaryContractRules.sanitizeBusinessVisibleText(headquartersLocation, null);
    sizeBand = ApiBoundaryContractRules.sanitizeBusinessVisibleText(sizeBand, null);
    paymentReliability = ApiBoundaryContractRules.sanitizeBusinessVisibleText(paymentReliability, null);
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
  }
}
