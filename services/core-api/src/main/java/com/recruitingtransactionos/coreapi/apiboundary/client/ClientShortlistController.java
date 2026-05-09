package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientShortlistSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
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
@RequestMapping("/api/client/shortlists")
public final class ClientShortlistController {

  private final ClientApiQueryService queryService;
  private final ClientApiCommandService commandService;

  public ClientShortlistController(
      ClientApiQueryService queryService,
      ClientApiCommandService commandService) {
    this.queryService = queryService;
    this.commandService = commandService;
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listShortlists(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireClientRole(principal.portalRole());
    List<ClientShortlistSummaryResponse> items =
        queryService.listShortlists(
            buildAccessRequest(AccessAction.READ),
            principal.organizationId(),
            principal.userAccountId());
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        PagedResult.of(items, items.size(), Math.max(PagedQuery.DEFAULT_LIMIT, items.size()), 0)));
  }

  @GetMapping("/{shortlistId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getShortlist(
      @PathVariable String shortlistId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireClientRole(principal.portalRole());
    return queryService.getShortlistDetail(
            buildAccessRequest(AccessAction.READ),
            principal.organizationId(),
            principal.userAccountId(),
            parseShortlistId(shortlistId))
        .map(response -> ResponseEntity.ok(ApiResponseEnvelope.<ApiSafeResponseBody>success(response)))
        .orElseGet(ClientShortlistController::notFound);
  }

  @PostMapping("/{shortlistId}/view")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> markViewed(
      @PathVariable String shortlistId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireClientRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(commandService.viewShortlist(
        buildAccessRequest(AccessAction.UPDATE),
        principal.organizationId(),
        principal.userAccountId(),
        parseShortlistId(shortlistId))));
  }

  @PostMapping("/{shortlistId}/cards/{cardId}/select")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> selectCandidate(
      @PathVariable String shortlistId,
      @PathVariable String cardId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    requireClientRole(principal.portalRole());
    return ResponseEntity.ok(ApiResponseEnvelope.success(commandService.selectShortlistCandidate(
        buildAccessRequest(AccessAction.UPDATE),
        principal.organizationId(),
        principal.userAccountId(),
        parseShortlistId(shortlistId),
        parseShortlistCandidateCardId(cardId))));
  }

  @PostMapping("/{shortlistId}/cards/{cardId}/unlock-requests")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> createUnlockRequest(
      @PathVariable String shortlistId,
      @PathVariable String cardId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ClientUnlockRequestCreateRequest request) {
    requireClientRole(principal.portalRole());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseEnvelope.success(
        commandService.createUnlockRequest(
            buildAccessRequest(AccessAction.UPDATE),
            principal.organizationId(),
            principal.userAccountId(),
            parseShortlistId(shortlistId),
            parseShortlistCandidateCardId(cardId),
            request)));
  }

  @PostMapping("/{shortlistId}/cards/{cardId}/feedback")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> submitFeedback(
      @PathVariable String shortlistId,
      @PathVariable String cardId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ClientInterviewFeedbackRequest request) {
    requireClientRole(principal.portalRole());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseEnvelope.success(
        commandService.submitInterviewFeedback(
            buildAccessRequest(AccessAction.UPDATE),
            principal.organizationId(),
            principal.userAccountId(),
            parseShortlistId(shortlistId),
            parseShortlistCandidateCardId(cardId),
            request)));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(HttpStatus.FORBIDDEN, ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(Exception exception) {
    return error(HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of("invalid_request", List.of("Invalid request: " + exception.getMessage())).toErrorResponse());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> unreadablePayload(
      HttpMessageNotReadableException exception) {
    return error(HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of("invalid_request", List.of("Invalid request body.")).toErrorResponse());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(RuntimeException exception) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR,
        new ApiErrorResponse("internal_error", "request_failed", "Request failed."));
  }

  private static void requireClientRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CLIENT) {
      throw new AccessDeniedException(new AccessDecision(false, "client_role_required", "Client role is required for this endpoint."));
    }
  }

  private static AccessRequest buildAccessRequest(AccessAction action) {
    return new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.SHORTLIST,
        action,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
        false);
  }

  private static ShortlistId parseShortlistId(String shortlistId) {
    return new ShortlistId(UUID.fromString(shortlistId));
  }

  private static ShortlistCandidateCardId parseShortlistCandidateCardId(String cardId) {
    return new ShortlistCandidateCardId(UUID.fromString(cardId));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound() {
    return error(HttpStatus.NOT_FOUND,
        new ApiErrorResponse("not_found", "shortlist_unavailable", "Shortlist is unavailable."));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(HttpStatus status, ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }
}
