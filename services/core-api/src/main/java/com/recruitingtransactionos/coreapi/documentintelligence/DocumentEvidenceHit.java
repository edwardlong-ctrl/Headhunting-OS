package com.recruitingtransactionos.coreapi.documentintelligence;

import java.util.Objects;
import java.util.UUID;

public record DocumentEvidenceHit(
    UUID parsedDocumentChunkId,
    int chunkIndex,
    Integer pageNumber,
    int startOffset,
    int endOffset,
    double score,
    String excerpt) {

  public DocumentEvidenceHit {
    Objects.requireNonNull(parsedDocumentChunkId, "parsedDocumentChunkId must not be null");
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
    if (Double.isNaN(score) || Double.isInfinite(score) || score < 0.0d) {
      throw new IllegalArgumentException("score must be a finite non-negative number");
    }
    excerpt = requireNonBlank(excerpt, "excerpt");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
