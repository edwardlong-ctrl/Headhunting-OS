package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;
import java.util.UUID;

public record EntityRef(
    String entityType,
    UUID entityId) {

  public EntityRef {
    entityType = PortContractGuards.requireNonBlank(entityType, "entityType");
    Objects.requireNonNull(entityId, "entityId must not be null");
  }
}
