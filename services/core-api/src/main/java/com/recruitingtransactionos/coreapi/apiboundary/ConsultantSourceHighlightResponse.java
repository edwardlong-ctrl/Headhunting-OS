package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantSourceHighlightResponse(
    String sourceItemId,
    String parsedDocumentId,
    String parsedDocumentChunkId,
    Integer pageNumber,
    int startOffset,
    int endOffset,
    String safeSnippet,
    String locator) {}
