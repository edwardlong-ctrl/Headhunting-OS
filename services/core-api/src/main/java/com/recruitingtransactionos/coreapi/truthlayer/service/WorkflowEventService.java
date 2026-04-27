package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionRegistry;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAuditPolicyRequest;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import java.util.Objects;

public final class WorkflowEventService {

  private final WorkflowEventPort workflowEventPort;
  private final WorkflowActionRegistry actionRegistry;

  public WorkflowEventService(WorkflowEventPort workflowEventPort) {
    this(workflowEventPort, WorkflowActionRegistry.standard());
  }

  WorkflowEventService(
      WorkflowEventPort workflowEventPort,
      WorkflowActionRegistry actionRegistry) {
    this.workflowEventPort = Objects.requireNonNull(workflowEventPort,
        "workflowEventPort must not be null");
    this.actionRegistry = Objects.requireNonNull(actionRegistry,
        "actionRegistry must not be null");
  }

  public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
    validateAppendCommand(command);
    return workflowEventPort.append(command);
  }

  private void validateAppendCommand(WorkflowEventAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.organizationId(), "organizationId must not be null");
    Objects.requireNonNull(command.entity(), "entity must not be null");
    Objects.requireNonNull(command.entity().entityId(), "entityId must not be null");
    Objects.requireNonNull(command.beforeState(), "beforeState must not be null");
    Objects.requireNonNull(command.afterState(), "afterState must not be null");
    Objects.requireNonNull(command.actor(), "actor must not be null");
    Objects.requireNonNull(command.actor().userId(), "actorUserId must not be null");
    Objects.requireNonNull(command.actor().role(), "actorRole must not be null");
    Objects.requireNonNull(command.occurredAt(), "occurredAt must not be null");
    actionRegistry.validate(new WorkflowAuditPolicyRequest(
        WorkflowActionCode.fromWireValue(command.action()),
        WorkflowEntityType.fromWireValue(command.entity().entityType()),
        command.actor().role(),
        aiInvolvement(command),
        command.beforeState(),
        command.afterState(),
        command.reason()));
  }

  private static WorkflowAiInvolvement aiInvolvement(WorkflowEventAppendCommand command) {
    if (command.actor().role() == ActorRole.AI) {
      return WorkflowAiInvolvement.AI_AUTOMATED_LOW_RISK;
    }
    if (command.aiTaskRunId() != null) {
      return WorkflowAiInvolvement.AI_ASSISTED;
    }
    return WorkflowAiInvolvement.NONE;
  }
}
