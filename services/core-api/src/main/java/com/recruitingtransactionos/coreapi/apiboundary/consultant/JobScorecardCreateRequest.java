package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record JobScorecardCreateRequest(
    String dimensions,
    String scoringGuidance,
    String status,
    String metadata) {

  public JobScorecardCreateRequest {
    dimensions = ApiBoundaryContractRules.requireNonBlank(dimensions, "dimensions");
  }
}
