package com.recruitingtransactionos.coreapi.governedintake;

import java.util.Objects;
import java.util.UUID;

public record DocumentUploadResult(
    SourceItemId sourceItemId,
    UUID informationPacketId,
    String contentHash,
    String storageRef,
    String scanStatus) {

  public DocumentUploadResult {
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Objects.requireNonNull(contentHash, "contentHash must not be null");
    Objects.requireNonNull(storageRef, "storageRef must not be null");
    scanStatus = Objects.requireNonNullElse(scanStatus, "not_scanned");
  }
}
