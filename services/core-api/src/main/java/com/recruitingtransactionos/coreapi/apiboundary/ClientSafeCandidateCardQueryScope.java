package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.Objects;
import java.util.UUID;

public record ClientSafeCandidateCardQueryScope(UUID organizationId) {

  public ClientSafeCandidateCardQueryScope {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
  }

  public static ClientSafeCandidateCardQueryScope of(UUID organizationId) {
    return new ClientSafeCandidateCardQueryScope(organizationId);
  }
}
