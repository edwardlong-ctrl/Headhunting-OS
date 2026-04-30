package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record JobRequirementCreateRequest(
    String requirementType,
    String label,
    String importance,
    String detail,
    int sortOrder,
    String metadata) {

  public JobRequirementCreateRequest {
    requirementType = ApiBoundaryContractRules.requireNonBlank(requirementType, "requirementType");
    label = ApiBoundaryContractRules.requireNonBlank(label, "label");
  }
}
