package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantBlockedActionResponse(
    String entityType,
    String entityId,
    String title,
    String reasonCode,
    String safeReason,
    String severity,
    String route) implements ApiSafeResponseBody {

  public ConsultantBlockedActionResponse {
    entityType = ApiBoundaryContractRules.requireNonBlank(entityType, "entityType");
    entityId = ApiBoundaryContractRules.requireNonBlank(entityId, "entityId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    reasonCode = ApiBoundaryContractRules.requireNonBlank(reasonCode, "reasonCode");
    safeReason = ApiBoundaryContractRules.requireNonBlank(safeReason, "safeReason");
    severity = ApiBoundaryContractRules.requireNonBlank(severity, "severity");
    route = ApiBoundaryContractRules.requireNonBlank(route, "route");
  }
}
