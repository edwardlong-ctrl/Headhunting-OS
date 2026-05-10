package com.recruitingtransactionos.coreapi.integration;

import java.util.Objects;
import java.util.UUID;

public record OcrSttProcessingRequest(
    UUID organizationId,
    UUID actorId,
    String sourceItemRef,
    String mimeType,
    String storageRef) {

  public OcrSttProcessingRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    sourceItemRef = requireNonBlank(sourceItemRef, "sourceItemRef");
    mimeType = requireNonBlank(mimeType, "mimeType");
    storageRef = requireNonBlank(storageRef, "storageRef");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
