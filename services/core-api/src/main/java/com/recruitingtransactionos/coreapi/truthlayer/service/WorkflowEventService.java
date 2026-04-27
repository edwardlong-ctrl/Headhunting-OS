package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import java.util.Objects;

public final class WorkflowEventService {

  private final WorkflowEventPort workflowEventPort;

  public WorkflowEventService(WorkflowEventPort workflowEventPort) {
    this.workflowEventPort = Objects.requireNonNull(workflowEventPort,
        "workflowEventPort must not be null");
  }

  public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
    validateAppendCommand(command);
    return workflowEventPort.append(command);
  }

  private static void validateAppendCommand(WorkflowEventAppendCommand command) {
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
  }
}
