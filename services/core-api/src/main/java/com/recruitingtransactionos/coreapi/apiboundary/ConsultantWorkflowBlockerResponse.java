package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantWorkflowBlockerResponse(
    String code,
    String safeReason) {

  public ConsultantWorkflowBlockerResponse {
    code = ApiBoundaryContractRules.requireNonBlank(
        ApiBoundaryContractRules.sanitizeApiSafeReasonCode(code, "workflow_blocked"),
        "code");
    safeReason = ApiBoundaryContractRules.sanitizeConsultantVisibleText(safeReason, null);
  }
}
