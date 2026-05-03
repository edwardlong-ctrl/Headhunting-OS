package com.recruitingtransactionos.coreapi.apiboundary.client;

public record ClientUnlockRequestCreateRequest(String requestReason) {
  public ClientUnlockRequestCreateRequest {
    if (requestReason == null || requestReason.isBlank()) {
      throw new IllegalArgumentException("requestReason must not be blank");
    }
  }
}
