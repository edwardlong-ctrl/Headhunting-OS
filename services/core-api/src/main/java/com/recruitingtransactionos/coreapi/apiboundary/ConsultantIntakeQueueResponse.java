package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantIntakeQueueResponse(
    List<ConsultantIntakeQueueItemResponse> items) implements ApiSafeResponseBody {

  public ConsultantIntakeQueueResponse {
    items = ApiBoundaryContractRules.requireNonNullList(items, "items");
  }
}
