package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;

public record AccessDecision(
    boolean allowed,
    String reasonCode,
    String safeExplanation) {

  public AccessDecision {
    reasonCode = requireNonBlank(reasonCode, "reasonCode");
    safeExplanation = requireNonBlank(safeExplanation, "safeExplanation");
  }

  static AccessDecision allow(String reasonCode, String safeExplanation) {
    return new AccessDecision(true, reasonCode, safeExplanation);
  }

  static AccessDecision deny(String reasonCode, String safeExplanation) {
    return new AccessDecision(false, reasonCode, safeExplanation);
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
