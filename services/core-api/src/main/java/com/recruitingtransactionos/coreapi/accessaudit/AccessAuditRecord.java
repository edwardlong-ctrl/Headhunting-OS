package com.recruitingtransactionos.coreapi.accessaudit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AccessAuditRecord(
    UUID auditLogId,
    UUID organizationId,
    UUID actorUserId,
    String actorRole,
    String action,
    String targetEntityType,
    UUID targetEntityId,
    String result,
    String reason,
    String sensitivityLevel,
    Instant occurredAt) {

  public AccessAuditRecord {
    Objects.requireNonNull(auditLogId, "auditLogId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    actorRole = requireNonBlank(actorRole, "actorRole");
    action = requireNonBlank(action, "action");
    targetEntityType = requireNonBlank(targetEntityType, "targetEntityType");
    Objects.requireNonNull(targetEntityId, "targetEntityId must not be null");
    result = requireNonBlank(result, "result");
    reason = reason == null ? null : reason.strip();
    sensitivityLevel = sensitivityLevel == null ? null : sensitivityLevel.strip();
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    String stripped = value.strip();
    if (stripped.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return stripped;
  }
}
