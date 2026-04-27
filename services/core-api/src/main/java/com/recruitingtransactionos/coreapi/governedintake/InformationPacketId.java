package com.recruitingtransactionos.coreapi.governedintake;

import java.util.Objects;
import java.util.UUID;

public record InformationPacketId(UUID value) {

  public InformationPacketId {
    Objects.requireNonNull(value, "informationPacketId must not be null");
  }
}
