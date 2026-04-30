package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record CompanyContactCreateRequest(
    String name,
    String title,
    String email,
    String phone,
    String roleType,
    boolean isPrimary,
    String status,
    String metadata) {

  public CompanyContactCreateRequest {
    name = ApiBoundaryContractRules.requireNonBlank(name, "name");
  }
}
