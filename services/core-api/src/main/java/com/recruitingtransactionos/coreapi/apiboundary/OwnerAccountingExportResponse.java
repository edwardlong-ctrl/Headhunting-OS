package com.recruitingtransactionos.coreapi.apiboundary;

public record OwnerAccountingExportResponse(
    String format,
    String process,
    String disclaimer,
    String generatedAt,
    String content) implements ApiSafeResponseBody {}
