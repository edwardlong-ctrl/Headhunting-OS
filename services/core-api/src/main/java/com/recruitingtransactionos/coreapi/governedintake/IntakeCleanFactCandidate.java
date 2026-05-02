package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import java.util.Objects;
import java.util.UUID;

public record IntakeCleanFactCandidate(
    String claimFieldName,
    String targetEntityType,
    String targetFieldPath,
    String proposedValue,
    SourceItemId sourceItemId,
    UUID parsedDocumentId,
    UUID parsedDocumentChunkId,
    Integer pageNumber,
    int startOffset,
    int endOffset,
    double confidence,
    VerificationStatus suggestedVerificationStatus,
    RiskTier suggestedRiskTier,
    String entityResolutionStatus,
    String resolvedEntityId,
    String sourceSpanDiscriminator,
    boolean conflictsWithCanonical,
    String rationale,
    String safeSnippet) {

  public IntakeCleanFactCandidate {
    claimFieldName = GovernedIntakeGuards.requireNonBlank(claimFieldName, "claimFieldName");
    targetEntityType = GovernedIntakeGuards.requireNonBlank(targetEntityType, "targetEntityType");
    targetFieldPath = GovernedIntakeGuards.requireNonBlank(targetFieldPath, "targetFieldPath");
    proposedValue = GovernedIntakeGuards.requireNonBlank(proposedValue, "proposedValue");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Objects.requireNonNull(parsedDocumentId, "parsedDocumentId must not be null");
    if (parsedDocumentChunkId == null) {
      throw new IllegalArgumentException("parsedDocumentChunkId must not be null");
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
    if (confidence < 0.0d || confidence > 1.0d) {
      throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
    }
    Objects.requireNonNull(
        suggestedVerificationStatus, "suggestedVerificationStatus must not be null");
    Objects.requireNonNull(suggestedRiskTier, "suggestedRiskTier must not be null");
    entityResolutionStatus =
        GovernedIntakeGuards.requireNonBlank(entityResolutionStatus, "entityResolutionStatus");
    resolvedEntityId =
        GovernedIntakeGuards.optionalNonBlank(resolvedEntityId, "resolvedEntityId");
    sourceSpanDiscriminator = GovernedIntakeGuards.requireNonBlank(
        sourceSpanDiscriminator,
        "sourceSpanDiscriminator");
    rationale = GovernedIntakeGuards.optionalNonBlank(rationale, "rationale");
    safeSnippet = GovernedIntakeGuards.requireNonBlank(safeSnippet, "safeSnippet");
  }
}
