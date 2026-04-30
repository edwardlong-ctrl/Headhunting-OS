package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantJobDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantJobSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultant/jobs")
public final class ConsultantJobController {

  private static final String ACTOR_ROLE_HEADER = "X-RTO-Actor-Role";
  private static final String ORGANIZATION_ID_HEADER = "X-RTO-Organization-Id";

  private final ConsultantApiQueryService queryService;
  private final ConsultantApiCommandService commandService;

  public ConsultantJobController(
      ConsultantApiQueryService queryService,
      ConsultantApiCommandService commandService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
    this.commandService = Objects.requireNonNull(commandService, "commandService must not be null");
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listJobs(
      @RequestHeader(name = ACTOR_ROLE_HEADER, required = false) String actorRole,
      @RequestHeader(name = ORGANIZATION_ID_HEADER, required = false) String organizationId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String companyId,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {

    requireConsultantRole(actorRole);
    UUID orgId = parseOrganizationId(organizationId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.JOB, AccessAction.READ);
    PagedQuery pagedQuery = PagedQuery.builder(orgId).limit(limit).offset(offset).build();

    PagedResult<ConsultantJobSummaryResponse> result =
        queryService.listJobs(accessRequest, pagedQuery, status, companyId);
    return ResponseEntity.ok(ApiResponseEnvelope.success(result));
  }

  @GetMapping("/{jobId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getJob(
      @PathVariable String jobId,
      @RequestHeader(name = ACTOR_ROLE_HEADER, required = false) String actorRole,
      @RequestHeader(name = ORGANIZATION_ID_HEADER, required = false) String organizationId) {

    requireConsultantRole(actorRole);
    UUID orgId = parseOrganizationId(organizationId);
    JobId jid = parseJobId(jobId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.JOB, AccessAction.READ);

    return queryService.getJobDetail(accessRequest, orgId, jid)
        .map(response -> ResponseEntity.ok(
            ApiResponseEnvelope.<ApiSafeResponseBody>success(response)))
        .orElseGet(ConsultantJobController::notFound);
  }

  @PostMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> createJob(
      @RequestHeader(name = ACTOR_ROLE_HEADER, required = false) String actorRole,
      @RequestHeader(name = ORGANIZATION_ID_HEADER, required = false) String organizationId,
      @RequestBody JobCreateRequest request) {

    requireConsultantRole(actorRole);
    UUID orgId = parseOrganizationId(organizationId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.JOB, AccessAction.CREATE);

    ConsultantJobDetailResponse result =
        commandService.createJob(accessRequest, orgId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponseEnvelope.success(result));
  }

  @PutMapping("/{jobId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> updateJob(
      @PathVariable String jobId,
      @RequestHeader(name = ACTOR_ROLE_HEADER, required = false) String actorRole,
      @RequestHeader(name = ORGANIZATION_ID_HEADER, required = false) String organizationId,
      @RequestBody JobUpdateRequest request) {

    requireConsultantRole(actorRole);
    UUID orgId = parseOrganizationId(organizationId);
    JobId jid = parseJobId(jobId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.JOB, AccessAction.UPDATE);

    ConsultantJobDetailResponse result =
        commandService.updateJob(accessRequest, orgId, jid, request);
    return ResponseEntity.ok(ApiResponseEnvelope.success(result));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(
        HttpStatus.FORBIDDEN,
        ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      Exception exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request: " + exception.getMessage()))
            .toErrorResponse());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> unreadablePayload(
      HttpMessageNotReadableException exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request body."))
            .toErrorResponse());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(
      RuntimeException exception) {
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        new ApiErrorResponse(
            "internal_error",
            "request_failed",
            "Request failed."));
  }

  private static void requireConsultantRole(String actorRole) {
    if (actorRole == null || !PortalRole.CONSULTANT.wireValue().equals(actorRole.strip())) {
      throw new AccessDeniedException(
          new AccessDecision(false,
              "consultant_role_required",
              "Consultant role is required for this endpoint."));
    }
  }

  private static UUID parseOrganizationId(String organizationId) {
    if (organizationId == null || organizationId.isBlank()) {
      throw new IllegalArgumentException("X-RTO-Organization-Id header is required.");
    }
    try {
      return UUID.fromString(organizationId.strip());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid organization ID format.");
    }
  }

  private static JobId parseJobId(String jobId) {
    if (jobId == null || jobId.isBlank()) {
      throw new IllegalArgumentException("jobId must not be blank");
    }
    try {
      return new JobId(UUID.fromString(jobId));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid job ID format.");
    }
  }

  private static AccessRequest buildAccessRequest(ResourceType resourceType, AccessAction action) {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        resourceType,
        action,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound() {
    return error(
        HttpStatus.NOT_FOUND,
        new ApiErrorResponse(
            "not_found",
            "job_unavailable",
            "Job is unavailable."));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(
      HttpStatus status, ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }
}
