package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantAuditDrawerResponse(
    String entityType,
    String entityId,
    List<ConsultantWorkflowEventResponse> items) implements ApiSafeResponseBody {

  public ConsultantAuditDrawerResponse {
    entityType = ApiBoundaryContractRules.requireNonBlank(entityType, "entityType");
    entityId = ApiBoundaryContractRules.requireNonBlank(entityId, "entityId");
    items = List.copyOf(items == null ? List.of() : items);
  }
}
