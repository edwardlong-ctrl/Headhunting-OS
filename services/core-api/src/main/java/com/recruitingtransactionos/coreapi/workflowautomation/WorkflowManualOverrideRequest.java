package com.recruitingtransactionos.coreapi.workflowautomation;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.util.Objects;
import java.util.UUID;

public record WorkflowManualOverrideRequest(
    UUID organizationId,
    UUID entityId,
    String actionCode,
    PortalRole actorRole,
    String reason) {

  public WorkflowManualOverrideRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(entityId, "entityId must not be null");
    actionCode = requireNonBlank(actionCode, "actionCode");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    reason = requireNonBlank(reason, "override reason");
  }

  public static WorkflowManualOverrideRequest create(
      UUID organizationId,
      UUID entityId,
      String actionCode,
      PortalRole actorRole,
      String reason) {
    return new WorkflowManualOverrideRequest(organizationId, entityId, actionCode, actorRole, reason);
  }

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
