package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantIntakeReviewResponse(
    String extractionRunId,
    String informationPacketId,
    String intendedEntityType,
    int cleanFactCount,
    List<ConsultantCleanFactResponse> cleanFacts) implements ApiSafeResponseBody {}
