package com.recruitingtransactionos.coreapi.integration;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AtsHrisMappingCommand(
    UUID organizationId,
    UUID actorId,
    String providerKey,
    String objectType,
    Map<String, String> fieldMappings,
    boolean confirmedFactWriteRequested) {

  public AtsHrisMappingCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    providerKey = requireNonBlank(providerKey, "providerKey");
    objectType = requireNonBlank(objectType, "objectType");
    fieldMappings = Map.copyOf(Objects.requireNonNull(fieldMappings, "fieldMappings must not be null"));
    if (fieldMappings.isEmpty()) {
      throw new IllegalArgumentException("fieldMappings must not be empty");
    }
    fieldMappings.forEach((source, target) -> {
      requireNonBlank(source, "fieldMappings source");
      requireNonBlank(target, "fieldMappings target");
    });
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
