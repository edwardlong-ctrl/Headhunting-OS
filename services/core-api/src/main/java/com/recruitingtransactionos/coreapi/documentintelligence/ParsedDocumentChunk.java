package com.recruitingtransactionos.coreapi.documentintelligence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ParsedDocumentChunk(
    UUID parsedDocumentChunkId,
    UUID organizationId,
    UUID parsedDocumentId,
    int chunkIndex,
    Integer pageNumber,
    int startOffset,
    int endOffset,
    String chunkText,
    Instant createdAt) {

  public ParsedDocumentChunk {
    Objects.requireNonNull(parsedDocumentChunkId, "parsedDocumentChunkId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(parsedDocumentId, "parsedDocumentId must not be null");
    if (chunkIndex < 0) {
      throw new IllegalArgumentException("chunkIndex must be >= 0");
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
    chunkText = requireNonBlank(chunkText, "chunkText");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
