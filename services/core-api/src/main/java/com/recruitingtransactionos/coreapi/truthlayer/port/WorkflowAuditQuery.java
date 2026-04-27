package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.time.Instant;
import java.util.UUID;

public record WorkflowAuditQuery(
    UUID organizationId,
    WorkflowEventId workflowEventId,
    String entityType,
    UUID entityId,
    String actionCode,
    ActorRole actorType,
    UUID actorId,
    WorkflowCorrelationId correlationId,
    WorkflowCausationId causationId,
    WorkflowIdempotencyKey idempotencyKey,
    Instant occurredFrom,
    Instant occurredTo,
    int limit,
    int offset) {

  public static final int DEFAULT_LIMIT = 50;
  public static final int MAX_LIMIT = 100;

  public static Builder builder(UUID organizationId) {
    return new Builder(organizationId);
  }

  public static final class Builder {
    private final UUID organizationId;
    private WorkflowEventId workflowEventId;
    private String entityType;
    private UUID entityId;
    private String actionCode;
    private ActorRole actorType;
    private UUID actorId;
    private WorkflowCorrelationId correlationId;
    private WorkflowCausationId causationId;
    private WorkflowIdempotencyKey idempotencyKey;
    private Instant occurredFrom;
    private Instant occurredTo;
    private int limit = DEFAULT_LIMIT;
    private int offset;

    private Builder(UUID organizationId) {
      this.organizationId = organizationId;
    }

    public Builder workflowEventId(WorkflowEventId workflowEventId) {
      this.workflowEventId = workflowEventId;
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

    public Builder correlationId(WorkflowCorrelationId correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder causationId(WorkflowCausationId causationId) {
      this.causationId = causationId;
      return this;
    }

    public Builder idempotencyKey(WorkflowIdempotencyKey idempotencyKey) {
      this.idempotencyKey = idempotencyKey;
      return this;
    }

    public Builder occurredFrom(Instant occurredFrom) {
      this.occurredFrom = occurredFrom;
      return this;
    }

    public Builder occurredTo(Instant occurredTo) {
      this.occurredTo = occurredTo;
      return this;
    }

    public Builder limit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder offset(int offset) {
      this.offset = offset;
      return this;
    }

    public WorkflowAuditQuery build() {
      return new WorkflowAuditQuery(
          organizationId,
          workflowEventId,
          entityType,
          entityId,
          actionCode,
          actorType,
          actorId,
          correlationId,
          causationId,
          idempotencyKey,
          occurredFrom,
          occurredTo,
          limit,
          offset);
    }
  }
}
