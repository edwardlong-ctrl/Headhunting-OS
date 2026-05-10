package com.recruitingtransactionos.coreapi.accessaudit;

import java.util.Objects;
import java.util.UUID;

public record AccessAuditSearchQuery(
    UUID organizationId,
    String action,
    String targetEntityType,
    String result,
    UUID actorUserId,
    UUID targetEntityId,
    int limit,
    int offset) {

  public AccessAuditSearchQuery {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    action = normalize(action);
    targetEntityType = normalize(targetEntityType);
    result = normalize(result);
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
