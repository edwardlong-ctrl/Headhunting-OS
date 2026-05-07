package com.recruitingtransactionos.coreapi.governanceconfig;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GovernanceConfigRecord(
    UUID governanceConfigId,
    UUID organizationId,
    String configType,
    String configKey,
    String payloadJson,
    boolean enabled,
    UUID createdByUserId,
    UUID updatedByUserId,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public GovernanceConfigRecord {
    Objects.requireNonNull(governanceConfigId, "governanceConfigId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    configType = requireNonBlank(configType, "configType");
    configKey = requireNonBlank(configKey, "configKey");
    payloadJson = payloadJson == null ? "{}" : payloadJson;
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
