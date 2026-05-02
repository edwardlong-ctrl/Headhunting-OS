package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
@RequestMapping("/api/consultant/candidates")
public final class ConsultantCandidateController {

  private final ConsultantCandidateQueryService queryService;

  public ConsultantCandidateController(ConsultantCandidateQueryService queryService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listCandidates(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    requireConsultantRole(principal.portalRole());
    PagedQuery pagedQuery = PagedQuery.builder(principal.organizationId()).limit(limit).offset(offset).build();
    return ResponseEntity.ok(ApiResponseEnvelope.success(queryService.listCandidates(
        buildAccessRequest(AccessAction.READ), pagedQuery, status)));
  }

  @GetMapping("/{candidateRef}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getCandidate(
      @PathVariable String candidateRef,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireConsultantRole(principal.portalRole());
    return queryService.getCandidateDetail(
            buildAccessRequest(AccessAction.READ),
            principal.organizationId(),
            parseCandidateRef(candidateRef))
        .map(response -> ResponseEntity.ok(ApiResponseEnvelope.<ApiSafeResponseBody>success(response)))
        .orElseGet(ConsultantCandidateController::notFound);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(AccessDeniedException exception) {
    return error(HttpStatus.FORBIDDEN, ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(IllegalArgumentException exception) {
    return error(HttpStatus.BAD_REQUEST, ApiValidationErrorResponse.of(
        "invalid_request", List.of("Invalid request: " + exception.getMessage())).toErrorResponse());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, new ApiErrorResponse(
        "internal_error", "request_failed", "Request failed."));
  }

  private static CandidateId parseCandidateRef(String candidateRef) {
    if (candidateRef == null || candidateRef.isBlank()) {
      throw new IllegalArgumentException("candidateRef must not be blank");
    }
    try {
      return new CandidateId(UUID.fromString(candidateRef));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid candidate ID format.");
    }
  }

  private static void requireConsultantRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CONSULTANT) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "consultant_role_required",
          "Consultant role is required for this endpoint."));
    }
  }

  private static AccessRequest buildAccessRequest(AccessAction action) {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.WORKFLOW_EVENT,
        action,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound() {
    return error(HttpStatus.NOT_FOUND, new ApiErrorResponse(
        "not_found", "candidate_unavailable", "Candidate is unavailable."));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(HttpStatus status, ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }
}
