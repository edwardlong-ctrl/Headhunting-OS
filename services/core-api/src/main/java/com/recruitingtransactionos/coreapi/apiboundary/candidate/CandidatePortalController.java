package com.recruitingtransactionos.coreapi.apiboundary.candidate;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidate")
public final class CandidatePortalController {

  private final CandidatePortalQueryService portalQueryService;

  public CandidatePortalController(CandidatePortalQueryService portalQueryService) {
    this.portalQueryService = Objects.requireNonNull(
        portalQueryService, "portalQueryService must not be null");
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> me(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireCandidateRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        portalQueryService.buildMe(principal.organizationId(), principal.userAccountId())));
  }

  @GetMapping("/profile/{candidateRef}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> profile(
      @PathVariable String candidateRef,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireCandidateRole(principal.portalRole());
    requireSelfCandidate(principal, candidateRef);
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        portalQueryService.buildProfileReview(principal.organizationId(), candidateRef)));
  }

  @GetMapping("/documents")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> documents(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    requireCandidateRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        portalQueryService.listDocuments(principal.organizationId(), principal.userAccountId(), limit, offset)));
  }

  @GetMapping("/opportunities")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> opportunities(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    requireCandidateRole(principal.portalRole());
    List<com.recruitingtransactionos.coreapi.apiboundary.CandidateOpportunityResponse> items =
        portalQueryService.listOpportunities(principal.organizationId(), principal.userAccountId());
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        com.recruitingtransactionos.coreapi.apiboundary.PagedResult.of(items, items.size(), Math.max(1, limit), Math.max(0, offset))));
  }

  @GetMapping("/timeline/{candidateRef}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> timeline(
      @PathVariable String candidateRef,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireCandidateRole(principal.portalRole());
    requireSelfCandidate(principal, candidateRef);
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        portalQueryService.buildTimeline(principal.organizationId(), candidateRef)));
  }

  @PostMapping("/documents/upload")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> uploadDocument(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "documentType", defaultValue = "resume") String documentType) {
    requireCandidateRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        portalQueryService.uploadDocument(principal.organizationId(), principal.userAccountId(), file, documentType)));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(
            ApiValidationErrorResponse.of("invalid_request", List.of(exception.getMessage())).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            "internal_error", "request_failed", "Request failed.")));
  }

  private static void requireCandidateRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CANDIDATE) {
      throw new AccessDeniedException(new AccessDecision(
          false, "candidate_role_required", "Candidate role is required for this endpoint."));
    }
  }

  private static void requireSelfCandidate(RtoAuthenticatedPrincipal principal, String candidateRef) {
    if (!principal.userAccountId().toString().equals(candidateRef)) {
      throw new AccessDeniedException(new AccessDecision(
          false, "candidate_self_scope_required", "Candidate portal only supports self-scoped access."));
    }
  }
}
