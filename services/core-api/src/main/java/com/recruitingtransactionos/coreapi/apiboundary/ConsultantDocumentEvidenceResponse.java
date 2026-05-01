package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantDocumentEvidenceResponse(
    String sourceItemId,
    String parsedDocumentId,
    String processingStatus,
    String query,
    int totalHits,
    List<Hit> hits) implements ApiSafeResponseBody {

  public ConsultantDocumentEvidenceResponse {
    sourceItemId = ApiBoundaryContractRules.requireNonBlank(sourceItemId, "sourceItemId");
    parsedDocumentId = ApiBoundaryContractRules.requireNonBlank(parsedDocumentId, "parsedDocumentId");
    processingStatus = ApiBoundaryContractRules.requireNonBlank(processingStatus, "processingStatus");
    query = query == null ? null : query.strip();
    if (totalHits < 0) {
      throw new IllegalArgumentException("totalHits must be >= 0");
    }
    hits = ApiBoundaryContractRules.requireNonNullList(hits, "hits");
  }

  public record Hit(
      String chunkId,
      int chunkIndex,
      Integer pageNumber,
      int startOffset,
      int endOffset,
      double score,
      String excerpt) {

    public Hit {
      chunkId = ApiBoundaryContractRules.requireNonBlank(chunkId, "chunkId");
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
      excerpt = sanitizeExcerpt(excerpt);
    }

    private static String sanitizeExcerpt(String excerpt) {
      String value = ApiBoundaryContractRules.requireNonBlank(excerpt, "excerpt");
      value = value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").replaceAll("\\s+", " ").trim();
      if (value.length() > 280) {
        return value.substring(0, 280).strip();
      }
      return value;
    }
  }
}
