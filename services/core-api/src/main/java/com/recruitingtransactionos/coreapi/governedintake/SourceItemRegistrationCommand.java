package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SourceItemRegistrationCommand(
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
    String metadataJson,
    SourceItemStatus status) {

  public SourceItemRegistrationCommand {
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
    metadataJson = GovernedIntakeGuards.metadataJson(metadataJson);
    Objects.requireNonNull(status, "status must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID organizationId;
    private SourceItemType sourceType;
    private SourceItemOrigin origin;
    private String title;
    private String contentHash;
    private String externalRef;
    private String storageRef;
    private String rawRef;
    private String language;
    private ActorRole uploadedByActorType;
    private UUID uploadedByActorId;
    private Instant receivedAt;
    private String metadataJson;
    private SourceItemStatus status;

    private Builder() {
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder sourceType(SourceItemType sourceType) {
      this.sourceType = sourceType;
      return this;
    }

    public Builder origin(SourceItemOrigin origin) {
      this.origin = origin;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder contentHash(String contentHash) {
      this.contentHash = contentHash;
      return this;
    }

    public Builder externalRef(String externalRef) {
      this.externalRef = externalRef;
      return this;
    }

    public Builder storageRef(String storageRef) {
      this.storageRef = storageRef;
      return this;
    }

    public Builder rawRef(String rawRef) {
      this.rawRef = rawRef;
      return this;
    }

    public Builder language(String language) {
      this.language = language;
      return this;
    }

    public Builder uploadedByActorType(ActorRole uploadedByActorType) {
      this.uploadedByActorType = uploadedByActorType;
      return this;
    }

    public Builder uploadedByActorId(UUID uploadedByActorId) {
      this.uploadedByActorId = uploadedByActorId;
      return this;
    }

    public Builder receivedAt(Instant receivedAt) {
      this.receivedAt = receivedAt;
      return this;
    }

    public Builder metadataJson(String metadataJson) {
      this.metadataJson = metadataJson;
      return this;
    }

    public Builder status(SourceItemStatus status) {
      this.status = status;
      return this;
    }

    public SourceItemRegistrationCommand build() {
      return new SourceItemRegistrationCommand(
          organizationId,
          sourceType,
          origin,
          title,
          contentHash,
          externalRef,
          storageRef,
          rawRef,
          language,
          uploadedByActorType,
          uploadedByActorId,
          receivedAt,
          metadataJson,
          status);
    }
  }
}
