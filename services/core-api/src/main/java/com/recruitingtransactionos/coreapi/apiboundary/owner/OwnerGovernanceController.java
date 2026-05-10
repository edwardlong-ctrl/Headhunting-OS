package com.recruitingtransactionos.coreapi.apiboundary.owner;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.governanceconsole.GovernanceConsoleReadService;
import com.recruitingtransactionos.coreapi.governancequery.GovernanceReadService;
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
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner")
public final class OwnerGovernanceController {

  private final GovernanceReadService governanceReadService;
  private final GovernanceConsoleReadService governanceConsoleReadService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public OwnerGovernanceController(
      GovernanceReadService governanceReadService,
      GovernanceConsoleReadService governanceConsoleReadService) {
    this(
        governanceReadService,
        governanceConsoleReadService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  OwnerGovernanceController(
      GovernanceReadService governanceReadService,
      PermissionEnforcer permissionEnforcer) {
    this(governanceReadService, null, permissionEnforcer);
  }

  OwnerGovernanceController(
      GovernanceReadService governanceReadService,
      GovernanceConsoleReadService governanceConsoleReadService,
      PermissionEnforcer permissionEnforcer) {
    this.governanceReadService = Objects.requireNonNull(
        governanceReadService,
        "governanceReadService must not be null");
    this.governanceConsoleReadService = governanceConsoleReadService;
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  @GetMapping({"/dashboard", "/pipeline", "/consultants", "/clients", "/risk", "/data-quality", "/ai-quality", "/audit"})
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> loadSection(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      jakarta.servlet.http.HttpServletRequest request) {
    requireOwnerRole(principal.portalRole());
    permissionEnforcer.requireAllowed(ownerGovernanceReadAccessRequest());
    String sectionKey = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
    if ("ai-quality".equals(sectionKey) && governanceConsoleReadService != null) {
      return ResponseEntity.ok(ApiResponseEnvelope.success(
          governanceConsoleReadService.loadOwnerSummary(principal.organizationId())));
    }
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        governanceReadService.loadOwnerSection(principal.organizationId(), sectionKey)));
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
            "owner_governance_failed",
            "request_failed",
            exception.getMessage())));
  }

  private static void requireOwnerRole(PortalRole portalRole) {
    if (portalRole != PortalRole.OWNER) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "owner_role_required",
          "Owner role is required for this endpoint."));
    }
  }

  private static AccessRequest ownerGovernanceReadAccessRequest() {
    return new AccessRequest(
        PortalRole.OWNER,
        ResourceType.ADMIN_GOVERNANCE,
        AccessAction.READ,
        FieldClassification.SYSTEM_GOVERNANCE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }
}
