package com.recruitingtransactionos.coreapi.company;

import java.util.Objects;
import java.util.UUID;

public record CompanyPreferenceId(UUID value) {

  public CompanyPreferenceId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
