package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiAccessDeniedResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistSummaryResponse;
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
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultant/shortlists")
public final class ConsultantShortlistController {


  private final ConsultantApiQueryService queryService;
  private final ConsultantApiCommandService commandService;

  public ConsultantShortlistController(
      ConsultantApiQueryService queryService,
      ConsultantApiCommandService commandService) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
    this.commandService = Objects.requireNonNull(commandService, "commandService must not be null");
  }

  @GetMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> listShortlists(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestParam(required = false) String jobId,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    AccessRequest accessRequest = buildAccessRequest(ResourceType.SHORTLIST);
    PagedQuery pagedQuery = PagedQuery.builder(orgId).limit(limit).offset(offset).build();

    PagedResult<ConsultantShortlistSummaryResponse> result =
        queryService.listShortlists(accessRequest, pagedQuery, jobId);
    return ResponseEntity.ok(ApiResponseEnvelope.success(result));
  }

  @GetMapping("/{shortlistId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> getShortlist(
      @PathVariable String shortlistId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    ShortlistId sid = parseShortlistId(shortlistId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.SHORTLIST);

    return queryService.getShortlistDetail(accessRequest, orgId, sid)
        .map(response -> ResponseEntity.ok(
            ApiResponseEnvelope.<ApiSafeResponseBody>success(response)))
        .orElseGet(ConsultantShortlistController::notFound);
  }

  @PostMapping
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> createShortlist(
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ShortlistCreateRequest request) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    AccessRequest accessRequest = buildAccessRequest(ResourceType.SHORTLIST, AccessAction.CREATE);

    ConsultantShortlistDetailResponse result =
        commandService.createShortlist(accessRequest, orgId, principal.userAccountId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponseEnvelope.success(result));
  }

  @PutMapping("/{shortlistId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> updateShortlist(
      @PathVariable String shortlistId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ShortlistUpdateRequest request) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    ShortlistId sid = parseShortlistId(shortlistId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.SHORTLIST, AccessAction.UPDATE);

    ConsultantShortlistDetailResponse result =
        commandService.updateShortlist(
            accessRequest, orgId, principal.userAccountId(), sid, request);
    return ResponseEntity.ok(ApiResponseEnvelope.success(result));
  }

  @PostMapping("/{shortlistId}/cards")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> addShortlistCandidateCard(
      @PathVariable String shortlistId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ShortlistCandidateCardCreateRequest request) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    ShortlistId sid = parseShortlistId(shortlistId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.SHORTLIST, AccessAction.UPDATE);

    ConsultantShortlistDetailResponse result = commandService.addShortlistCandidateCard(
        accessRequest,
        orgId,
        principal.userAccountId(),
        sid,
        request);
    return ResponseEntity.ok(ApiResponseEnvelope.success(result));
  }

  @PutMapping("/{shortlistId}/cards/{cardId}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> updateShortlistCandidateCard(
      @PathVariable String shortlistId,
      @PathVariable String cardId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ShortlistCandidateCardUpdateRequest request) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    ShortlistId sid = parseShortlistId(shortlistId);
    ShortlistCandidateCardId shortlistCandidateCardId = parseShortlistCandidateCardId(cardId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.SHORTLIST, AccessAction.UPDATE);

    ConsultantShortlistDetailResponse result = commandService.updateShortlistCandidateCard(
        accessRequest,
        orgId,
        principal.userAccountId(),
        sid,
        shortlistCandidateCardId,
        request);
    return ResponseEntity.ok(ApiResponseEnvelope.success(result));
  }

  @PostMapping("/{shortlistId}/send")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> sendShortlist(
      @PathVariable String shortlistId,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal,
      @RequestBody ShortlistSendRequest request) {

    requireConsultantRole(principal.portalRole());
    UUID orgId = principal.organizationId();
    ShortlistId sid = parseShortlistId(shortlistId);
    AccessRequest accessRequest = buildAccessRequest(ResourceType.SHORTLIST, AccessAction.UPDATE);

    ConsultantShortlistDetailResponse result = commandService.sendShortlist(
        accessRequest,
        orgId,
        principal.userAccountId(),
        sid,
        request);
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

  private static void requireConsultantRole(PortalRole portalRole) {
    if (portalRole != PortalRole.CONSULTANT) {
      throw new AccessDeniedException(
          new AccessDecision(false,
              "consultant_role_required",
              "Consultant role is required for this endpoint."));
    }
  }


  private static ShortlistId parseShortlistId(String shortlistId) {
    if (shortlistId == null || shortlistId.isBlank()) {
      throw new IllegalArgumentException("shortlistId must not be blank");
    }
    try {
      return new ShortlistId(UUID.fromString(shortlistId));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid shortlist ID format.");
    }
  }

  private static ShortlistCandidateCardId parseShortlistCandidateCardId(String cardId) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("cardId must not be blank");
    }
    try {
      return new ShortlistCandidateCardId(UUID.fromString(cardId));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid shortlist candidate card ID format.");
    }
  }

  private static AccessRequest buildAccessRequest(ResourceType resourceType) {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        resourceType,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
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
            "shortlist_unavailable",
            "Shortlist is unavailable."));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(
      HttpStatus status, ApiErrorResponse error) {
    return ResponseEntity.status(status).body(ApiResponseEnvelope.failure(error));
  }
}
