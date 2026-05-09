package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultant/workflow")
public final class ConsultantWorkflowController {

  private final ConsultantWorkflowSurfaceService workflowSurfaceService;

  public ConsultantWorkflowController(ConsultantWorkflowSurfaceService workflowSurfaceService) {
    this.workflowSurfaceService = Objects.requireNonNull(workflowSurfaceService, "workflowSurfaceService must not be null");
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> timeline(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String entityId,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    requireConsultantRole(principal.portalRole());
    UUID parsedEntityId = parseOptionalUuid(entityId);
    return ResponseEntity.ok(ApiResponseEnvelope.success(workflowSurfaceService.timeline(
        buildAccessRequest(), principal.organizationId(), entityType, parsedEntityId, limit, offset)));
  }

  @GetMapping("/audit")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> auditDrawer(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam String entityType,
      @RequestParam String entityId,
      @RequestParam(defaultValue = "10") int limit) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(workflowSurfaceService.auditDrawer(
        buildAccessRequest(), principal.organizationId(), entityType, parseUuid(entityId), limit)));
  }

  @GetMapping("/automation")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> automationQueue(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(defaultValue = "50") int limit) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(workflowSurfaceService.automationQueue(
        buildAccessRequest(), principal.organizationId(), limit, Instant.now())));
  }

  @GetMapping("/export")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> timelineExport(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String entityId,
      @RequestParam(defaultValue = "100") int limit) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(workflowSurfaceService.timelineExport(
        buildAccessRequest(), principal.organizationId(), entityType, parseOptionalUuid(entityId), limit, Instant.now())));
  }

  @GetMapping("/entity-state")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> entityState(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam String entityType,
      @RequestParam String entityId) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(workflowSurfaceService.entityState(
        buildAccessRequest(), principal.organizationId(), entityType, parseUuid(entityId))));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(ApiValidationErrorResponse.of(
            "invalid_request", List.of("Invalid request: " + exception.getMessage())).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            "internal_error", "request_failed", "Request failed.")));
  }

  private static void requireConsultantRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CONSULTANT) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "consultant_role_required",
          "Consultant role is required for this endpoint."));
    }
  }

  private static UUID parseOptionalUuid(String value) {
    return value == null || value.isBlank() ? null : parseUuid(value);
  }

  private static UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid UUID format.");
    }
  }

  private static AccessRequest buildAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.WORKFLOW_EVENT,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }
}
