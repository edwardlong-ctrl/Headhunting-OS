package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record PagedResult<T>(
    List<T> items,
    int totalCount,
    int limit,
    int offset,
    boolean hasMore) implements ApiSafeResponseBody {

  public PagedResult {
    Objects.requireNonNull(items, "items must not be null");
    if (totalCount < 0) {
      throw new IllegalArgumentException("totalCount must be >= 0, got " + totalCount);
    }
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be >= 1, got " + limit);
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be >= 0, got " + offset);
    }
  }

  public static <T> PagedResult<T> empty(int limit, int offset) {
    return new PagedResult<>(Collections.emptyList(), 0, limit, offset, false);
  }

  public static <T> PagedResult<T> of(List<T> items, int totalCount, int limit, int offset) {
    boolean hasMore = (offset + limit) < totalCount;
    return new PagedResult<>(items, totalCount, limit, offset, hasMore);
  }
}
