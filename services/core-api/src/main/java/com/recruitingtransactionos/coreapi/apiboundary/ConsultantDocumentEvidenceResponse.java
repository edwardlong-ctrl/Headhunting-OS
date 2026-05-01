package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantDocumentEvidenceResponse(
    String processingStatus,
    String query,
    int totalHits,
    List<Hit> hits) implements ApiSafeResponseBody {

  public ConsultantDocumentEvidenceResponse {
    processingStatus = ApiBoundaryContractRules.requireNonBlank(processingStatus, "processingStatus");
    query = ApiBoundaryContractRules.sanitizeExternalText(query, null);
    if (totalHits < 0) {
      throw new IllegalArgumentException("totalHits must be >= 0");
    }
    hits = ApiBoundaryContractRules.requireNonNullList(hits, "hits");
  }

  public record Hit(
      int chunkIndex,
      Integer pageNumber,
      int startOffset,
      int endOffset,
      double score,
      String locator) {

    public Hit {
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
      locator = sanitizeLocator(locator);
    }

    private static String sanitizeLocator(String locator) {
      return ApiBoundaryContractRules.requireApiSafeExternalText(locator, "locator");
    }
  }
}
