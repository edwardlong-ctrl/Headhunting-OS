package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantWorkflowAutomationQueueResponse(
    List<ConsultantWorkflowAutomationItemResponse> items,
    String generatedAt) implements ApiSafeResponseBody {

  public ConsultantWorkflowAutomationQueueResponse {
    items = List.copyOf(items == null ? List.of() : items);
    generatedAt = ApiBoundaryContractRules.requireNonBlank(generatedAt, "generatedAt");
  }
}
