package com.recruitingtransactionos.coreapi.apiboundary;

public record GovernanceConfigUpdateResponse(
    String sectionKey,
    String status,
    String updatedAt) implements ApiSafeResponseBody {}
