package com.recruitingtransactionos.coreapi.governedintake;

import java.util.Objects;
import java.util.UUID;

public record AttachSourceItemToPacketCommand(
    UUID organizationId,
    InformationPacketId informationPacketId,
    SourceItemId sourceItemId) {

  public AttachSourceItemToPacketCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
  }
}
