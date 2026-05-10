package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

public record OutboundIntegrationResult(
    OutboundIntegrationStatus status,
    IntegrationProviderStatus providerStatus,
    String providerKey,
    String safeStatusCode,
    UUID auditId,
    boolean sensitiveDataSent) {}
