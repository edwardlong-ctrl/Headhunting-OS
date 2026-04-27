package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InformationPacket(
    InformationPacketId informationPacketId,
    UUID organizationId,
    InformationPacketType packetType,
    IntendedEntityType intendedEntityType,
    UUID intendedEntityId,
    ActorRole createdByActorType,
    UUID createdByActorId,
    InformationPacketStatus processingStatus,
    Instant createdAt,
    Instant updatedAt,
    String notes,
    String metadataJson) {

  public InformationPacket {
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(packetType, "packetType must not be null");
    Objects.requireNonNull(intendedEntityType, "intendedEntityType must not be null");
    Objects.requireNonNull(createdByActorType, "createdByActorType must not be null");
    Objects.requireNonNull(processingStatus, "processingStatus must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    notes = GovernedIntakeGuards.optionalNonBlank(notes, "notes");
    metadataJson = GovernedIntakeGuards.metadataJson(metadataJson);
  }
}
