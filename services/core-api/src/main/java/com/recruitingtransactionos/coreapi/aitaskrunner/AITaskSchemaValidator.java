package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AITaskSchemaValidator {

  private static final JsonSchemaFactory JSON_SCHEMA_FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  private static final int MAX_SUMMARY_MESSAGES = 5;
  private static final int MAX_MESSAGE_LENGTH = 160;
  private static final int MAX_SUMMARY_LENGTH = 512;

  private final ObjectMapper objectMapper;

  public AITaskSchemaValidator(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  public void validate(String schemaResourcePath, JsonNode payload, String label) {
    Objects.requireNonNull(schemaResourcePath, "schemaResourcePath must not be null");
    Objects.requireNonNull(payload, "payload must not be null");
    Objects.requireNonNull(label, "label must not be null");
    try (InputStream stream = getClass().getResourceAsStream(schemaResourcePath)) {
      if (stream == null) {
        throw new IllegalArgumentException("missing_ai_task_schema");
      }
      JsonSchema schema =
          JSON_SCHEMA_FACTORY.getSchema(new String(stream.readAllBytes(), StandardCharsets.UTF_8),
              InputFormat.JSON);
      Set<ValidationMessage> messages = schema.validate(payload);
      if (!messages.isEmpty()) {
        throw new AITaskSchemaValidationException(label, safeSummary(messages));
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read AI task schema", exception);
    }
  }

  public JsonNode parseJson(String value) {
    try {
      return objectMapper.readTree(value);
    } catch (IOException exception) {
      throw new IllegalArgumentException("provider_output_not_valid_json", exception);
    }
  }

  private static String safeSummary(Set<ValidationMessage> messages) {
    String summary = messages.stream()
        .map(AITaskSchemaValidator::safeMessageDescriptor)
        .map(AITaskSchemaValidator::sanitizeSummaryPart)
        .filter(value -> !value.isBlank())
        .distinct()
        .limit(MAX_SUMMARY_MESSAGES)
        .collect(Collectors.joining("; "));
    if (summary.isBlank()) {
      return "schema validation failed";
    }
    if (summary.length() > MAX_SUMMARY_LENGTH) {
      return summary.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
    }
    return summary;
  }

  private static String safeMessageDescriptor(ValidationMessage message) {
    String code = safeToken(message.getCode());
    String type = safeToken(message.getType());
    String location = safeLocation(message.getInstanceLocation());
    return "code=" + code + ", type=" + type + ", location=" + location;
  }

  private static String sanitizeSummaryPart(String value) {
    if (value == null) {
      return "";
    }
    String normalized = value.replaceAll("\\s+", " ").strip();
    if (normalized.length() > MAX_MESSAGE_LENGTH) {
      return normalized.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
    }
    return normalized;
  }

  private static String safeToken(String value) {
    if (value == null || value.isBlank()) {
      return "unknown";
    }
    return value.replaceAll("[^A-Za-z0-9._:-]", "_");
  }

  private static String safeLocation(Object value) {
    if (value == null) {
      return "/";
    }
    String normalized = value.toString().replaceAll("\\s+", "");
    if (normalized.isBlank()) {
      return "/";
    }
    return normalized.replaceAll("[^A-Za-z0-9_./\\-\\[\\]$]", "_");
  }
}
