package com.recruitingtransactionos.coreapi.identityauth;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

public record RtoAuthenticatedPrincipal(
    UUID userAccountId,
    UUID organizationId,
    PortalRole portalRole,
    String displayName,
    UUID sessionId) implements Principal {

  public RtoAuthenticatedPrincipal {
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(portalRole, "portalRole must not be null");
    displayName = Objects.requireNonNull(displayName, "displayName must not be null").strip();
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
  }

  @Override
  public String getName() {
    return userAccountId.toString();
  }
}
