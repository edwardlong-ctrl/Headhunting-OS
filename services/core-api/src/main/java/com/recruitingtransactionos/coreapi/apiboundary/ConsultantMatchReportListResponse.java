package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantMatchReportListResponse(
    List<ConsultantMatchReportResponse> reports
) implements ApiSafeResponseBody {}
