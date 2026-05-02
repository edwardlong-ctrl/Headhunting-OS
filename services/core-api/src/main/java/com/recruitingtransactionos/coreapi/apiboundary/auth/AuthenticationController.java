package com.recruitingtransactionos.coreapi.apiboundary.auth;

import com.recruitingtransactionos.coreapi.apiboundary.ApiErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ApiResponseEnvelope;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ApiValidationErrorResponse;
import com.recruitingtransactionos.coreapi.apiboundary.AuthLogoutResponse;
import com.recruitingtransactionos.coreapi.apiboundary.AuthSessionResponse;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.AuthenticationFailureException;
import com.recruitingtransactionos.coreapi.identityauth.AuthenticationService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public final class AuthenticationController {

  private final AuthenticationService authenticationService;

  public AuthenticationController(AuthenticationService authenticationService) {
    this.authenticationService = Objects.requireNonNull(
        authenticationService, "authenticationService must not be null");
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> login(
      @RequestBody LoginRequest request) {
    AuthenticationService.AuthenticatedSession session = authenticationService.login(
        parseOrganizationId(request.organizationId()),
        request.email(),
        request.password(),
        parsePortalRole(request.portalRole()));
    return ResponseEntity.ok(ApiResponseEnvelope.success(AuthSessionResponse.from(session)));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> refresh(
      @RequestBody RefreshTokenRequest request) {
    AuthenticationService.AuthenticatedSession session = authenticationService.refresh(
        request.refreshToken());
    return ResponseEntity.ok(ApiResponseEnvelope.success(AuthSessionResponse.from(session)));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> logout(
      @RequestBody LogoutRequest request) {
    return ResponseEntity.ok(ApiResponseEnvelope.success(
        AuthLogoutResponse.from(authenticationService.logout(request.refreshToken()))));
  }

  @ExceptionHandler(AuthenticationFailureException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> authenticationFailed(
      AuthenticationFailureException exception) {
    return ResponseEntity.status(exception.httpStatus())
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            exception.errorCode(),
            exception.safeReason(),
            exception.safeMessage())));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> validationFailed(
      IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(ApiResponseEnvelope.failure(
        ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request."))
            .toErrorResponse()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> unreadablePayload(
      HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(ApiResponseEnvelope.failure(
        ApiValidationErrorResponse.of(
            "invalid_request",
            List.of("Invalid request body."))
            .toErrorResponse()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> requestFailed(
      RuntimeException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponseEnvelope.failure(new ApiErrorResponse(
            "internal_error",
            "request_failed",
            "Request failed.")));
  }

  private static UUID parseOrganizationId(String organizationId) {
    if (organizationId == null || organizationId.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(organizationId);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid organization ID format.");
    }
  }

  private static PortalRole parsePortalRole(String portalRole) {
    String normalized = portalRole.strip().toLowerCase(Locale.ROOT);
    for (PortalRole value : PortalRole.values()) {
      if (value.wireValue().equals(normalized)) {
        if (value == PortalRole.UNKNOWN
            || value == PortalRole.SYSTEM
            || value == PortalRole.AI_ASSISTANT) {
          throw new IllegalArgumentException("Invalid portal role.");
        }
        return value;
      }
    }
    throw new IllegalArgumentException("Invalid portal role.");
  }
}
