package com.recruitingtransactionos.coreapi.apiboundary.candidate;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateConsentSummaryResponse;
import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService;
import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService.CandidateConsentView;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.util.List;
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
@RequestMapping("/api/candidate/consent")
public final class CandidateConsentController {

  private final CandidateConsentWorkflowService consentWorkflowService;

  public CandidateConsentController(CandidateConsentWorkflowService consentWorkflowService) {
    this.consentWorkflowService = consentWorkflowService;
  }

  @GetMapping("/{candidateRef}/{candidateProfileRef}/{jobRef}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> latestConsent(
      @PathVariable String candidateRef,
      @PathVariable String candidateProfileRef,
      @PathVariable String jobRef,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireCandidateRole(principal.portalRole());
    requireSelfCandidate(principal, candidateRef);
    CandidateConsentView view = consentWorkflowService.viewLatestConsent(
        principal.organizationId(),
        candidateRef,
        candidateProfileRef,
        jobRef,
        principal.userAccountId());
    return ResponseEntity.ok(ApiResponseEnvelope.success(toResponse(view, candidateRef, candidateProfileRef, jobRef)));
  }

  @PostMapping("/{candidateRef}/{candidateProfileRef}/{jobRef}/respond")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> respond(
      @PathVariable String candidateRef,
      @PathVariable String candidateProfileRef,
      @PathVariable String jobRef,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody CandidateConsentDecisionRequest request) {
    requireCandidateRole(principal.portalRole());
    requireSelfCandidate(principal, candidateRef);
    consentWorkflowService.respondToConsent(
        principal.organizationId(),
        principal.userAccountId(),
        candidateRef,
        candidateProfileRef,
        jobRef,
        request.approve());
    CandidateConsentView view = consentWorkflowService.viewLatestConsent(
        principal.organizationId(),
        candidateRef,
        candidateProfileRef,
        jobRef,
        principal.userAccountId());
    return ResponseEntity.ok(ApiResponseEnvelope.success(toResponse(view, candidateRef, candidateProfileRef, jobRef)));
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
            "internal_error",
            "request_failed",
            "Request failed.")));
  }

  private static CandidateConsentSummaryResponse toResponse(
      CandidateConsentView view,
      String candidateRef,
      String candidateProfileRef,
      String jobRef) {
    return new CandidateConsentSummaryResponse(
        candidateRef,
        candidateProfileRef,
        jobRef,
        view.jobTitle(),
        view.consentRecord().consentRecordRef(),
        view.consentRecord().status().wireValue(),
        view.consentRecord().consentTextVersion(),
        view.currentProfileVersion(),
        view.currentProfileVersion().equals(view.consentRecord().profileVersion()),
        view.consentRecord().revoked(),
        view.consentRecord().expiresAt() == null ? null : view.consentRecord().expiresAt().toString(),
        view.sharedFields().stream()
            .map(field -> new CandidateConsentSummaryResponse.SharedField(field.fieldPath(), field.jsonValue()))
            .toList());
  }

  private static void requireCandidateRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CANDIDATE) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "candidate_role_required",
          "Candidate role is required for this endpoint."));
    }
  }

  private static void requireSelfCandidate(RtoAuthenticatedPrincipal principal, String candidateRef) {
    if (!principal.userAccountId().toString().equals(candidateRef)) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "candidate_self_scope_required",
          "Candidate portal only supports self-scoped consent access."));
    }
  }

  public record CandidateConsentDecisionRequest(boolean approve) {}
}
