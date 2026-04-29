package com.recruitingtransactionos.coreapi.company;

import java.util.Objects;
import java.util.UUID;

public record CompanyContactId(UUID value) {

  public CompanyContactId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
