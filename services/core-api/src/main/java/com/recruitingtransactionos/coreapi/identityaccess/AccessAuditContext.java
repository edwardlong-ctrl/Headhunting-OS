package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;
import java.util.UUID;

public record AccessAuditContext(
    UUID organizationId,
    UUID actorUserId,
    UUID targetEntityId,
    String sensitivityLevel) {

  public AccessAuditContext {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    Objects.requireNonNull(targetEntityId, "targetEntityId must not be null");
    sensitivityLevel = requireNonBlank(sensitivityLevel, "sensitivityLevel");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
