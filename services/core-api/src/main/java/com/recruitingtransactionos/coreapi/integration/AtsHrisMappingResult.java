package com.recruitingtransactionos.coreapi.integration;

public record AtsHrisMappingResult(
    AtsHrisMappingStatus status,
    IntegrationProviderStatus providerStatus,
    String safeStatusCode) {}
