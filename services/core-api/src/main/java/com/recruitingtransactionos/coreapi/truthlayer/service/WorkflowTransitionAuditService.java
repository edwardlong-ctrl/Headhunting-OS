package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionPolicy;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionRegistry;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAuditPolicyRequest;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import java.util.Objects;

public final class WorkflowTransitionAuditService {

  private final WorkflowActionRegistry actionRegistry;
  private final WorkflowEventService workflowEventService;

  public WorkflowTransitionAuditService(WorkflowEventService workflowEventService) {
    this(workflowEventService, WorkflowActionRegistry.standard());
  }

  WorkflowTransitionAuditService(
      WorkflowEventService workflowEventService,
      WorkflowActionRegistry actionRegistry) {
    this.workflowEventService = Objects.requireNonNull(workflowEventService,
        "workflowEventService must not be null");
    this.actionRegistry = Objects.requireNonNull(actionRegistry,
        "actionRegistry must not be null");
  }

  public WorkflowEventAppendResult record(WorkflowTransitionAuditRequest request) {
    WorkflowActionPolicy policy = validateRequest(request);
    return workflowEventService.append(toWorkflowEventAppendCommand(request, policy));
  }

  private WorkflowActionPolicy validateRequest(WorkflowTransitionAuditRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    WorkflowActionCode actionCode = WorkflowActionCode.fromWireValue(request.actionCode());
    WorkflowEntityType entityType = WorkflowEntityType.fromWireValue(request.entityType());
    WorkflowActionPolicy policy = actionRegistry.policyFor(actionCode);
    if (!policy.stateTransition()) {
      throw new IllegalArgumentException("workflow action " + actionCode.wireValue()
          + " is not configured as a state-transition audit action");
    }
    actionRegistry.validate(new WorkflowAuditPolicyRequest(
        actionCode,
        entityType,
        request.actorType(),
        request.aiInvolvement(),
        request.beforeState(),
        request.afterState(),
        request.reason()));
    return policy;
  }

  private static WorkflowEventAppendCommand toWorkflowEventAppendCommand(
      WorkflowTransitionAuditRequest request,
      WorkflowActionPolicy policy) {
    return new WorkflowEventAppendCommand(
        request.organizationId(),
        request.entityNamespace(),
        new EntityRef(request.entityType(), request.entityId()),
        request.entityVersion(),
        policy.actionCode().wireValue(),
        request.beforeState(),
        request.afterState(),
        new ActorRef(request.actorId(), request.actorType()),
        request.sourceType(),
        request.sourceRefId(),
        request.aiTaskRunId(),
        request.reviewEventId(),
        effectiveReason(request, policy),
        request.idempotencyKey(),
        request.correlationId(),
        request.causationId(),
        request.occurredAt());
  }

  private static String effectiveReason(
      WorkflowTransitionAuditRequest request,
      WorkflowActionPolicy policy) {
    if (request.reason() != null && !request.reason().isBlank()) {
      return request.reason();
    }
    return "transition audit recorded for " + policy.actionCode().wireValue();
  }
}
