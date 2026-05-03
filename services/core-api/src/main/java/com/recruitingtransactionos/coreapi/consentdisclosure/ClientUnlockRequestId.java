package com.recruitingtransactionos.coreapi.consentdisclosure;

import java.util.Objects;
import java.util.UUID;

public record ClientUnlockRequestId(UUID value) {

  public ClientUnlockRequestId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
