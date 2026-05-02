package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantWorkflowTransitionOptionResponse(
    String actionCode,
    String currentStatus,
    String targetStatus,
    boolean allowed,
    List<ConsultantWorkflowBlockerResponse> blockers) {

  public ConsultantWorkflowTransitionOptionResponse {
    actionCode = ApiBoundaryContractRules.requireNonBlank(actionCode, "actionCode");
    currentStatus = ApiBoundaryContractRules.sanitizeConsultantVisibleText(currentStatus, null);
    targetStatus = ApiBoundaryContractRules.sanitizeConsultantVisibleText(targetStatus, null);
    blockers = List.copyOf(blockers == null ? List.of() : blockers);
  }
}
