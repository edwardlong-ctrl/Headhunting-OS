package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportListResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse;
import com.recruitingtransactionos.coreapi.consultantmatching.ConsultantMatchingSurfaceService;
import com.recruitingtransactionos.coreapi.consultantmatching.ConsultantMatchingSurfaceService.ConsultantMatchSelection;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultant/jobs/{jobId}/matching")
public final class ConsultantMatchingController {

  private final ConsultantMatchingSurfaceService consultantMatchingSurfaceService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantMatchingController(
      ConsultantMatchingSurfaceService consultantMatchingSurfaceService) {
    this(
        consultantMatchingSurfaceService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  private ConsultantMatchingController(
      ConsultantMatchingSurfaceService consultantMatchingSurfaceService,
      PermissionEnforcer permissionEnforcer) {
    this.consultantMatchingSurfaceService = consultantMatchingSurfaceService;
    this.permissionEnforcer = permissionEnforcer;
  }

  @PostMapping("/generate")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> generateMatchReport(
      @PathVariable String jobId,
      @RequestBody ConsultantMatchGenerationRequest request,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    AccessRequest accessRequest = requireAccess(principal.portalRole(), AccessAction.CREATE);

    try {
      ConsultantMatchReportResponse response = consultantMatchingSurfaceService.generateMatchReport(
          accessRequest,
          principal.organizationId(),
          new JobId(java.util.UUID.fromString(jobId)),
          new ConsultantMatchSelection(request.candidateId(), request.shortlistCandidateCardId()));

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponseEnvelope.success(response));
    } catch (IllegalArgumentException e) {
      return error(
          HttpStatus.BAD_REQUEST,
          ApiValidationErrorResponse.of("invalid_request", List.of(e.getMessage())).toErrorResponse());
    }
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listMatchReports(
      @PathVariable String jobId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    AccessRequest accessRequest = requireAccess(principal.portalRole(), AccessAction.READ);
    try {
      List<ConsultantMatchReportResponse> response = consultantMatchingSurfaceService.listMatchReports(
          accessRequest,
          principal.organizationId(),
          new JobId(java.util.UUID.fromString(jobId)));
      return ResponseEntity.ok(ApiResponseEnvelope.success(new ConsultantMatchReportListResponse(response)));
    } catch (IllegalArgumentException e) {
      return error(
          HttpStatus.BAD_REQUEST,
          ApiValidationErrorResponse.of("invalid_request", List.of(e.getMessage())).toErrorResponse());
    }
  }

  private AccessRequest requireAccess(PortalRole portalRole, AccessAction action) {
    AccessRequest accessRequest = new AccessRequest(
        portalRole,
        ResourceType.MATCH_REPORT,
        action,
        FieldClassification.CONSULTANT_PRIVATE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
    permissionEnforcer.requireAllowed(accessRequest);
    return accessRequest;
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(
      HttpStatus status, com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(
        HttpStatus.FORBIDDEN,
        com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }
}

record ConsultantMatchGenerationRequest(
    String candidateId,
    String shortlistCandidateCardId
) {}
