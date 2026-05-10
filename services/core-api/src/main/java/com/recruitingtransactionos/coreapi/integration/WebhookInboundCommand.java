package com.recruitingtransactionos.coreapi.integration;

import java.util.Objects;
import java.util.UUID;

public record WebhookInboundCommand(
    UUID routeOrganizationId,
    UUID eventOrganizationId,
    UUID actorId,
    String providerKey,
    String eventType,
    String schemaVersion,
    String payloadJson,
    String idempotencyKey) {

  public WebhookInboundCommand {
    Objects.requireNonNull(routeOrganizationId, "routeOrganizationId must not be null");
    Objects.requireNonNull(eventOrganizationId, "eventOrganizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    providerKey = requireNonBlank(providerKey, "providerKey");
    eventType = requireNonBlank(eventType, "eventType");
    schemaVersion = requireNonBlank(schemaVersion, "schemaVersion");
    payloadJson = requireNonBlank(payloadJson, "payloadJson");
    idempotencyKey = optionalNonBlank(idempotencyKey, "idempotencyKey");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static String optionalNonBlank(String value, String name) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
