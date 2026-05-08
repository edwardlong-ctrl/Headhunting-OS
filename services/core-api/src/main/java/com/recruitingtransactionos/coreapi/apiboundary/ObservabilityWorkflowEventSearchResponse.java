package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.observability.ObservabilityWorkflowEventResponse;
import java.util.List;

public record ObservabilityWorkflowEventSearchResponse(
    List<ObservabilityWorkflowEventResponse> items,
    int limit,
    int offset,
    boolean hasMore) implements ApiSafeResponseBody {

  public ObservabilityWorkflowEventSearchResponse {
    items = List.copyOf(items);
  }
}
