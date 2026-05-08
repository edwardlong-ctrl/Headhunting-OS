package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;

public record AccessAuditEvent(
    AccessRequest request,
    AccessDecision decision,
    AccessAuditContext context) {

  public AccessAuditEvent {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    Objects.requireNonNull(context, "context must not be null");
  }
}
