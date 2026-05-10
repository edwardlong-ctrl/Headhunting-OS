package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

public record WebhookDispatchCommand(
    UUID organizationId,
    UUID actorId,
    String eventType,
    String payloadJson,
    String idempotencyKey) {

  static WebhookDispatchCommand fromOutbound(OutboundIntegrationCommand command) {
    return new WebhookDispatchCommand(
        command.organizationId(),
        command.actorId(),
        command.payloadKind().name(),
        command.safeSummaryPayload(),
        command.idempotencyKey());
  }
}
