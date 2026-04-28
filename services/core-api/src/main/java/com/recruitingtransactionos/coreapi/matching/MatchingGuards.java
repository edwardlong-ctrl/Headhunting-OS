package com.recruitingtransactionos.coreapi.matching;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

final class MatchingGuards {

  private static final Pattern RAW_UUID = Pattern.compile(
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private static final Pattern SAFE_REASON_CODE = Pattern.compile("[a-z0-9_.-]+");

  private MatchingGuards() {
  }

  static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    String stripped = value.strip();
    if (stripped.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return stripped;
  }

  static String requireOpaqueRef(String value, String name, String requiredPrefix) {
    String stripped = requireNonBlank(value, name);
    if (RAW_UUID.matcher(stripped).matches()) {
      throw new IllegalArgumentException(name + " must not be a raw UUID");
    }
    if (!stripped.startsWith(requiredPrefix)) {
      throw new IllegalArgumentException(name + " must use the " + requiredPrefix + " prefix");
    }
    return stripped;
  }

  static String requireSafeReasonCode(String reasonCode) {
    String normalized = requireNonBlank(reasonCode, "reasonCode").toLowerCase(Locale.ROOT);
    if (!SAFE_REASON_CODE.matcher(normalized).matches()) {
      throw new IllegalArgumentException("reasonCode must be a safe reason code");
    }
    return normalized;
  }

  static String requireSafeExplanation(String safeExplanation) {
    String stripped = requireNonBlank(safeExplanation, "safeExplanation");
    String lower = stripped.toLowerCase(Locale.ROOT);
    if (stripped.contains("\n")
        || stripped.contains("\r")
        || lower.contains("stack trace")
        || lower.contains("stacktrace")
        || lower.contains("exception")
        || lower.contains("\tat ")
        || lower.contains("java.")
        || lower.contains("email")
        || lower.contains("phone")
        || lower.contains("linkedin")) {
      throw new IllegalArgumentException("safeExplanation must be safe single-line text");
    }
    return stripped;
  }
}
