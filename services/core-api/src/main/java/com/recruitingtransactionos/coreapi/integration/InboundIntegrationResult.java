package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

public record InboundIntegrationResult(
    InboundIntegrationStatus status,
    UUID sourceItemId,
    UUID informationPacketId,
    boolean confirmedFactWritten,
    String safeStatusCode) {}
