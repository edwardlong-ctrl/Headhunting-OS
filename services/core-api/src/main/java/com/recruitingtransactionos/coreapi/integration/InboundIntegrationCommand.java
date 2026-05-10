package com.recruitingtransactionos.coreapi.integration;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import java.util.Objects;
import java.util.UUID;

public record InboundIntegrationCommand(
    UUID organizationId,
    UUID actorId,
    UUID actorOrganizationId,
    String providerKey,
    IntegrationChannel channel,
    String externalRef,
    SourceItemType sourceItemType,
    InformationPacketType packetType,
    IntendedEntityType intendedEntityType,
    String rawPayloadJson,
    String metadataJson,
    InboundIntegrationPurpose purpose) {

  public InboundIntegrationCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(actorOrganizationId, "actorOrganizationId must not be null");
    providerKey = requireNonBlank(providerKey, "providerKey");
    Objects.requireNonNull(channel, "channel must not be null");
    externalRef = requireNonBlank(externalRef, "externalRef");
    Objects.requireNonNull(sourceItemType, "sourceItemType must not be null");
    Objects.requireNonNull(packetType, "packetType must not be null");
    Objects.requireNonNull(intendedEntityType, "intendedEntityType must not be null");
    rawPayloadJson = requireNonBlank(rawPayloadJson, "rawPayloadJson");
    metadataJson = requireNonBlank(metadataJson, "metadataJson");
    Objects.requireNonNull(purpose, "purpose must not be null");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
