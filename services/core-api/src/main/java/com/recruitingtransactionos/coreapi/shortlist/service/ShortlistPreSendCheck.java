package com.recruitingtransactionos.coreapi.shortlist.service;

import java.util.Objects;

public record ShortlistPreSendCheck(
    String code,
    String label,
    boolean passed) {

  public ShortlistPreSendCheck {
    code = requireNonBlank(code, "code");
    label = requireNonBlank(label, "label");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
