package com.recruitingtransactionos.coreapi.apiboundary.auth;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record LogoutRequest(String refreshToken) {

  public LogoutRequest {
    refreshToken = ApiBoundaryContractRules.requireNonBlank(refreshToken, "refreshToken");
  }
}
