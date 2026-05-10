package com.recruitingtransactionos.coreapi.apiboundary.owner;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/revenue")
public final class OwnerRevenueController {

  private final OwnerRevenueQueryService queryService;

  public OwnerRevenueController(OwnerRevenueQueryService queryService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> loadRevenue(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireOwnerRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(queryService.load(revenueReadAccessRequest(), principal.organizationId())));
  }

  @GetMapping("/accounting-export")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> exportAccountingHandoff(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireOwnerRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(queryService.exportAccountingHandoff(revenueReadAccessRequest(), principal.organizationId())));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse("owner_revenue_failed", "request_failed", exception.getMessage())));
  }

  private static void requireOwnerRole(PortalRole portalRole) {
    if (portalRole != PortalRole.OWNER) {
      throw new AccessDeniedException(new AccessDecision(false, "owner_role_required", "Owner role is required for this endpoint."));
    }
  }

  private static AccessRequest revenueReadAccessRequest() {
    return new AccessRequest(PortalRole.OWNER, ResourceType.REVENUE_REPORT, AccessAction.READ, FieldClassification.INTERNAL, Set.of(RelationshipScope.SAME_ORGANIZATION), false);
  }
}
