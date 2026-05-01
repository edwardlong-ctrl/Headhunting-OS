package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-safe/candidate-cards")
public final class ClientSafeCandidateCardController {

  private final ClientSafeCandidateCardApiQueryService queryService;

  public ClientSafeCandidateCardController(ClientSafeCandidateCardApiQueryService queryService) {
    this.queryService =
        Objects.requireNonNull(queryService, "queryService must not be null");
  }

  @GetMapping("/{anonymousCardRef}")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> readClientSafeCandidateCard(
      @PathVariable String anonymousCardRef,
      @AuthenticationPrincipal RtoAuthenticatedPrincipal principal) {
    AnonymousCandidateCardId cardId = AnonymousCandidateCardId.of(anonymousCardRef);
    AccessRequest accessRequest = ClientSafeCandidateCardApiAccessContextAdapter.fromPrincipal(principal);
    ClientSafeCandidateCardQueryScope queryScope =
        ClientSafeCandidateCardApiAccessContextAdapter.queryScopeFromPrincipal(principal);

    return queryService.findClientSafeCandidateCard(accessRequest, queryScope, cardId)
        .map(ClientSafeCandidateCardController::success)
        .orElseGet(ClientSafeCandidateCardController::notFound);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> accessDenied(
      AccessDeniedException exception) {
    return error(
        HttpStatus.FORBIDDEN,
        ApiAccessDeniedResponse.from(exception).toErrorResponse());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      IllegalArgumentException exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        ApiValidationErrorResponse.of(
            "invalid_anonymous_card_reference",
            java.util.List.of("Invalid anonymous card reference."))
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

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> success(
      ClientSafeCandidateCardResponse response) {
    return ResponseEntity.ok(ApiResponseEnvelope.success(response));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> notFound() {
    return error(
        HttpStatus.NOT_FOUND,
        new ApiErrorResponse(
            "not_found",
            "client_safe_candidate_card_unavailable",
            "Client-safe candidate card is unavailable."));
  }

  private static ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> error(
      HttpStatus status,
      ApiErrorResponse error) {
    return ResponseEntity
        .status(status)
        .body(ApiResponseEnvelope.failure(error));
  }
}
