package com.recruitingtransactionos.coreapi.apiboundary.auth;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import java.util.Locale;

public record LoginRequest(
    String organizationId,
    String email,
    String password,
    String portalRole) {

  public LoginRequest {
    organizationId = optionalNonBlank(organizationId);
    email = ApiBoundaryContractRules.requireNonBlank(email, "email").toLowerCase(Locale.ROOT);
    password = ApiBoundaryContractRules.requireNonBlank(password, "password");
    portalRole = ApiBoundaryContractRules.requireNonBlank(portalRole, "portalRole")
        .toLowerCase(Locale.ROOT);
  }

  private static String optionalNonBlank(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.strip();
    return normalized.isEmpty() ? null : normalized;
  }
}
