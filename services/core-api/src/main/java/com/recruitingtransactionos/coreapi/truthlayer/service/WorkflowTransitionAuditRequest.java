package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WorkflowTransitionAuditRequest(
    UUID organizationId,
    String entityNamespace,
    String entityType,
    UUID entityId,
    Integer entityVersion,
    String actionCode,
    ActorRole actorType,
    UUID actorId,
    WorkflowAiInvolvement aiInvolvement,
    WorkflowStateSnapshot beforeState,
    WorkflowStateSnapshot afterState,
    String reason,
    WorkflowIdempotencyKey idempotencyKey,
    WorkflowCorrelationId correlationId,
    WorkflowCausationId causationId,
    String sourceType,
    UUID sourceRefId,
    AITaskRunId aiTaskRunId,
    ReviewEventId reviewEventId,
    Instant occurredAt) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public WorkflowTransitionAuditRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    entityNamespace = requireNonBlank(entityNamespace, "entityNamespace");
    entityType = requireNonBlank(entityType, "entityType");
    Objects.requireNonNull(entityId, "entityId must not be null");
    actionCode = requireNonBlank(actionCode, "actionCode");
    Objects.requireNonNull(actorType, "actorType must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(aiInvolvement, "aiInvolvement must not be null");
    Objects.requireNonNull(beforeState, "beforeState must not be null");
    Objects.requireNonNull(afterState, "afterState must not be null");
    if (equivalentState(beforeState, afterState)) {
      throw new IllegalArgumentException("beforeState and afterState must not be equal");
    }
    sourceType = requireNonBlank(sourceType, "sourceType");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  private static boolean equivalentState(
      WorkflowStateSnapshot beforeState,
      WorkflowStateSnapshot afterState) {
    if (Objects.equals(beforeState, afterState)) {
      return true;
    }
    try {
      return OBJECT_MAPPER.readTree(beforeState.json())
          .equals(OBJECT_MAPPER.readTree(afterState.json()));
    } catch (JsonProcessingException exception) {
      return Objects.equals(beforeState.json(), afterState.json());
    }
  }

  public static final class Builder {
    private UUID organizationId;
    private String entityNamespace;
    private String entityType;
    private UUID entityId;
    private Integer entityVersion;
    private String actionCode;
    private ActorRole actorType;
    private UUID actorId;
    private WorkflowAiInvolvement aiInvolvement;
    private WorkflowStateSnapshot beforeState;
    private WorkflowStateSnapshot afterState;
    private String reason;
    private WorkflowIdempotencyKey idempotencyKey;
    private WorkflowCorrelationId correlationId;
    private WorkflowCausationId causationId;
    private String sourceType;
    private UUID sourceRefId;
    private AITaskRunId aiTaskRunId;
    private ReviewEventId reviewEventId;
    private Instant occurredAt;

    private Builder() {
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder entityNamespace(String entityNamespace) {
      this.entityNamespace = entityNamespace;
      return this;
    }

    public Builder entityType(String entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder entityId(UUID entityId) {
      this.entityId = entityId;
      return this;
    }

    public Builder entityVersion(Integer entityVersion) {
      this.entityVersion = entityVersion;
      return this;
    }

    public Builder actionCode(String actionCode) {
      this.actionCode = actionCode;
      return this;
    }

    public Builder actorType(ActorRole actorType) {
      this.actorType = actorType;
      return this;
    }

    public Builder actorId(UUID actorId) {
      this.actorId = actorId;
      return this;
    }

    public Builder aiInvolvement(WorkflowAiInvolvement aiInvolvement) {
      this.aiInvolvement = aiInvolvement;
      return this;
    }

    public Builder beforeState(String beforeState) {
      this.beforeState = beforeState == null ? null : new WorkflowStateSnapshot(beforeState);
      return this;
    }

    public Builder beforeState(WorkflowStateSnapshot beforeState) {
      this.beforeState = beforeState;
      return this;
    }

    public Builder afterState(String afterState) {
      this.afterState = afterState == null ? null : new WorkflowStateSnapshot(afterState);
      return this;
    }

    public Builder afterState(WorkflowStateSnapshot afterState) {
      this.afterState = afterState;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder idempotencyKey(String idempotencyKey) {
      this.idempotencyKey = idempotencyKey == null
          ? null
          : new WorkflowIdempotencyKey(idempotencyKey);
      return this;
    }

    public Builder idempotencyKey(WorkflowIdempotencyKey idempotencyKey) {
      this.idempotencyKey = idempotencyKey;
      return this;
    }

    public Builder correlationId(UUID correlationId) {
      this.correlationId = correlationId == null
          ? null
          : new WorkflowCorrelationId(correlationId);
      return this;
    }

    public Builder correlationId(WorkflowCorrelationId correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder causationId(UUID causationId) {
      this.causationId = causationId == null
          ? null
          : new WorkflowCausationId(causationId);
      return this;
    }

    public Builder causationId(WorkflowCausationId causationId) {
      this.causationId = causationId;
      return this;
    }

    public Builder sourceType(String sourceType) {
      this.sourceType = sourceType;
      return this;
    }

    public Builder sourceRefId(UUID sourceRefId) {
      this.sourceRefId = sourceRefId;
      return this;
    }

    public Builder aiTaskRunId(UUID aiTaskRunId) {
      this.aiTaskRunId = aiTaskRunId == null ? null : new AITaskRunId(aiTaskRunId);
      return this;
    }

    public Builder aiTaskRunId(AITaskRunId aiTaskRunId) {
      this.aiTaskRunId = aiTaskRunId;
      return this;
    }

    public Builder reviewEventId(UUID reviewEventId) {
      this.reviewEventId = reviewEventId == null ? null : new ReviewEventId(reviewEventId);
      return this;
    }

    public Builder reviewEventId(ReviewEventId reviewEventId) {
      this.reviewEventId = reviewEventId;
      return this;
    }

    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public WorkflowTransitionAuditRequest build() {
      return new WorkflowTransitionAuditRequest(
          organizationId,
          entityNamespace,
          entityType,
          entityId,
          entityVersion,
          actionCode,
          actorType,
          actorId,
          aiInvolvement,
          beforeState,
          afterState,
          reason,
          idempotencyKey,
          correlationId,
          causationId,
          sourceType,
          sourceRefId,
          aiTaskRunId,
          reviewEventId,
          occurredAt);
    }
  }
}
