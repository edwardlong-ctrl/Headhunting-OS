package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantWorkflowEventResponse(
    String workflowEventId,
    String entityType,
    String entityId,
    String actionCode,
    String actorType,
    String aiInvolvement,
    String riskTier,
    String beforeStatus,
    String afterStatus,
    String beforeCardStatus,
    String afterCardStatus,
    String reason,
    String occurredAt) implements ApiSafeResponseBody {

  public ConsultantWorkflowEventResponse {
    workflowEventId = ApiBoundaryContractRules.requireNonBlank(workflowEventId, "workflowEventId");
    entityType = ApiBoundaryContractRules.requireNonBlank(entityType, "entityType");
    entityId = ApiBoundaryContractRules.requireNonBlank(entityId, "entityId");
    actionCode = ApiBoundaryContractRules.requireNonBlank(actionCode, "actionCode");
    actorType = ApiBoundaryContractRules.requireNonBlank(actorType, "actorType");
    aiInvolvement = ApiBoundaryContractRules.requireNonBlank(aiInvolvement, "aiInvolvement");
    riskTier = ApiBoundaryContractRules.requireNonBlank(riskTier, "riskTier");
    beforeStatus = ApiBoundaryContractRules.sanitizeConsultantVisibleText(beforeStatus, null);
    afterStatus = ApiBoundaryContractRules.sanitizeConsultantVisibleText(afterStatus, null);
    beforeCardStatus = ApiBoundaryContractRules.sanitizeConsultantVisibleText(beforeCardStatus, null);
    afterCardStatus = ApiBoundaryContractRules.sanitizeConsultantVisibleText(afterCardStatus, null);
    reason = ApiBoundaryContractRules.requireNonBlank(reason, "reason");
    occurredAt = ApiBoundaryContractRules.requireNonBlank(occurredAt, "occurredAt");
  }
}
