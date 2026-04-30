package com.recruitingtransactionos.coreapi.identityauth;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record IdentityUserAccount(
    UUID userAccountId,
    UUID organizationId,
    String email,
    String displayName,
    String status,
    String passwordHash,
    Instant lastLoginAt) {

  public IdentityUserAccount {
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    email = Objects.requireNonNull(email, "email must not be null").strip().toLowerCase(Locale.ROOT);
    displayName = Objects.requireNonNull(displayName, "displayName must not be null").strip();
    status = Objects.requireNonNull(status, "status must not be null").strip().toLowerCase(Locale.ROOT);
    if (email.isBlank()) {
      throw new IllegalArgumentException("email must not be blank");
    }
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    if (status.isBlank()) {
      throw new IllegalArgumentException("status must not be blank");
    }
  }

  public boolean isActiveForLogin() {
    return "active".equals(status);
  }

  public boolean hasPasswordHash() {
    return passwordHash != null && !passwordHash.isBlank();
  }
}
