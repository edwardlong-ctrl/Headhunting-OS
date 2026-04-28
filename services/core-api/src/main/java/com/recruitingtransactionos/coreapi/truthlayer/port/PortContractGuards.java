package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

final class PortContractGuards {

  private static final int MAX_SAFE_FAILURE_REASON_LENGTH = 512;
  private static final Pattern STACK_TRACE_MARKER =
      Pattern.compile("(?i)(\\R|\\bat\\s+[\\w.$]+\\(|exception|stacktrace|caused by)");

  private PortContractGuards() {
  }

  static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  static String requireNonBlankMaxLength(String value, String name, int maxLength) {
    String normalized = requireNonBlank(value, name).strip();
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(name + " must be " + maxLength + " characters or fewer");
    }
    return normalized;
  }

  static UUID requireUuidWireValue(String value, String name, int maxLength) {
    String normalized = requireNonBlankMaxLength(value, name, maxLength);
    try {
      return UUID.fromString(normalized);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(name + " must be a valid UUID", exception);
    }
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

  static String safeFailureReason(String value, AITaskRunStatus status) {
    if (status != AITaskRunStatus.FAILED && value == null) {
      return null;
    }
    if (status == AITaskRunStatus.FAILED && (value == null || value.isBlank())) {
      throw new IllegalArgumentException("failureReason must be present for failed AI task runs");
    }
    if (value == null) {
      return null;
    }
    String normalized = value.strip();
    if (normalized.isBlank() || normalized.length() > MAX_SAFE_FAILURE_REASON_LENGTH
        || STACK_TRACE_MARKER.matcher(normalized).find()) {
      throw new IllegalArgumentException("failureReason must be a safe single-line reason");
    }
    return normalized;
  }
}
