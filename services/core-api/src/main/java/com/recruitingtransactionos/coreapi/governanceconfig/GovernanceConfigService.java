package com.recruitingtransactionos.coreapi.governanceconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GovernanceConfigService {

  private final GovernanceConfigPort governanceConfigPort;
  private final ObjectMapper objectMapper;

  public GovernanceConfigService(
      GovernanceConfigPort governanceConfigPort,
      ObjectMapper objectMapper) {
    this.governanceConfigPort = Objects.requireNonNull(
        governanceConfigPort,
        "governanceConfigPort must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  public Optional<GovernanceConfigRecord> find(UUID organizationId, String configType, String configKey) {
    return governanceConfigPort.findByTypeAndKey(organizationId, configType, configKey);
  }

  public List<GovernanceConfigRecord> list(UUID organizationId, String configType) {
    return governanceConfigPort.listByType(organizationId, configType);
  }

  public GovernanceConfigRecord save(
      UUID organizationId,
      String configType,
      String configKey,
      String payloadJson,
      boolean enabled,
      UUID actorUserId) {
    return save(
        organizationId,
        configType,
        configKey,
        payloadJson,
        enabled,
        actorUserId,
        PortalRole.ADMIN);
  }

  public GovernanceConfigRecord save(
      UUID organizationId,
      String configType,
      String configKey,
      String payloadJson,
      boolean enabled,
      UUID actorUserId,
      PortalRole actorRole) {
    validateJson(payloadJson);
    return governanceConfigPort.upsert(
        organizationId,
        configType,
        configKey,
        payloadJson,
        enabled,
        actorUserId,
        actorRole);
  }

  public JsonNode loadPayload(UUID organizationId, String configType, String configKey) {
    return find(organizationId, configType, configKey)
        .filter(GovernanceConfigRecord::enabled)
        .map(record -> parse(record.payloadJson()))
        .orElseGet(objectMapper::createObjectNode);
  }

  private void validateJson(String payloadJson) {
    parse(payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson);
  }

  private JsonNode parse(String payloadJson) {
    try {
      return objectMapper.readTree(payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("invalid_governance_config_json", exception);
    }
  }
}
