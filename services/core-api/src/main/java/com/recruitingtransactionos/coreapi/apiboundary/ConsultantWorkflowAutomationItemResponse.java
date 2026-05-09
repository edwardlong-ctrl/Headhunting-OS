package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantWorkflowAutomationItemResponse(
    String workflowEventId,
    String entityType,
    String entityId,
    String actionCode,
    String workflowFamily,
    String ownerRole,
    String occurredAt,
    String dueAt,
    String reminderAt,
    String escalationAt,
    String status,
    String blockerCode,
    String nextBestAction) implements ApiSafeResponseBody {

  public ConsultantWorkflowAutomationItemResponse {
    workflowEventId = ApiBoundaryContractRules.requireNonBlank(workflowEventId, "workflowEventId");
    entityType = ApiBoundaryContractRules.requireNonBlank(entityType, "entityType");
    entityId = ApiBoundaryContractRules.requireNonBlank(entityId, "entityId");
    actionCode = ApiBoundaryContractRules.requireNonBlank(actionCode, "actionCode");
    workflowFamily = ApiBoundaryContractRules.requireNonBlank(workflowFamily, "workflowFamily");
    ownerRole = ApiBoundaryContractRules.requireNonBlank(ownerRole, "ownerRole");
    occurredAt = ApiBoundaryContractRules.requireNonBlank(occurredAt, "occurredAt");
    dueAt = ApiBoundaryContractRules.requireNonBlank(dueAt, "dueAt");
    reminderAt = ApiBoundaryContractRules.requireNonBlank(reminderAt, "reminderAt");
    escalationAt = ApiBoundaryContractRules.requireNonBlank(escalationAt, "escalationAt");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    blockerCode = ApiBoundaryContractRules.requireNonBlank(blockerCode, "blockerCode");
    nextBestAction = ApiBoundaryContractRules.sanitizeConsultantVisibleText(
        ApiBoundaryContractRules.requireNonBlank(nextBestAction, "nextBestAction"),
        "");
  }
}
