package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantFollowUpSummaryResponse(
    String followUpType,
    String entityType,
    String entityId,
    String title,
    String status,
    String safeReason,
    String route,
    String occurredAt) implements ApiSafeResponseBody {

  public ConsultantFollowUpSummaryResponse {
    followUpType = ApiBoundaryContractRules.requireNonBlank(followUpType, "followUpType");
    entityType = ApiBoundaryContractRules.requireNonBlank(entityType, "entityType");
    entityId = ApiBoundaryContractRules.requireNonBlank(entityId, "entityId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    safeReason = ApiBoundaryContractRules.requireNonBlank(safeReason, "safeReason");
    route = ApiBoundaryContractRules.requireNonBlank(route, "route");
    occurredAt = ApiBoundaryContractRules.requireNonBlank(occurredAt, "occurredAt");
  }
}
