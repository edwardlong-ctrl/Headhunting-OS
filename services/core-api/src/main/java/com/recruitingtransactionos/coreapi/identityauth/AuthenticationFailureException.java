package com.recruitingtransactionos.coreapi.identityauth;

import org.springframework.http.HttpStatus;

public final class AuthenticationFailureException extends RuntimeException {

  private final HttpStatus httpStatus;
  private final String errorCode;
  private final String safeReason;
  private final String safeMessage;

  private AuthenticationFailureException(
      HttpStatus httpStatus,
      String errorCode,
      String safeReason,
      String safeMessage) {
    super(safeReason);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
    this.safeReason = safeReason;
    this.safeMessage = safeMessage;
  }

  public static AuthenticationFailureException invalidCredentials() {
    return new AuthenticationFailureException(
        HttpStatus.UNAUTHORIZED,
        "authentication_failed",
        "invalid_credentials",
        "Authentication failed.");
  }

  public static AuthenticationFailureException invalidRefreshToken() {
    return new AuthenticationFailureException(
        HttpStatus.UNAUTHORIZED,
        "authentication_failed",
        "invalid_refresh_token",
        "Authentication failed.");
  }

  public static AuthenticationFailureException accountInactive() {
    return new AuthenticationFailureException(
        HttpStatus.FORBIDDEN,
        "access_denied",
        "account_inactive",
        "Access denied.");
  }

  public static AuthenticationFailureException roleAssignmentRequired() {
    return new AuthenticationFailureException(
        HttpStatus.FORBIDDEN,
        "access_denied",
        "role_assignment_required",
        "Access denied.");
  }

  public static AuthenticationFailureException organizationSelectionRequired() {
    return new AuthenticationFailureException(
        HttpStatus.CONFLICT,
        "organization_selection_required",
        "organization_selection_required",
        "Multiple organizations match this account. Organization selection is required.");
  }

  public HttpStatus httpStatus() {
    return httpStatus;
  }

  public String errorCode() {
    return errorCode;
  }

  public String safeReason() {
    return safeReason;
  }

  public String safeMessage() {
    return safeMessage;
  }
}
