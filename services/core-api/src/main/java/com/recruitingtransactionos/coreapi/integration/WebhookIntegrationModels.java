package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

enum WebhookInboundStatus {
  ACCEPTED_FOR_REVIEW,
  BLOCKED_CROSS_ORG,
  REJECTED_SCHEMA_INVALID
}

record WebhookInboundResult(
    WebhookInboundStatus status,
    String eventKey,
    UUID sourceItemId,
    UUID informationPacketId,
    String safeStatusCode) {}
