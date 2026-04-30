package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.Objects;
import java.util.UUID;

public record DocumentUploadCommand(
    UUID organizationId,
    SourceItemType sourceType,
    SourceItemOrigin origin,
    String title,
    ActorRole uploadedByActorType,
    UUID uploadedByActorId,
    String originalFilename,
    String mimeType,
    long contentLength) {

  public DocumentUploadCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(origin, "origin must not be null");
    title = GovernedIntakeGuards.optionalNonBlank(title, "title");
    Objects.requireNonNull(uploadedByActorType, "uploadedByActorType must not be null");
    originalFilename = GovernedIntakeGuards.optionalNonBlank(originalFilename, "originalFilename");
    if (mimeType == null || mimeType.isBlank()) {
      throw new IllegalArgumentException("mimeType must not be null or blank");
    }
    mimeType = mimeType.strip();
    if (contentLength <= 0) {
      throw new IllegalArgumentException("contentLength must be positive");
    }
  }

  public static DocumentUploadCommand fromWireValues(
      UUID organizationId,
      String sourceType,
      String origin,
      String title,
      ActorRole uploadedByActorType,
      UUID uploadedByActorId,
      String originalFilename,
      String mimeType,
      long contentLength) {
    return new DocumentUploadCommand(
        organizationId,
        SourceItemType.fromWireValue(sourceType),
        SourceItemOrigin.fromWireValue(origin),
        title,
        uploadedByActorType,
        uploadedByActorId,
        originalFilename,
        mimeType,
        contentLength);
  }

  public static final class Builder {
    private final UUID organizationId;
    private final SourceItemType sourceType;
    private final SourceItemOrigin origin;
    private final ActorRole uploadedByActorType;
    private String title;
    private UUID uploadedByActorId;
    private String originalFilename;
    private String mimeType;
    private long contentLength;

    public Builder(UUID organizationId, SourceItemType sourceType, SourceItemOrigin origin,
        ActorRole uploadedByActorType) {
      this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
      this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
      this.origin = Objects.requireNonNull(origin, "origin must not be null");
      this.uploadedByActorType = Objects.requireNonNull(uploadedByActorType,
          "uploadedByActorType must not be null");
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder uploadedByActorId(UUID uploadedByActorId) {
      this.uploadedByActorId = uploadedByActorId;
      return this;
    }

    public Builder originalFilename(String originalFilename) {
      this.originalFilename = originalFilename;
      return this;
    }

    public Builder mimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public Builder contentLength(long contentLength) {
      this.contentLength = contentLength;
      return this;
    }

    public DocumentUploadCommand build() {
      return new DocumentUploadCommand(
          organizationId, sourceType, origin, title, uploadedByActorType,
          uploadedByActorId, originalFilename, mimeType, contentLength);
    }
  }
}
