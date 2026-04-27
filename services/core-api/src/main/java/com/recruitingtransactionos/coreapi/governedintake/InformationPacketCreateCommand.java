package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.Objects;
import java.util.UUID;

public record InformationPacketCreateCommand(
    UUID organizationId,
    InformationPacketType packetType,
    IntendedEntityType intendedEntityType,
    UUID intendedEntityId,
    ActorRole createdByActorType,
    UUID createdByActorId,
    InformationPacketStatus processingStatus,
    String notes,
    String metadataJson) {

  public InformationPacketCreateCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(packetType, "packetType must not be null");
    Objects.requireNonNull(intendedEntityType, "intendedEntityType must not be null");
    Objects.requireNonNull(createdByActorType, "createdByActorType must not be null");
    Objects.requireNonNull(processingStatus, "processingStatus must not be null");
    notes = GovernedIntakeGuards.optionalNonBlank(notes, "notes");
    metadataJson = GovernedIntakeGuards.metadataJson(metadataJson);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID organizationId;
    private InformationPacketType packetType;
    private IntendedEntityType intendedEntityType;
    private UUID intendedEntityId;
    private ActorRole createdByActorType;
    private UUID createdByActorId;
    private InformationPacketStatus processingStatus;
    private String notes;
    private String metadataJson;

    private Builder() {
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder packetType(InformationPacketType packetType) {
      this.packetType = packetType;
      return this;
    }

    public Builder intendedEntityType(IntendedEntityType intendedEntityType) {
      this.intendedEntityType = intendedEntityType;
      return this;
    }

    public Builder intendedEntityId(UUID intendedEntityId) {
      this.intendedEntityId = intendedEntityId;
      return this;
    }

    public Builder createdByActorType(ActorRole createdByActorType) {
      this.createdByActorType = createdByActorType;
      return this;
    }

    public Builder createdByActorId(UUID createdByActorId) {
      this.createdByActorId = createdByActorId;
      return this;
    }

    public Builder processingStatus(InformationPacketStatus processingStatus) {
      this.processingStatus = processingStatus;
      return this;
    }

    public Builder notes(String notes) {
      this.notes = notes;
      return this;
    }

    public Builder metadataJson(String metadataJson) {
      this.metadataJson = metadataJson;
      return this;
    }

    public InformationPacketCreateCommand build() {
      return new InformationPacketCreateCommand(
          organizationId,
          packetType,
          intendedEntityType,
          intendedEntityId,
          createdByActorType,
          createdByActorId,
          processingStatus,
          notes,
          metadataJson);
    }
  }
}
