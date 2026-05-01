package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;

public record ConsultantMatchReportResponse(
    String matchReportId,
    int finalScore,
    boolean capApplied,
    String capReason,
    String confidence
) implements ApiSafeResponseBody {}
