package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.observability.ObservabilityAITaskRunResponse;
import java.util.List;

public record ObservabilityAITaskRunSearchResponse(
    List<ObservabilityAITaskRunResponse> items,
    int limit,
    int offset,
    boolean hasMore) implements ApiSafeResponseBody {

  public ObservabilityAITaskRunSearchResponse {
    items = List.copyOf(items);
  }
}
