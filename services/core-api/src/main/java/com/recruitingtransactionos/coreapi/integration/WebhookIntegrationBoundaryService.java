package com.recruitingtransactionos.coreapi.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

public final class WebhookIntegrationBoundaryService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final InboundIntegrationBoundaryService inboundIntegrationBoundaryService;

  public WebhookIntegrationBoundaryService(
      InboundIntegrationBoundaryService inboundIntegrationBoundaryService) {
    this.inboundIntegrationBoundaryService = Objects.requireNonNull(
        inboundIntegrationBoundaryService, "inboundIntegrationBoundaryService must not be null");
  }

  public WebhookInboundResult acceptInbound(WebhookInboundCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    if (!command.routeOrganizationId().equals(command.eventOrganizationId())) {
      return rejected(WebhookInboundStatus.BLOCKED_CROSS_ORG, null, "cross_org_webhook_blocked");
    }
    if (!"v1".equals(command.schemaVersion()) || !isJsonObject(command.payloadJson())) {
      return rejected(
          WebhookInboundStatus.REJECTED_SCHEMA_INVALID,
          null,
          "webhook_schema_invalid");
    }

    String eventKey = command.idempotencyKey() != null
        ? command.idempotencyKey()
        : "deterministic:" + sha256(command.routeOrganizationId()
            + "|" + command.providerKey()
            + "|" + command.eventType()
            + "|" + command.payloadJson());
    InboundIntegrationResult inboundResult = inboundIntegrationBoundaryService.acceptInbound(
        new InboundIntegrationCommand(
            command.routeOrganizationId(),
            command.actorId(),
            command.eventOrganizationId(),
            command.providerKey(),
            IntegrationChannel.WEBHOOK_EVENT,
            eventKey,
            SourceItemType.OTHER,
            InformationPacketType.MIXED,
            IntendedEntityType.CANDIDATE,
            command.payloadJson(),
            metadataJson(command),
            InboundIntegrationPurpose.SOURCE_INTAKE));
    if (inboundResult.status() != InboundIntegrationStatus.ACCEPTED_FOR_REVIEW) {
      return rejected(
          WebhookInboundStatus.REJECTED_SCHEMA_INVALID,
          eventKey,
          inboundResult.safeStatusCode());
    }
    return new WebhookInboundResult(
        WebhookInboundStatus.ACCEPTED_FOR_REVIEW,
        eventKey,
        inboundResult.sourceItemId(),
        inboundResult.informationPacketId(),
        "accepted_for_review");
  }

  private static WebhookInboundResult rejected(
      WebhookInboundStatus status,
      String eventKey,
      String safeStatusCode) {
    return new WebhookInboundResult(status, eventKey, null, null, safeStatusCode);
  }

  private static boolean isJsonObject(String payloadJson) {
    try {
      JsonNode node = OBJECT_MAPPER.readTree(payloadJson);
      return node != null && node.isObject();
    } catch (Exception exception) {
      return false;
    }
  }

  private static String metadataJson(WebhookInboundCommand command) {
    try {
      return OBJECT_MAPPER.writeValueAsString(Map.of(
          "eventType", command.eventType(),
          "schemaVersion", "v1"));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize webhook metadata", exception);
    }
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 not available", exception);
    }
  }
}
