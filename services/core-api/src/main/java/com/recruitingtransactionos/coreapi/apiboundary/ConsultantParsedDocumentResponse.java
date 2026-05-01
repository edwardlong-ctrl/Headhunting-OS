package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantParsedDocumentResponse(
    String sourceItemId,
    String parsedDocumentId,
    String processingStatus,
    String parserName,
    String parserVersion,
    String mediaType,
    boolean ocrRequired,
    int chunkCount,
    String createdAt,
    String completedAt,
    String failureReason) implements ApiSafeResponseBody {

  public ConsultantParsedDocumentResponse {
    sourceItemId = ApiBoundaryContractRules.requireNonBlank(sourceItemId, "sourceItemId");
    parsedDocumentId = ApiBoundaryContractRules.requireNonBlank(parsedDocumentId, "parsedDocumentId");
    processingStatus = ApiBoundaryContractRules.requireNonBlank(processingStatus, "processingStatus");
    parserName = ApiBoundaryContractRules.requireNonBlank(parserName, "parserName");
    parserVersion = ApiBoundaryContractRules.requireNonBlank(parserVersion, "parserVersion");
    mediaType = ApiBoundaryContractRules.requireNonBlank(mediaType, "mediaType");
    if (chunkCount < 0) {
      throw new IllegalArgumentException("chunkCount must be >= 0");
    }
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    completedAt = completedAt == null || completedAt.isBlank() ? null : completedAt.strip();
    failureReason = failureReason == null || failureReason.isBlank() ? null : failureReason.strip();
  }
}
