package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.observability.ObservabilityReviewEventResponse;
import java.util.List;

public record ObservabilityReviewEventSearchResponse(
    List<ObservabilityReviewEventResponse> items,
    int limit,
    int offset,
    boolean hasMore) implements ApiSafeResponseBody {

  public ObservabilityReviewEventSearchResponse {
    items = List.copyOf(items);
  }
}
