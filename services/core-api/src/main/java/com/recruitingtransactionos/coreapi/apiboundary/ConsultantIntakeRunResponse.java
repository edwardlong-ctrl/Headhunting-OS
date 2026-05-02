package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantIntakeRunResponse(
    String extractionRunId,
    String informationPacketId,
    String intendedEntityType,
    String status,
    String outputSchemaVersion,
    int cleanFactCount,
    List<String> aiTaskRunIds) implements ApiSafeResponseBody {}
