package com.recruitingtransactionos.coreapi.documentintelligence;

import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ParsedDocument(
    UUID parsedDocumentId,
    UUID organizationId,
    SourceItemId sourceItemId,
    DocumentProcessingStatus processingStatus,
    String parserName,
    String parserVersion,
    String mediaType,
    String contentHash,
    String language,
    boolean ocrRequired,
    Instant createdAt,
    Optional<Instant> completedAt,
    Optional<String> failureReason) {

  public ParsedDocument {
    Objects.requireNonNull(parsedDocumentId, "parsedDocumentId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Objects.requireNonNull(processingStatus, "processingStatus must not be null");
    parserName = requireNonBlank(parserName, "parserName");
    parserVersion = requireNonBlank(parserVersion, "parserVersion");
    mediaType = requireNonBlank(mediaType, "mediaType");
    contentHash = optionalNonBlank(contentHash);
    language = optionalNonBlank(language);
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    completedAt = completedAt == null ? Optional.empty() : completedAt;
    failureReason = failureReason == null ? Optional.empty() : failureReason.map(
        reason -> requireNonBlank(reason, "failureReason"));
    if (processingStatus == DocumentProcessingStatus.SUCCEEDED && completedAt.isEmpty()) {
      throw new IllegalArgumentException("succeeded parsed document requires completedAt");
    }
    if (processingStatus == DocumentProcessingStatus.FAILED && failureReason.isEmpty()) {
      throw new IllegalArgumentException("failed parsed document requires failureReason");
    }
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }

  private static String optionalNonBlank(String value) {
    if (value == null) {
      return null;
    }
    return requireNonBlank(value, "optionalValue");
  }
}
