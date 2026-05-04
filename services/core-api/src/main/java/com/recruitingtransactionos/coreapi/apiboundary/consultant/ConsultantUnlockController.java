package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateConsentSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantUnlockDecisionResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantUnlockQueueResponse;
import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/consultant/unlock-requests")
public final class ConsultantUnlockController {

  private final UnlockWorkflowService unlockWorkflowService;
  private final CandidateConsentWorkflowService candidateConsentWorkflowService;

  public ConsultantUnlockController(
      UnlockWorkflowService unlockWorkflowService,
      CandidateConsentWorkflowService candidateConsentWorkflowService) {
    this.unlockWorkflowService = unlockWorkflowService;
    this.candidateConsentWorkflowService = candidateConsentWorkflowService;
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listPending(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireConsultantRole(principal.portalRole());
    List<ConsultantUnlockQueueResponse.Item> items = unlockWorkflowService.listPendingRequests(
            principal.organizationId(),
            principal.userAccountId())
        .stream()
        .map(item -> new ConsultantUnlockQueueResponse.Item(
            item.unlockRequestId(),
            item.shortlistId(),
            item.shortlistCandidateCardId(),
            item.status(),
            item.requestReason(),
            item.createdAt().toString(),
            item.anonymousCandidateCardRef(),
            item.jobTitle(),
            item.clientCompanyName(),
            item.consentStatus(),
            item.blockers().stream()
                .map(blocker -> new ConsultantUnlockQueueResponse.Blocker(blocker.code(), blocker.message()))
                .toList()))
        .toList();
    return ResponseEntity.ok(ApiResponseEnvelope.success(new ConsultantUnlockQueueResponse(items)));
  }

  @PostMapping("/consent-requests")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestConsent(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ConsultantConsentRequest request) {
    requireConsultantRole(principal.portalRole());
    var consent = candidateConsentWorkflowService.requestConsent(
        principal.organizationId(),
        principal.userAccountId(),
        request.candidateRef(),
        request.candidateProfileRef(),
        request.jobRef(),
        request.consentTextVersion(),
        request.expiresAt() == null ? Instant.now().plusSeconds(60L * 60L * 24L * 14L) : Instant.parse(request.expiresAt()));
    var view = candidateConsentWorkflowService.latestConsentSnapshot(
        principal.organizationId(),
        request.candidateRef(),
        request.candidateProfileRef(),
        request.jobRef());
    CandidateConsentSummaryResponse response = new CandidateConsentSummaryResponse(
        request.candidateRef(),
        request.candidateProfileRef(),
        request.jobRef(),
        view.jobTitle(),
        consent.consentRecordRef(),
        view.consentRecord().status().wireValue(),
        view.consentRecord().consentTextVersion(),
        view.currentProfileVersion(),
        view.currentProfileVersion().equals(view.consentRecord().profileVersion()),
        view.consentRecord().revoked(),
        view.consentRecord().expiresAt() == null ? null : view.consentRecord().expiresAt().toString(),
        view.sharedFields().stream()
            .map(field -> new CandidateConsentSummaryResponse.SharedField(field.fieldPath(), field.jsonValue()))
            .toList());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseEnvelope.success(response));
  }

  @PostMapping("/{shortlistId}/{cardId}/approve")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> approve(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @PathVariable String shortlistId,
      @PathVariable String cardId,
      @RequestBody ConsultantUnlockDecisionRequest request) {
    requireConsultantRole(principal.portalRole());
    var result = unlockWorkflowService.approveRequest(
        principal.organizationId(),
        principal.userAccountId(),
        new ShortlistId(UUID.fromString(shortlistId)),
        new ShortlistCandidateCardId(UUID.fromString(cardId)),
        request.reason());
    return ResponseEntity.ok(ApiResponseEnvelope.success(new ConsultantUnlockDecisionResponse(
        result.unlockRequest() == null ? null : result.unlockRequest().clientUnlockRequestId().value().toString(),
        result.unlockRequest() == null ? "blocked" : result.unlockRequest().status().wireValue(),
        result.unlockRequest() == null ? null : result.unlockRequest().unlockDecisionRef(),
        result.unlockRequest() == null ? null : result.unlockRequest().approvedDisclosureRecordRef(),
        result.blockers().stream()
            .map(blocker -> new ConsultantUnlockQueueResponse.Blocker(blocker.code(), blocker.message()))
            .toList())));
  }

  @PostMapping("/{shortlistId}/{cardId}/reject")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> reject(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @PathVariable String shortlistId,
      @PathVariable String cardId,
      @RequestBody ConsultantUnlockDecisionRequest request) {
    requireConsultantRole(principal.portalRole());
    var result = unlockWorkflowService.rejectRequest(
        principal.organizationId(),
        principal.userAccountId(),
        new ShortlistId(UUID.fromString(shortlistId)),
        new ShortlistCandidateCardId(UUID.fromString(cardId)),
        request.reason());
    return ResponseEntity.ok(ApiResponseEnvelope.success(new ConsultantUnlockDecisionResponse(
        result.unlockRequest() == null ? null : result.unlockRequest().clientUnlockRequestId().value().toString(),
        result.unlockRequest() == null ? "blocked" : result.unlockRequest().status().wireValue(),
        result.unlockRequest() == null ? null : result.unlockRequest().unlockDecisionRef(),
        result.unlockRequest() == null ? null : result.unlockRequest().approvedDisclosureRecordRef(),
        result.blockers().stream()
            .map(blocker -> new ConsultantUnlockQueueResponse.Blocker(blocker.code(), blocker.message()))
            .toList())));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(
            ApiValidationErrorResponse.of("invalid_request", List.of(exception.getMessage())).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponseEnvelope.failure(
            new ApiErrorResponse("internal_error", "request_failed", "Request failed.")));
  }

  private static void requireConsultantRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CONSULTANT) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "consultant_role_required",
          "Consultant role is required for this endpoint."));
    }
  }

  public record ConsultantUnlockDecisionRequest(String reason) {}

  public record ConsultantConsentRequest(
      String candidateRef,
      String candidateProfileRef,
      String jobRef,
      String consentTextVersion,
      String expiresAt) {}
}
