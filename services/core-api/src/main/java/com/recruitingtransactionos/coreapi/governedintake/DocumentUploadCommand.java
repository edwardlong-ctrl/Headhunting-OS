package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.Objects;
import java.util.UUID;

public record DocumentUploadCommand(
    UUID organizationId,
    SourceItemType sourceType,
    SourceItemOrigin origin,
    InformationPacketType packetType,
    IntendedEntityType intendedEntityType,
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
    Objects.requireNonNull(packetType, "packetType must not be null");
    Objects.requireNonNull(intendedEntityType, "intendedEntityType must not be null");
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
      String packetType,
      String intendedEntityType,
      String title,
      ActorRole uploadedByActorType,
      UUID uploadedByActorId,
      String originalFilename,
      String mimeType,
      long contentLength) {
    SourceItemType typedSourceType = SourceItemType.fromWireValue(sourceType);
    PacketIntent packetIntent = PacketIntent.resolve(
        typedSourceType,
        packetType == null ? null : InformationPacketType.fromWireValue(packetType),
        intendedEntityType == null ? null : IntendedEntityType.fromWireValue(intendedEntityType));
    return new DocumentUploadCommand(
        organizationId,
        typedSourceType,
        SourceItemOrigin.fromWireValue(origin),
        packetIntent.packetType(),
        packetIntent.intendedEntityType(),
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
    private InformationPacketType packetType;
    private IntendedEntityType intendedEntityType;
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
      PacketIntent packetIntent = PacketIntent.resolve(sourceType, null, null);
      this.packetType = packetIntent.packetType();
      this.intendedEntityType = packetIntent.intendedEntityType();
    }

    public Builder packetType(InformationPacketType packetType) {
      this.packetType = packetType;
      return this;
    }

    public Builder intendedEntityType(IntendedEntityType intendedEntityType) {
      this.intendedEntityType = intendedEntityType;
      return this;
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
          organizationId, sourceType, origin, packetType, intendedEntityType, title, uploadedByActorType,
          uploadedByActorId, originalFilename, mimeType, contentLength);
    }
  }

  private record PacketIntent(
      InformationPacketType packetType,
      IntendedEntityType intendedEntityType) {

    private static PacketIntent resolve(
        SourceItemType sourceType,
        InformationPacketType packetType,
        IntendedEntityType intendedEntityType) {
      InformationPacketType resolvedPacketType = packetType != null
          ? packetType
          : defaultPacketType(sourceType);
      IntendedEntityType resolvedIntendedEntityType = intendedEntityType != null
          ? intendedEntityType
          : defaultIntendedEntityType(sourceType);
      if (!isSupported(resolvedPacketType, resolvedIntendedEntityType)) {
        throw new IllegalArgumentException(
            "unsupported packet/intended entity combination: "
                + resolvedPacketType.wireValue() + " -> " + resolvedIntendedEntityType.wireValue());
      }
      return new PacketIntent(resolvedPacketType, resolvedIntendedEntityType);
    }

    private static InformationPacketType defaultPacketType(SourceItemType sourceType) {
      return switch (sourceType) {
        case COMPANY_MATERIAL -> InformationPacketType.COMPANY;
        case JD -> InformationPacketType.JOB;
        case CALL_NOTE -> InformationPacketType.CALL_NOTE;
        case INTERVIEW_FEEDBACK -> InformationPacketType.FEEDBACK;
        default -> InformationPacketType.CANDIDATE;
      };
    }

    private static IntendedEntityType defaultIntendedEntityType(SourceItemType sourceType) {
      return switch (sourceType) {
        case COMPANY_MATERIAL -> IntendedEntityType.COMPANY;
        case JD -> IntendedEntityType.JOB;
        default -> IntendedEntityType.CANDIDATE;
      };
    }

    private static boolean isSupported(
        InformationPacketType packetType,
        IntendedEntityType intendedEntityType) {
      return switch (intendedEntityType) {
        case CANDIDATE -> packetType == InformationPacketType.CANDIDATE
            || packetType == InformationPacketType.CALL_NOTE
            || packetType == InformationPacketType.FEEDBACK;
        case COMPANY -> packetType == InformationPacketType.COMPANY
            || packetType == InformationPacketType.CALL_NOTE
            || packetType == InformationPacketType.FEEDBACK;
        case JOB -> packetType == InformationPacketType.JOB
            || packetType == InformationPacketType.CALL_NOTE
            || packetType == InformationPacketType.FEEDBACK;
        default -> false;
      };
    }
  }
}
