package com.recruitingtransactionos.coreapi.governedintake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

final class GovernedIntakeGuards {

  static final int METADATA_JSON_MAX_LENGTH = 8192;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private GovernedIntakeGuards() {
  }

  static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  static String optionalNonBlank(String value, String name) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  static String metadataJson(String value) {
    if (value == null) {
      return "{}";
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException("metadataJson must not be blank");
    }
    String stripped = value.strip();
    if (stripped.length() > METADATA_JSON_MAX_LENGTH) {
      throw new IllegalArgumentException(
          "metadataJson must be " + METADATA_JSON_MAX_LENGTH + " characters or fewer");
    }
    try {
      JsonNode node = OBJECT_MAPPER.readTree(stripped);
      if (!node.isObject()) {
        throw new IllegalArgumentException("metadataJson must be a JSON object");
      }
      return OBJECT_MAPPER.writeValueAsString(node);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("metadataJson must be valid JSON", exception);
    }
  }
}
