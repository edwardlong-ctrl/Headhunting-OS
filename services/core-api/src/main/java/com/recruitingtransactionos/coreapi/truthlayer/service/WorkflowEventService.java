package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionRegistry;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAuditPolicyRequest;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.util.Objects;
import java.util.Optional;

public final class WorkflowEventService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    Optional<WorkflowEventIdempotencyRecord> existing = findExistingIdempotentAppend(command);
    if (existing.isPresent()) {
      WorkflowEventIdempotencyRecord record = existing.get();
      if (!isEquivalent(record.command(), command)) {
        throw new IllegalArgumentException(
            "workflow event idempotency conflict for key " + command.idempotencyKey().value());
      }
      return new WorkflowEventAppendResult(record.workflowEventId());
    }
    try {
      return workflowEventPort.append(command);
    } catch (IllegalStateException exception) {
      return resolveIdempotentAppendFailure(command, exception);
    }
  }

  private Optional<WorkflowEventIdempotencyRecord> findExistingIdempotentAppend(
      WorkflowEventAppendCommand command) {
    if (command.idempotencyKey() == null) {
      return Optional.empty();
    }
    return workflowEventPort.findByIdempotencyKey(
        command.organizationId(),
        command.idempotencyKey());
  }

  private WorkflowEventAppendResult resolveIdempotentAppendFailure(
      WorkflowEventAppendCommand command,
      IllegalStateException exception) {
    if (command.idempotencyKey() == null) {
      throw exception;
    }
    Optional<WorkflowEventIdempotencyRecord> existing = findExistingIdempotentAppend(command);
    if (existing.isEmpty()) {
      throw exception;
    }
    WorkflowEventIdempotencyRecord record = existing.get();
    if (!isEquivalent(record.command(), command)) {
      throw new IllegalArgumentException(
          "workflow event idempotency conflict for key " + command.idempotencyKey().value(),
          exception);
    }
    return new WorkflowEventAppendResult(record.workflowEventId());
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

  private static boolean isEquivalent(
      WorkflowEventAppendCommand existing,
      WorkflowEventAppendCommand requested) {
    return Objects.equals(existing.organizationId(), requested.organizationId())
        && Objects.equals(existing.entityNamespace(), requested.entityNamespace())
        && Objects.equals(existing.entity(), requested.entity())
        && Objects.equals(existing.entityVersion(), requested.entityVersion())
        && Objects.equals(existing.action(), requested.action())
        && jsonEquivalent(existing.beforeState(), requested.beforeState())
        && jsonEquivalent(existing.afterState(), requested.afterState())
        && Objects.equals(existing.actor(), requested.actor())
        && Objects.equals(existing.sourceType(), requested.sourceType())
        && Objects.equals(existing.sourceRefId(), requested.sourceRefId())
        && Objects.equals(existing.aiTaskRunId(), requested.aiTaskRunId())
        && Objects.equals(existing.reviewEventId(), requested.reviewEventId())
        && Objects.equals(existing.reason(), requested.reason())
        && Objects.equals(existing.idempotencyKey(), requested.idempotencyKey())
        && Objects.equals(existing.correlationId(), requested.correlationId())
        && Objects.equals(existing.causationId(), requested.causationId())
        && Objects.equals(existing.occurredAt(), requested.occurredAt());
  }

  private static boolean jsonEquivalent(
      WorkflowStateSnapshot existing,
      WorkflowStateSnapshot requested) {
    if (Objects.equals(existing, requested)) {
      return true;
    }
    try {
      return OBJECT_MAPPER.readTree(existing.json())
          .equals(OBJECT_MAPPER.readTree(requested.json()));
    } catch (JsonProcessingException exception) {
      return false;
    }
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
