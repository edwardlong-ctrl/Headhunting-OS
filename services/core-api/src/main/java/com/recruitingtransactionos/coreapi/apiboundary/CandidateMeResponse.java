package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.Objects;

public record CandidateMeResponse(
    String candidateRef,
    String displayName,
    String organizationId,
    String currentProfileVersion,
    int documentCount,
    int activeOpportunityCount,
    int pendingFollowUpCount) implements ApiSafeResponseBody {

  public CandidateMeResponse {
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    Objects.requireNonNull(displayName, "displayName must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(currentProfileVersion, "currentProfileVersion must not be null");
  }
}
