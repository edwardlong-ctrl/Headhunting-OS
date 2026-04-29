package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.UUID;

public record ClientSafeCandidateCardQueryScope(UUID organizationId) {

  public ClientSafeCandidateCardQueryScope {
    if (organizationId == null) {
      throw new NullPointerException("organizationId must not be null");
    }
  }

  public static ClientSafeCandidateCardQueryScope of(UUID organizationId) {
    return new ClientSafeCandidateCardQueryScope(organizationId);
  }
}
