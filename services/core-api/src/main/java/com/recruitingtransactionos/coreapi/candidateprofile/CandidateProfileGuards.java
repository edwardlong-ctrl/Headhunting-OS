package com.recruitingtransactionos.coreapi.candidateprofile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

final class CandidateProfileGuards {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private CandidateProfileGuards() {
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

  static String requireJsonValue(String value, String name) {
    String normalized = requireNonBlank(value, name);
    try {
      JsonNode node = OBJECT_MAPPER.readTree(normalized);
      if (node.isNull()) {
        throw new IllegalArgumentException(name + " must not be JSON null");
      }
      return OBJECT_MAPPER.writeValueAsString(node);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException(name + " must be valid JSON", exception);
    }
  }
}
