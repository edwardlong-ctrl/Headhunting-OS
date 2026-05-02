package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantWorkflowTimelineResponse(
    List<ConsultantWorkflowEventResponse> items,
    List<ConsultantWorkflowEntityStateResponse> entityStates,
    int limit,
    int offset,
    boolean hasMore) implements ApiSafeResponseBody {

  public ConsultantWorkflowTimelineResponse {
    items = List.copyOf(items == null ? List.of() : items);
    entityStates = List.copyOf(entityStates == null ? List.of() : entityStates);
  }
}
