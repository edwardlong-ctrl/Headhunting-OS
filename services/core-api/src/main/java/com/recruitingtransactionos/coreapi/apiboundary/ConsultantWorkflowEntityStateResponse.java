package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantWorkflowEntityStateResponse(
    String entityType,
    String entityId,
    String currentStatus,
    List<ConsultantWorkflowTransitionOptionResponse> transitionOptions) implements ApiSafeResponseBody {

  public ConsultantWorkflowEntityStateResponse {
    entityType = ApiBoundaryContractRules.requireNonBlank(entityType, "entityType");
    entityId = ApiBoundaryContractRules.requireNonBlank(entityId, "entityId");
    currentStatus = ApiBoundaryContractRules.sanitizeConsultantVisibleText(currentStatus, null);
    transitionOptions = List.copyOf(transitionOptions == null ? List.of() : transitionOptions);
  }
}
