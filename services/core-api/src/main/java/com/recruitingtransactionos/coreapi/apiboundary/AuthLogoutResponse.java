package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.identityauth.AuthenticationService;
import java.util.Objects;

public record AuthLogoutResponse(
    String status,
    String loggedOutAt) implements ApiSafeResponseBody {

  public AuthLogoutResponse {
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    loggedOutAt = ApiBoundaryContractRules.requireNonBlank(loggedOutAt, "loggedOutAt");
  }

  public static AuthLogoutResponse from(AuthenticationService.LoggedOutSession loggedOut) {
    Objects.requireNonNull(loggedOut, "loggedOut must not be null");
    return new AuthLogoutResponse(loggedOut.status(), loggedOut.loggedOutAt().toString());
  }
}
