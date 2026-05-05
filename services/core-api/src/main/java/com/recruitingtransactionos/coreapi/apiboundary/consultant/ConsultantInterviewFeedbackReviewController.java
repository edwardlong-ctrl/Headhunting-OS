package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackReviewService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackSuggestionNotFoundException;
import java.util.List;
import java.util.Set;
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
@RequestMapping("/api/consultant/interview-feedback-suggestions")
public final class ConsultantInterviewFeedbackReviewController {

  private final InterviewFeedbackReviewService reviewService;

  public ConsultantInterviewFeedbackReviewController(InterviewFeedbackReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @GetMapping("/{suggestionId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getSuggestion(
      @PathVariable String suggestionId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(reviewService.getSuggestion(
        principal.organizationId(),
        new InterviewFeedbackSuggestionId(parseUuid(suggestionId)))));
  }

  @PostMapping("/{suggestionId}/review")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> reviewSuggestion(
      @PathVariable String suggestionId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ConsultantInterviewFeedbackReviewRequest request) {
    requireConsultantRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(reviewService.review(
        principal.organizationId(),
        principal.userAccountId(),
        new InterviewFeedbackSuggestionId(parseUuid(suggestionId)),
        request.decision(),
        request.note())));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponseEnvelope.failure(ApiAccessDeniedResponse.from(exception).toErrorResponse()));
  }

  @ExceptionHandler(InterviewFeedbackSuggestionNotFoundException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound(
      InterviewFeedbackSuggestionNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            "not_found",
            "interview_feedback_suggestion_unavailable",
            "Interview feedback suggestion is unavailable.")));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponseEnvelope.failure(ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request: " + exception.getMessage())).toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            "internal_error", "request_failed", "Request failed.")));
  }

  private static void requireConsultantRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CONSULTANT) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "consultant_role_required",
          "Consultant role is required for this endpoint."));
    }
  }

  private static UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid UUID format.");
    }
  }
}
