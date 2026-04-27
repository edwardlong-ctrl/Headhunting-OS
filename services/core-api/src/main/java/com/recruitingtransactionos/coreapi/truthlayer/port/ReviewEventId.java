package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;
import java.util.UUID;

public record ReviewEventId(UUID value) {

  public ReviewEventId {
    Objects.requireNonNull(value, "reviewEventId must not be null");
  }
}
