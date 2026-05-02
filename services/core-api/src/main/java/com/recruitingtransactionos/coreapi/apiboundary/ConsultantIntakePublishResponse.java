package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantIntakePublishResponse(
    String informationPacketId,
    int canonicalWriteCount,
    List<String> canonicalWriteStatuses,
    List<String> directWrites) implements ApiSafeResponseBody {}
