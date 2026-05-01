package com.recruitingtransactionos.coreapi.documentintelligence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ParsedDocumentSpan(
    UUID parsedDocumentSpanId,
    UUID organizationId,
    UUID parsedDocumentId,
    UUID parsedDocumentChunkId,
    int spanIndex,
    Integer pageNumber,
    int startOffset,
    int endOffset,
    Instant createdAt) {

  public ParsedDocumentSpan {
    Objects.requireNonNull(parsedDocumentSpanId, "parsedDocumentSpanId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(parsedDocumentId, "parsedDocumentId must not be null");
    Objects.requireNonNull(parsedDocumentChunkId, "parsedDocumentChunkId must not be null");
    if (spanIndex < 0) {
      throw new IllegalArgumentException("spanIndex must be >= 0");
    }
    if (pageNumber != null && pageNumber < 1) {
      throw new IllegalArgumentException("pageNumber must be >= 1 when present");
    }
    if (startOffset < 0) {
      throw new IllegalArgumentException("startOffset must be >= 0");
    }
    if (endOffset <= startOffset) {
      throw new IllegalArgumentException("endOffset must be > startOffset");
    }
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}
