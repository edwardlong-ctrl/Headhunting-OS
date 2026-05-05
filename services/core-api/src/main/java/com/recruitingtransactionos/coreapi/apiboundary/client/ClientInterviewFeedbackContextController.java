package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/interviews")
public final class ClientInterviewFeedbackContextController {

  private final ClientApiQueryService queryService;

  public ClientInterviewFeedbackContextController(ClientApiQueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping("/{interviewId}/feedback")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getFeedbackContext(
      @PathVariable String interviewId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireClientRole(principal.portalRole());
    return queryService.getInterviewFeedbackContext(
            buildAccessRequest(),
            principal.organizationId(),
            principal.userAccountId(),
            new CandidateCompanyInteractionId(UUID.fromString(interviewId)))
        .map(response -> ResponseEntity.ok(ApiResponseEnvelope.<ApiSafeResponseBody>success(response)))
        .orElseGet(ClientInterviewFeedbackContextController::notFound);
  }

  private static void requireClientRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CLIENT) {
      throw new AccessDeniedException(new AccessDecision(false, "client_role_required", "Client role is required for this endpoint."));
    }
  }

  private static AccessRequest buildAccessRequest() {
    return new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.SHORTLIST,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
        false);
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            "not_found",
            "interview_feedback_context_unavailable",
            "Interview feedback context is unavailable.")));
  }
}
