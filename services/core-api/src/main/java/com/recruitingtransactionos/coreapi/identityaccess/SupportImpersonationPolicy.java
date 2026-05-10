package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;
import java.util.Set;

public final class SupportImpersonationPolicy {

  private final AccessAuditRecorder accessAuditRecorder;

  public SupportImpersonationPolicy(AccessAuditRecorder accessAuditRecorder) {
    this.accessAuditRecorder = Objects.requireNonNull(
        accessAuditRecorder,
        "accessAuditRecorder must not be null");
  }

  public SupportImpersonationDecision authorize(SupportImpersonationRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    SupportImpersonationDecision decision = evaluate(request);
    accessAuditRecorder.record(new AccessAuditEvent(
        accessRequest(request),
        new AccessDecision(decision.allowed(), decision.reasonCode(), decision.safeExplanation()),
        new AccessAuditContext(
            request.actorOrganizationId(),
            request.actorUserId(),
            request.targetUserId(),
            FieldClassification.SYSTEM_GOVERNANCE.wireValue())));
    return decision;
  }

  private static SupportImpersonationDecision evaluate(SupportImpersonationRequest request) {
    if (request.actorRole() != PortalRole.ADMIN) {
      return deny("support_impersonation_admin_required", "Only same-organization Admin can request support impersonation.", request);
    }
    if (!request.actorOrganizationId().equals(request.targetOrganizationId())) {
      return deny("support_impersonation_cross_org_denied", "Support impersonation cannot cross organization boundaries.", request);
    }
    if (request.ticketRef().isBlank()) {
      return deny("support_impersonation_ticket_required", "Support impersonation requires a support ticket reference.", request);
    }
    if (request.reason().isBlank()) {
      return deny("support_impersonation_reason_required", "Support impersonation requires a human-readable reason.", request);
    }
    if (!request.breakGlassApproved()) {
      return deny("support_impersonation_break_glass_required", "Support impersonation requires explicit break-glass approval.", request);
    }
    if (request.targetRole() == PortalRole.ADMIN
        || request.targetRole() == PortalRole.SYSTEM
        || request.targetRole() == PortalRole.AI_ASSISTANT
        || request.targetRole() == PortalRole.UNKNOWN) {
      return deny("support_impersonation_governance_role_denied", "Support impersonation cannot target governance or automation roles.", request);
    }
    return SupportImpersonationDecision.allow(
        "support_impersonation_same_org_admin_allowed",
        "Same-organization Admin support impersonation is explicitly ticketed, reasoned, approved, and audited.",
        request.ticketRef());
  }

  private static SupportImpersonationDecision deny(
      String reasonCode,
      String safeExplanation,
      SupportImpersonationRequest request) {
    return SupportImpersonationDecision.deny(reasonCode, safeExplanation, request.ticketRef());
  }

  private static AccessRequest accessRequest(SupportImpersonationRequest request) {
    return new AccessRequest(
        request.actorRole(),
        ResourceType.USER_ACCOUNT,
        AccessAction.IMPERSONATE,
        FieldClassification.SYSTEM_GOVERNANCE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }
}
