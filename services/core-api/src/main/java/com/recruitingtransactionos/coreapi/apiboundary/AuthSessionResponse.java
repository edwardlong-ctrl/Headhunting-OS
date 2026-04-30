package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.identityauth.AuthenticationService;
import java.util.Objects;

public record AuthSessionResponse(
    String organizationId,
    String userAccountId,
    String displayName,
    String portalRole,
    String tokenType,
    String accessToken,
    String refreshToken,
    String accessTokenExpiresAt,
    String refreshTokenExpiresAt) implements ApiSafeResponseBody {

  public AuthSessionResponse {
    organizationId = ApiBoundaryContractRules.requireNonBlank(organizationId, "organizationId");
    userAccountId = ApiBoundaryContractRules.requireNonBlank(userAccountId, "userAccountId");
    displayName = ApiBoundaryContractRules.sanitizeExternalText(displayName, "User");
    portalRole = ApiBoundaryContractRules.requireNonBlank(portalRole, "portalRole");
    tokenType = ApiBoundaryContractRules.requireNonBlank(tokenType, "tokenType");
    accessToken = ApiBoundaryContractRules.requireNonBlank(accessToken, "accessToken");
    refreshToken = ApiBoundaryContractRules.requireNonBlank(refreshToken, "refreshToken");
    accessTokenExpiresAt = ApiBoundaryContractRules.requireNonBlank(
        accessTokenExpiresAt, "accessTokenExpiresAt");
    refreshTokenExpiresAt = ApiBoundaryContractRules.requireNonBlank(
        refreshTokenExpiresAt, "refreshTokenExpiresAt");
  }

  public static AuthSessionResponse from(AuthenticationService.AuthenticatedSession session) {
    Objects.requireNonNull(session, "session must not be null");
    return new AuthSessionResponse(
        session.organizationId().toString(),
        session.userAccountId().toString(),
        session.displayName(),
        session.portalRole().wireValue(),
        "Bearer",
        session.accessToken(),
        session.refreshToken(),
        session.accessTokenExpiresAt().toString(),
        session.refreshTokenExpiresAt().toString());
  }
}
