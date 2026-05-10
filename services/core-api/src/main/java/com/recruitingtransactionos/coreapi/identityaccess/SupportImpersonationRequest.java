package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;
import java.util.UUID;

public record SupportImpersonationRequest(
    UUID actorOrganizationId,
    UUID actorUserId,
    PortalRole actorRole,
    UUID targetOrganizationId,
    UUID targetUserId,
    PortalRole targetRole,
    String ticketRef,
    String reason,
    boolean breakGlassApproved) {

  public SupportImpersonationRequest {
    Objects.requireNonNull(actorOrganizationId, "actorOrganizationId must not be null");
    Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    Objects.requireNonNull(targetOrganizationId, "targetOrganizationId must not be null");
    Objects.requireNonNull(targetUserId, "targetUserId must not be null");
    Objects.requireNonNull(targetRole, "targetRole must not be null");
    ticketRef = ticketRef == null ? "" : ticketRef.strip();
    reason = reason == null ? "" : reason.strip();
  }
}
