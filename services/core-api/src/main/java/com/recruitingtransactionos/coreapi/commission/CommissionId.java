package com.recruitingtransactionos.coreapi.commission;

import java.util.Objects;
import java.util.UUID;

public record CommissionId(UUID value) {

  public CommissionId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
