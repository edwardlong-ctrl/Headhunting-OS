package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class PortContractGuards {

  private PortContractGuards() {
  }

  static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  static Duration requireNonNegative(Duration value, String name) {
    if (value != null && value.isNegative()) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
    return value;
  }

  static List<UUID> copyUuidList(List<UUID> values, String name) {
    Objects.requireNonNull(values, name + " must not be null");
    List<UUID> copy = List.copyOf(values);
    copy.forEach(value -> Objects.requireNonNull(value, name + " must not contain null values"));
    return copy;
  }
}
