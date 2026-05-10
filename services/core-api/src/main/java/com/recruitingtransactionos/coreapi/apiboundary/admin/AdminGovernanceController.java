package com.recruitingtransactionos.coreapi.apiboundary.admin;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceConfigUpdateResponse;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigRecord;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public final class AdminGovernanceController {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final GovernanceReadService governanceReadService;
  private final GovernanceConsoleReadService governanceConsoleReadService;
  private final GovernanceConfigService governanceConfigService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public AdminGovernanceController(
      GovernanceReadService governanceReadService,
      GovernanceConfigService governanceConfigService,
      GovernanceConsoleReadService governanceConsoleReadService) {
    this(
        governanceReadService,
        governanceConfigService,
        governanceConsoleReadService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  AdminGovernanceController(
      GovernanceReadService governanceReadService,
      GovernanceConfigService governanceConfigService,
      PermissionEnforcer permissionEnforcer) {
    this(governanceReadService, governanceConfigService, null, permissionEnforcer);
  }

  AdminGovernanceController(
      GovernanceReadService governanceReadService,
      GovernanceConfigService governanceConfigService,
      GovernanceConsoleReadService governanceConsoleReadService,
      PermissionEnforcer permissionEnforcer) {
    this.governanceReadService = Objects.requireNonNull(
        governanceReadService,
        "governanceReadService must not be null");
    this.governanceConsoleReadService = governanceConsoleReadService;
    this.governanceConfigService = Objects.requireNonNull(
        governanceConfigService,
        "governanceConfigService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  @GetMapping({
      "/review-quality",
      "/claim-ledger",
      "/ontology-governance",
      "/privacy-redaction",
      "/model-routing",
      "/eval-feedback",
      "/eval-dashboard",
      "/negative-cases",
      "/cost-latency",
      "/ontology-drift",
      "/redaction-incidents",
      "/ai-resume-authenticity-risk",
      "/ai-policy",
      "/ai-task-registry",
      "/industry-packs",
      "/schema",
      "/workflow-rules",
      "/permissions",
      "/audit-log",
      "/integrations",
      "/security"
  })
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> loadSection(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      jakarta.servlet.http.HttpServletRequest request) {
    requireAdminRole(principal.portalRole());
    permissionEnforcer.requireAllowed(governanceAccessRequest(principal.portalRole(), AccessAction.READ));
    String sectionKey = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
    if (governanceConsoleReadService != null && GovernanceConsoleReadService.isTask50AdminSection(sectionKey)) {
      return ResponseEntity.ok(ApiResponseEnvelope.success(
          governanceConsoleReadService.loadAdminSection(principal.organizationId(), sectionKey)));
    }
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        governanceReadService.loadAdminSection(principal.organizationId(), sectionKey)));
  }

  @PutMapping({
      "/model-routing"
  })
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> saveSectionConfig(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      jakarta.servlet.http.HttpServletRequest request,
      @RequestBody AdminGovernanceConfigRequest requestBody) {
    requireAdminRole(principal.portalRole());
    permissionEnforcer.requireAllowed(governanceAccessRequest(principal.portalRole(), AccessAction.UPDATE));
    String sectionKey = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
    GovernanceConfigRecord record = governanceConfigService.save(
        principal.organizationId(),
        sectionKey,
        "default",
        requestBody == null ? "{}" : requestBody.payloadJson(),
        requestBody == null || requestBody.enabled() == null ? true : requestBody.enabled(),
        principal.userAccountId(),
        principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(new GovernanceConfigUpdateResponse(
        sectionKey,
        record.enabled() ? "saved" : "disabled",
        TIMESTAMP_FORMATTER.format(OffsetDateTime.ofInstant(record.updatedAt(), ZoneOffset.UTC)))));
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
            "admin_governance_failed",
            "request_failed",
            exception.getMessage())));
  }

  private static void requireAdminRole(PortalRole portalRole) {
    if (portalRole != PortalRole.ADMIN && portalRole != PortalRole.SYSTEM) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "admin_role_required",
          "Admin role is required for this endpoint."));
    }
  }

  private static AccessRequest governanceAccessRequest(PortalRole portalRole, AccessAction action) {
    return new AccessRequest(
        portalRole,
        ResourceType.ADMIN_GOVERNANCE,
        action,
        FieldClassification.SYSTEM_GOVERNANCE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }
}
