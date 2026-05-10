package com.recruitingtransactionos.coreapi.supportops;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.util.Objects;

public final class SupportOperationsPermissionPolicy {

  public SupportAuthorizationDecision authorize(SupportCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    if (command.actor().role() != PortalRole.ADMIN) {
      return SupportAuthorizationDecision.deny("support_admin_role_required");
    }
    if (!command.actor().organizationId().equals(command.targetOrganizationId())) {
      return SupportAuthorizationDecision.deny("support_same_org_required");
    }
    if (command.ticketRef().isBlank()) {
      return SupportAuthorizationDecision.deny("support_ticket_required");
    }
    if (command.reason().isBlank()) {
      return SupportAuthorizationDecision.deny("support_reason_required");
    }
    return SupportAuthorizationDecision.allow();
  }
}
