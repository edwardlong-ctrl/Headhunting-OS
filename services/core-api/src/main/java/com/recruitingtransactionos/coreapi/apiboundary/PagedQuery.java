package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.Objects;
import java.util.UUID;

public record PagedQuery(
    UUID organizationId,
    int limit,
    int offset) {

  public static final int DEFAULT_LIMIT = 20;
  public static final int MAX_LIMIT = 100;

  public PagedQuery {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be >= 1, got " + limit);
    }
    if (limit > MAX_LIMIT) {
      throw new IllegalArgumentException(
          "limit must be <= " + MAX_LIMIT + ", got " + limit);
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be >= 0, got " + offset);
    }
  }

  public static Builder builder(UUID organizationId) {
    return new Builder(organizationId);
  }

  public static final class Builder {
    private final UUID organizationId;
    private int limit = DEFAULT_LIMIT;
    private int offset;

    private Builder(UUID organizationId) {
      this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
    }

    public Builder limit(int limit) { this.limit = limit; return this; }
    public Builder offset(int offset) { this.offset = offset; return this; }

    public PagedQuery build() {
      return new PagedQuery(organizationId, limit, offset);
    }
  }
}
