package com.recruitingtransactionos.coreapi.company;

import java.util.Objects;
import java.util.UUID;

public record CompanyId(UUID value) {

  public CompanyId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
