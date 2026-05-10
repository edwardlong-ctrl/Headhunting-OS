package com.recruitingtransactionos.coreapi.integration;

enum AtsHrisMappingStatus {
  ACCEPTED_FOR_REVIEW_MAPPING,
  BLOCKED_CONFIRMED_FACT_WRITE
}

record AtsHrisMappingResult(
    AtsHrisMappingStatus status,
    IntegrationProviderStatus providerStatus,
    String safeStatusCode) {}
