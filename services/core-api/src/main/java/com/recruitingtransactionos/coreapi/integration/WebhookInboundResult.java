package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

public record WebhookInboundResult(
    WebhookInboundStatus status,
    String eventKey,
    UUID sourceItemId,
    UUID informationPacketId,
    String safeStatusCode) {}
