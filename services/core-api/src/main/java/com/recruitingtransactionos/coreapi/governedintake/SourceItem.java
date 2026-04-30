package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SourceItem(
    SourceItemId sourceItemId,
    UUID organizationId,
    SourceItemType sourceType,
    SourceItemOrigin origin,
    String title,
    String contentHash,
    String externalRef,
    String storageRef,
    String rawRef,
    String language,
    ActorRole uploadedByActorType,
    UUID uploadedByActorId,
    Instant receivedAt,
    Instant createdAt,
    String metadataJson,
    SourceItemStatus status,
    String mimeType,
    Long fileSizeBytes,
    String originalFilename,
    String scanStatus) {

  public SourceItem {
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(origin, "origin must not be null");
    title = GovernedIntakeGuards.optionalNonBlank(title, "title");
    contentHash = GovernedIntakeGuards.optionalNonBlank(contentHash, "contentHash");
    externalRef = GovernedIntakeGuards.optionalNonBlank(externalRef, "externalRef");
    storageRef = GovernedIntakeGuards.optionalNonBlank(storageRef, "storageRef");
    rawRef = GovernedIntakeGuards.optionalNonBlank(rawRef, "rawRef");
    language = GovernedIntakeGuards.optionalNonBlank(language, "language");
    Objects.requireNonNull(uploadedByActorType, "uploadedByActorType must not be null");
    Objects.requireNonNull(receivedAt, "receivedAt must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    metadataJson = GovernedIntakeGuards.metadataJson(metadataJson);
    Objects.requireNonNull(status, "status must not be null");
    mimeType = GovernedIntakeGuards.optionalNonBlank(mimeType, "mimeType");
    originalFilename = GovernedIntakeGuards.optionalNonBlank(originalFilename, "originalFilename");
    scanStatus = GovernedIntakeGuards.optionalNonBlank(scanStatus, "scanStatus");
  }
}
