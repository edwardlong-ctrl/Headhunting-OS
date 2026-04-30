package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.time.Instant;
import java.util.UUID;

public record CanonicalWriteAttemptQuery(
    UUID organizationId,
    String decision,
    UUID actorUserId,
    String entityType,
    UUID entityId,
    String reasonCode,
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
    private String decision;
    private UUID actorUserId;
    private String entityType;
    private UUID entityId;
    private String reasonCode;
    private WorkflowIdempotencyKey idempotencyKey;
    private Instant occurredFrom;
    private Instant occurredTo;
    private int limit = DEFAULT_LIMIT;
    private int offset;

    private Builder(UUID organizationId) {
      this.organizationId = organizationId;
    }

    public Builder decision(String decision) {
      this.decision = decision;
      return this;
    }

    public Builder actorUserId(UUID actorUserId) {
      this.actorUserId = actorUserId;
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

    public Builder reasonCode(String reasonCode) {
      this.reasonCode = reasonCode;
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

    public CanonicalWriteAttemptQuery build() {
      return new CanonicalWriteAttemptQuery(
          organizationId,
          decision,
          actorUserId,
          entityType,
          entityId,
          reasonCode,
          idempotencyKey,
          occurredFrom,
          occurredTo,
          limit,
          offset);
    }
  }
}
