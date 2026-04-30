package com.recruitingtransactionos.coreapi.identityauth;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record IdentityAuthSession(
    UUID sessionId,
    UUID organizationId,
    UUID userAccountId,
    PortalRole portalRole,
    String refreshTokenHash,
    Instant expiresAt,
    Instant revokedAt,
    Instant createdAt,
    Instant lastUsedAt,
    int version) {

  public IdentityAuthSession {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(portalRole, "portalRole must not be null");
    refreshTokenHash = Objects.requireNonNull(refreshTokenHash, "refreshTokenHash must not be null").strip();
    Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    if (refreshTokenHash.isBlank()) {
      throw new IllegalArgumentException("refreshTokenHash must not be blank");
    }
    if (version < 1) {
      throw new IllegalArgumentException("version must be greater than zero");
    }
  }

  public boolean isActiveAt(Instant instant) {
    Objects.requireNonNull(instant, "instant must not be null");
    return revokedAt == null && expiresAt.isAfter(instant);
  }
}
