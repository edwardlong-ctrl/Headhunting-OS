package com.recruitingtransactionos.coreapi.apiboundary;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CandidateTimelineResponse(
    String candidateRef,
    List<TimelineEvent> events) implements ApiSafeResponseBody {

  public CandidateTimelineResponse {
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    events = events == null ? List.of() : List.copyOf(events);
  }

  public record TimelineEvent(
      String eventType,
      String actionCode,
      String status,
      String reason,
      Instant occurredAt) {

    public TimelineEvent {
      Objects.requireNonNull(eventType, "eventType must not be null");
      Objects.requireNonNull(actionCode, "actionCode must not be null");
      Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
  }
}
