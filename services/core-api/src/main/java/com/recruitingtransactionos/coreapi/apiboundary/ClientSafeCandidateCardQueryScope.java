package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.UUID;

public record ClientSafeCandidateCardQueryScope(UUID organizationId) {

  private static final ClientSafeCandidateCardQueryScope UNSCOPED =
      new ClientSafeCandidateCardQueryScope(null);

  public static ClientSafeCandidateCardQueryScope of(UUID organizationId) {
    return new ClientSafeCandidateCardQueryScope(organizationId);
  }

  public static ClientSafeCandidateCardQueryScope unscoped() {
    return UNSCOPED;
  }

  public boolean hasOrganizationScope() {
    return organizationId != null;
  }
}
