package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.observability.ObservabilityAccessAuditEventResponse;
import java.util.List;

public record ObservabilityAccessAuditSearchResponse(
    List<ObservabilityAccessAuditEventResponse> items,
    int limit,
    int offset,
    boolean hasMore) implements ApiSafeResponseBody {

  public ObservabilityAccessAuditSearchResponse {
    items = List.copyOf(items);
  }
}
