package com.recruitingtransactionos.coreapi.truthlayer.port;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

final class PortContractGuards {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int MAX_SAFE_FAILURE_REASON_LENGTH = 512;
  private static final int MAX_SAFE_TRACE_REF_LENGTH = 255;
  private static final Pattern STACK_TRACE_MARKER =
      Pattern.compile("(?i)(\\R|\\bat\\s+[\\w.$]+\\(|exception|stacktrace|caused by)");
  private static final Pattern SAFE_REASON_CODE = Pattern.compile("[a-z0-9_.-]+");

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

  static BigDecimal requireNonNegative(BigDecimal value, String name) {
    if (value != null && value.signum() < 0) {
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

  static String safeReasonCode(String value, String name) {
    if (value == null) {
      return null;
    }
    String normalized = value.strip().toLowerCase();
    if (normalized.isBlank() || !SAFE_REASON_CODE.matcher(normalized).matches()) {
      throw new IllegalArgumentException(name + " must be a safe reason code");
    }
    return normalized;
  }

  static String safeTraceRef(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.strip();
    if (normalized.isBlank()
        || normalized.length() > MAX_SAFE_TRACE_REF_LENGTH
        || STACK_TRACE_MARKER.matcher(normalized).find()) {
      throw new IllegalArgumentException("traceRef must be safe single-line text");
    }
    return normalized;
  }

  static String normalizedJsonObject(String value, String name, boolean defaultEmptyObject) {
    if (value == null) {
      return defaultEmptyObject ? "{}" : null;
    }
    JsonNode node = parseJson(value, name);
    if (!node.isObject()) {
      throw new IllegalArgumentException(name + " must be a JSON object");
    }
    return node.toString();
  }

  static String normalizedJsonArray(String value, String name, boolean defaultEmptyArray) {
    if (value == null) {
      return defaultEmptyArray ? "[]" : null;
    }
    JsonNode node = parseJson(value, name);
    if (!node.isArray()) {
      throw new IllegalArgumentException(name + " must be a JSON array");
    }
    return node.toString();
  }

  static String normalizedJsonValue(String value, String name, boolean allowNull) {
    if (value == null) {
      return allowNull ? null : "{}";
    }
    return parseJson(value, name).toString();
  }

  private static JsonNode parseJson(String value, String name) {
    String normalized = requireNonBlank(value, name).strip();
    try {
      return OBJECT_MAPPER.readTree(normalized);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException(name + " must be valid JSON", exception);
    }
  }
}
