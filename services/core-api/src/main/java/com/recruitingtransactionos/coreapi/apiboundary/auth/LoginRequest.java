package com.recruitingtransactionos.coreapi.apiboundary.auth;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import java.util.Locale;

public record LoginRequest(
    String organizationId,
    String email,
    String password,
    String portalRole) {

  public LoginRequest {
    organizationId = ApiBoundaryContractRules.requireNonBlank(organizationId, "organizationId");
    email = ApiBoundaryContractRules.requireNonBlank(email, "email").toLowerCase(Locale.ROOT);
    password = ApiBoundaryContractRules.requireNonBlank(password, "password");
    portalRole = ApiBoundaryContractRules.requireNonBlank(portalRole, "portalRole")
        .toLowerCase(Locale.ROOT);
  }
}
