package com.recruitingtransactionos.coreapi.integration;

import java.util.Objects;
import java.util.UUID;

public record OutboundIntegrationTarget(String address, UUID organizationId) {
  public OutboundIntegrationTarget {
    Objects.requireNonNull(address, "address must not be null");
    if (address.isBlank()) {
      throw new IllegalArgumentException("address must not be blank");
    }
    address = address.strip();
    Objects.requireNonNull(organizationId, "organizationId must not be null");
  }
}
