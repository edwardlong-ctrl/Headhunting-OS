package com.recruitingtransactionos.coreapi.apiboundary;

import java.time.Instant;
import java.util.Objects;

public record CandidateOpportunityResponse(
    String interactionId,
    String jobTitle,
    String companyName,
    String status,
    String interactionType,
    Instant startedAt,
    Instant updatedAt) implements ApiSafeResponseBody {

  public CandidateOpportunityResponse {
    Objects.requireNonNull(interactionId, "interactionId must not be null");
    Objects.requireNonNull(jobTitle, "jobTitle must not be null");
    Objects.requireNonNull(companyName, "companyName must not be null");
    Objects.requireNonNull(status, "status must not be null");
  }
}
