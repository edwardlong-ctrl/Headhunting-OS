package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientDashboardResponse;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/dashboard")
public final class ClientDashboardController {

  private final ClientApiQueryService queryService;

  public ClientDashboardController(ClientApiQueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getDashboard(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireClientRole(principal.portalRole());
    ClientDashboardResponse response = queryService.getDashboard(
        buildAccessRequest(),
        principal.organizationId(),
        principal.userAccountId());
    return ResponseEntity.ok(ApiResponseEnvelope.success(response));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(HttpStatus.FORBIDDEN, ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(Exception exception) {
    return error(HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of("invalid_request", List.of("Invalid request.")).toErrorResponse());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR,
        new ApiErrorResponse("internal_error", "request_failed", "Request failed."));
  }

  private static void requireClientRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CLIENT) {
      throw new AccessDeniedException(new AccessDecision(false, "client_role_required", "Client role is required for this endpoint."));
    }
  }

  private static AccessRequest buildAccessRequest() {
    return new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.SHORTLIST,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
        false);
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(HttpStatus status, ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }
}
