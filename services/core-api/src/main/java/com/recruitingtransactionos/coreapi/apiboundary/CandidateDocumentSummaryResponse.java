package com.recruitingtransactionos.coreapi.apiboundary;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CandidateDocumentSummaryResponse(
    UUID documentId,
    String documentType,
    String title,
    String status,
    long fileSizeBytes,
    String mimeType,
    Instant uploadedAt) implements ApiSafeResponseBody {

  public CandidateDocumentSummaryResponse {
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(documentType, "documentType must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
  }
}
