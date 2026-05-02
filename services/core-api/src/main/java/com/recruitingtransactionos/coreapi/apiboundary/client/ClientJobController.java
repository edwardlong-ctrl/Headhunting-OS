package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/jobs")
public final class ClientJobController {

  private final ClientApiQueryService queryService;
  private final ClientApiCommandService commandService;

  public ClientJobController(
      ClientApiQueryService queryService,
      ClientApiCommandService commandService) {
    this.queryService = queryService;
    this.commandService = commandService;
  }

  @GetMapping("/{jobId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getJobStatus(
      @PathVariable String jobId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireClientRole(principal.portalRole());
    return queryService.getJobStatus(
            buildAccessRequest(AccessAction.READ),
            principal.organizationId(),
            principal.userAccountId(),
            parseJobId(jobId))
        .map(response -> ResponseEntity.ok(ApiResponseEnvelope.<ApiSafeResponseBody>success(response)))
        .orElseGet(ClientJobController::notFound);
  }

  @PostMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> createJobSubmission(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ClientJobIntakeCreateRequest request) {
    requireClientRole(principal.portalRole());
    var response = commandService.createJobSubmission(
        buildAccessRequest(AccessAction.CREATE),
        principal.organizationId(),
        principal.userAccountId(),
        request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseEnvelope.success(response));
  }

  @PostMapping("/{jobId}/clarification")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> answerClarification(
      @PathVariable String jobId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ClientJobClarificationRequest request) {
    requireClientRole(principal.portalRole());
    var response = commandService.answerClarification(
        buildAccessRequest(AccessAction.UPDATE),
        principal.organizationId(),
        principal.userAccountId(),
        parseJobId(jobId),
        request);
    return ResponseEntity.ok(ApiResponseEnvelope.success(response));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(HttpStatus.FORBIDDEN, ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      Exception exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of("invalid_request", List.of("Invalid request.")).toErrorResponse());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> unreadablePayload(
      HttpMessageNotReadableException exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of("invalid_request", List.of("Invalid request body."))
            .toErrorResponse());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(
      RuntimeException exception) {
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        new ApiErrorResponse("internal_error", "request_failed", "Request failed."));
  }

  private static void requireClientRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CLIENT) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "client_role_required",
          "Client role is required for this endpoint."));
    }
  }

  private static AccessRequest buildAccessRequest(AccessAction action) {
    return new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.JOB,
        action,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
        false);
  }

  private static JobId parseJobId(String jobId) {
    try {
      return new JobId(UUID.fromString(jobId));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("invalid_job_id");
    }
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound() {
    return error(
        HttpStatus.NOT_FOUND,
        new ApiErrorResponse("not_found", "job_submission_unavailable", "Job submission is unavailable."));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(
      HttpStatus status,
      ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }
}
