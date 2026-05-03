package com.recruitingtransactionos.coreapi.apiboundary.consultant;

public record ShortlistSendRequest(boolean approvalConfirmed) {

  public ShortlistSendRequest {
    if (!approvalConfirmed) {
      throw new IllegalArgumentException("approvalConfirmed must be true");
    }
  }
}
