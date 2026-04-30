package com.recruitingtransactionos.coreapi.apiboundary.auth;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record RefreshTokenRequest(String refreshToken) {

  public RefreshTokenRequest {
    refreshToken = ApiBoundaryContractRules.requireNonBlank(refreshToken, "refreshToken");
  }
}
