package com.recruitingtransactionos.coreapi.apiboundary.owner;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner")
public final class OwnerPlacementController {

  private final OwnerPlacementQueryService queryService;

  public OwnerPlacementController(OwnerPlacementQueryService queryService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
  }

  @GetMapping("/placements")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listPlacements(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    requireOwnerRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(queryService.listPlacements(
        placementReadAccessRequest(), PagedQuery.builder(principal.organizationId()).limit(limit).offset(offset).build())));
  }

  @GetMapping("/commission")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listCommissions(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    requireOwnerRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(queryService.listCommissions(
        commissionReadAccessRequest(), PagedQuery.builder(principal.organizationId()).limit(limit).offset(offset).build())));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse("owner_request_failed", "request_failed", exception.getMessage())));
  }

  private static void requireOwnerRole(PortalRole portalRole) {
    if (portalRole != PortalRole.OWNER) {
      throw new AccessDeniedException(new AccessDecision(false, "owner_role_required", "Owner role is required for this endpoint."));
    }
  }

  private static AccessRequest placementReadAccessRequest() {
    return new AccessRequest(PortalRole.OWNER, ResourceType.PLACEMENT, AccessAction.READ, FieldClassification.INTERNAL, Set.of(RelationshipScope.SAME_ORGANIZATION), false);
  }

  private static AccessRequest commissionReadAccessRequest() {
    return new AccessRequest(PortalRole.OWNER, ResourceType.COMMISSION, AccessAction.READ, FieldClassification.INTERNAL, Set.of(RelationshipScope.SAME_ORGANIZATION), false);
  }
}
