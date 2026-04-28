package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.List;
import java.util.Objects;

final class ClientSafeProjectionGuards {

  private ClientSafeProjectionGuards() {
  }

  static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  static List<String> copyNonBlankList(List<String> values, String name) {
    Objects.requireNonNull(values, name + " must not be null");
    return values.stream()
        .map(value -> requireNonBlank(value, name + " entry"))
        .toList();
  }
}
