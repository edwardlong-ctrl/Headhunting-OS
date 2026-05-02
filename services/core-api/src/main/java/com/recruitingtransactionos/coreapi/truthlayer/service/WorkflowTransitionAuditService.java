package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionPolicy;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionRegistry;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAuditPolicyRequest;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionBlocker;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionDecision;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionLegalityPolicy;
import java.util.Objects;
import java.util.Optional;

public final class WorkflowTransitionAuditService {

  private final WorkflowActionRegistry actionRegistry;
  private final WorkflowTransitionLegalityPolicy legalityPolicy;
  private final WorkflowEventService workflowEventService;
  private final WorkflowEntityStatePort entityStatePort;

  public WorkflowTransitionAuditService(
      WorkflowEventService workflowEventService,
      WorkflowEntityStatePort entityStatePort) {
    this(
        workflowEventService,
        entityStatePort,
        WorkflowActionRegistry.standard(),
        WorkflowTransitionLegalityPolicy.standard());
  }

  WorkflowTransitionAuditService(
      WorkflowEventService workflowEventService,
      WorkflowEntityStatePort entityStatePort,
      WorkflowActionRegistry actionRegistry,
      WorkflowTransitionLegalityPolicy legalityPolicy) {
    this.workflowEventService = Objects.requireNonNull(workflowEventService,
        "workflowEventService must not be null");
    this.entityStatePort = Objects.requireNonNull(entityStatePort,
        "entityStatePort must not be null");
    this.actionRegistry = Objects.requireNonNull(actionRegistry,
        "actionRegistry must not be null");
    this.legalityPolicy = Objects.requireNonNull(
        legalityPolicy, "legalityPolicy must not be null");
  }

  public WorkflowEventAppendResult record(WorkflowTransitionAuditRequest request) {
    WorkflowActionPolicy policy = validateRequest(request, true);
    Optional<WorkflowTransitionBlocker> stateMismatchBlocker = validateCurrentState(request);
    if (stateMismatchBlocker.isPresent()) {
      String currentStatus = extractStatus(entityStatePort.getCurrentStateJson(
          request.organizationId(),
          request.entityNamespace(),
          request.entityType(),
          request.entityId()).orElse(null));
      String beforeStatus = extractStatus(request.beforeState().json());
      throw new IllegalArgumentException(
          "workflow transition beforeState does not match actual entity state: "
              + currentStatus + " vs " + beforeStatus);
    }

    return workflowEventService.append(toWorkflowEventAppendCommand(request, policy));
  }

  public WorkflowTransitionDecision preview(WorkflowTransitionAuditRequest request) {
    WorkflowActionPolicy policy = validateRequest(request, false);
    Optional<WorkflowTransitionBlocker> stateMismatchBlocker = validateCurrentState(request);
    if (stateMismatchBlocker.isPresent()) {
      return WorkflowTransitionDecision.blocked(
          policy.actionCode().wireValue(),
          extractStatus(request.beforeState().json()),
          extractStatus(request.afterState().json()),
          java.util.List.of(stateMismatchBlocker.get()));
    }
    return legalityPolicy.evaluate(
        policy.actionCode(),
        request.beforeState(),
        request.afterState());
  }

  private WorkflowActionPolicy validateRequest(
      WorkflowTransitionAuditRequest request,
      boolean enforceLegality) {
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
    if (enforceLegality) {
      legalityPolicy.enforce(actionCode, request.beforeState(), request.afterState());
    }
    return policy;
  }

  private Optional<WorkflowTransitionBlocker> validateCurrentState(
      WorkflowTransitionAuditRequest request) {
    Optional<String> currentStateOpt = entityStatePort.getCurrentStateJson(
        request.organizationId(),
        request.entityNamespace(),
        request.entityType(),
        request.entityId());
    if (currentStateOpt.isPresent()
        && request.beforeState() != null
        && request.beforeState().json() != null
        && !request.beforeState().json().isBlank()) {
      String currentStatus = extractStatus(currentStateOpt.get());
      String beforeStatus = extractStatus(request.beforeState().json());
      if (currentStatus != null && beforeStatus != null && !currentStatus.equals(beforeStatus)) {
        return Optional.of(new WorkflowTransitionBlocker(
            "before_state_mismatch",
            "Current entity state no longer matches the requested workflow transition."));
      }
    }
    return Optional.empty();
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

  private static String extractStatus(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      com.fasterxml.jackson.databind.JsonNode node =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
      if (node.has("status")) {
        return node.get("status").asText();
      }
    } catch (Exception e) {
      // ignore
    }
    return null;
  }
}
