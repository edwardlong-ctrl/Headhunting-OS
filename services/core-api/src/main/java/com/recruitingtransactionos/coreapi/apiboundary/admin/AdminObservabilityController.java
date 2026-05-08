package com.recruitingtransactionos.coreapi.apiboundary.admin;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.observability.ObservabilityAITaskRunQuery;
import com.recruitingtransactionos.coreapi.observability.ObservabilityDisclosureAuditExportQuery;
import com.recruitingtransactionos.coreapi.observability.ObservabilityReadService;
import com.recruitingtransactionos.coreapi.observability.ObservabilityReviewEventQuery;
import com.recruitingtransactionos.coreapi.observability.ObservabilityWorkflowEventQuery;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/observability")
public final class AdminObservabilityController {

  private final ObservabilityReadService observabilityReadService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public AdminObservabilityController(ObservabilityReadService observabilityReadService) {
    this(observabilityReadService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  AdminObservabilityController(
      ObservabilityReadService observabilityReadService,
      PermissionEnforcer permissionEnforcer) {
    this.observabilityReadService = Objects.requireNonNull(
        observabilityReadService,
        "observabilityReadService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  @GetMapping("/workflow-events")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> workflowEvents(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(required = false) UUID workflowEventId,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) UUID entityId,
      @RequestParam(required = false) String actionCode,
      @RequestParam(required = false) String actorType,
      @RequestParam(required = false) UUID actorId,
      @RequestParam(required = false) UUID correlationId,
      @RequestParam(required = false) UUID causationId,
      @RequestParam(required = false) Instant occurredFrom,
      @RequestParam(required = false) Instant occurredTo,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> denied = denyUnlessAdmin(principal);
    if (denied != null) {
      return denied;
    }
    permissionEnforcer.requireAllowed(adminGovernanceReadAccessRequest(principal.portalRole()));
    return ResponseEntity.ok(ApiResponseEnvelope.success(observabilityReadService.searchWorkflowEvents(
        new ObservabilityWorkflowEventQuery(
            principal.organizationId(),
            workflowEventId,
            entityType,
            entityId,
            actionCode,
            actorType,
            actorId,
            correlationId,
            causationId,
            occurredFrom,
            occurredTo,
            limit,
            offset))));
  }

  @GetMapping("/review-events")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> reviewEvents(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(required = false) String targetEntityType,
      @RequestParam(required = false) UUID targetEntityId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID claimLedgerItemId,
      @RequestParam(required = false) UUID reviewerUserId,
      @RequestParam(required = false) Instant createdFrom,
      @RequestParam(required = false) Instant createdTo,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> denied = denyUnlessAdmin(principal);
    if (denied != null) {
      return denied;
    }
    permissionEnforcer.requireAllowed(adminGovernanceReadAccessRequest(principal.portalRole()));
    return ResponseEntity.ok(ApiResponseEnvelope.success(observabilityReadService.searchReviewEvents(
        new ObservabilityReviewEventQuery(
            principal.organizationId(),
            targetEntityType,
            targetEntityId,
            status,
            claimLedgerItemId,
            reviewerUserId,
            createdFrom,
            createdTo,
            limit,
            offset))));
  }

  @GetMapping("/ai-task-runs")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> aiTaskRuns(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(required = false) String taskName,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String targetEntityType,
      @RequestParam(required = false) UUID targetEntityId,
      @RequestParam(required = false) UUID correlationId,
      @RequestParam(required = false) UUID causationId,
      @RequestParam(required = false) Instant startedFrom,
      @RequestParam(required = false) Instant startedTo,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> denied = denyUnlessAdmin(principal);
    if (denied != null) {
      return denied;
    }
    permissionEnforcer.requireAllowed(adminGovernanceReadAccessRequest(principal.portalRole()));
    return ResponseEntity.ok(ApiResponseEnvelope.success(observabilityReadService.searchAiTaskRuns(
        new ObservabilityAITaskRunQuery(
            principal.organizationId(),
            taskName,
            status,
            targetEntityType,
            targetEntityId,
            correlationId,
            causationId,
            startedFrom,
            startedTo,
            limit,
            offset))));
  }

  @GetMapping("/disclosures/{disclosureRecordRef}/audit-export")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> disclosureAuditExport(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @PathVariable String disclosureRecordRef) {
    ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> denied = denyUnlessAdmin(principal);
    if (denied != null) {
      return denied;
    }
    permissionEnforcer.requireAllowed(adminDisclosureExportAccessRequest(principal.portalRole()));
    return ResponseEntity.ok(ApiResponseEnvelope.success(observabilityReadService.disclosureAuditExport(
        new ObservabilityDisclosureAuditExportQuery(principal.organizationId(), disclosureRecordRef))));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            "admin_observability_failed",
            "request_failed",
            "Observability request failed.")));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> denyUnlessAdmin(
      RtoAuthenticatedPrincipal principal) {
    if (principal == null || (principal.portalRole() != PortalRole.ADMIN && principal.portalRole() != PortalRole.SYSTEM)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponseEnvelope.failure(new ApiAccessDeniedResponse(
              "admin_role_required",
              "admin_role_required",
              "Admin role is required for observability endpoints.").toErrorResponse()));
    }
    return null;
  }

  private static AccessRequest adminGovernanceReadAccessRequest(PortalRole portalRole) {
    return new AccessRequest(
        portalRole,
        ResourceType.ADMIN_GOVERNANCE,
        AccessAction.READ,
        FieldClassification.SYSTEM_GOVERNANCE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  private static AccessRequest adminDisclosureExportAccessRequest(PortalRole portalRole) {
    return new AccessRequest(
        portalRole,
        ResourceType.DISCLOSURE_RECORD,
        AccessAction.EXPORT,
        FieldClassification.SYSTEM_GOVERNANCE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }
}
