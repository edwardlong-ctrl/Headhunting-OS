package com.recruitingtransactionos.coreapi.placement;

import java.util.Objects;
import java.util.UUID;

public record PlacementId(UUID value) {

  public PlacementId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
