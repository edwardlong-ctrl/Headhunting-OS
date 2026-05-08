package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;

public final class PermissionEnforcer {

  private final PermissionEvaluator permissionEvaluator;
  private final AccessAuditRecorder accessAuditRecorder;

  public PermissionEnforcer(PermissionEvaluator permissionEvaluator) {
    this(permissionEvaluator, AccessAuditRecorder.noop());
  }

  public PermissionEnforcer(
      PermissionEvaluator permissionEvaluator,
      AccessAuditRecorder accessAuditRecorder) {
    this.permissionEvaluator =
        Objects.requireNonNull(permissionEvaluator, "permissionEvaluator must not be null");
    this.accessAuditRecorder =
        Objects.requireNonNull(accessAuditRecorder, "accessAuditRecorder must not be null");
  }

  public AccessDecision evaluate(AccessRequest request) {
    if (request == null) {
      return deny("access_request_required", "Access request is required and fails closed.");
    }
    try {
      AccessDecision decision = permissionEvaluator.evaluate(request);
      if (decision == null) {
        return deny(
            "permission_evaluator_returned_no_decision",
            "Permission evaluator returned no decision and fails closed.");
      }
      return decision;
    } catch (RuntimeException exception) {
      return deny(
          "permission_evaluation_failed_closed",
          "Permission evaluation failed closed.");
    }
  }

  public AccessDecision requireAllowed(AccessRequest request) {
    AccessDecision decision = evaluate(request);
    if (!decision.allowed()) {
      throw new AccessDeniedException(decision);
    }
    return decision;
  }

  public AccessDecision requireAllowed(AccessRequest request, AccessAuditContext auditContext) {
    Objects.requireNonNull(auditContext, "auditContext must not be null");
    AccessDecision decision = evaluate(request);
    accessAuditRecorder.record(new AccessAuditEvent(request, decision, auditContext));
    if (!decision.allowed()) {
      throw new AccessDeniedException(decision);
    }
    return decision;
  }

  private static AccessDecision deny(String reasonCode, String safeExplanation) {
    return new AccessDecision(false, reasonCode, safeExplanation);
  }
}
