package com.recruitingtransactionos.coreapi.governedintake;

import java.util.Objects;

public record IntakeExtractionSourceSnapshot(
    SourceItemId sourceItemId,
    SourceItemType sourceType,
    String title,
    String contentHash,
    String externalRef) {

  public IntakeExtractionSourceSnapshot {
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    title = GovernedIntakeGuards.optionalNonBlank(title, "title");
    contentHash = GovernedIntakeGuards.optionalNonBlank(contentHash, "contentHash");
    externalRef = GovernedIntakeGuards.optionalNonBlank(externalRef, "externalRef");
  }
}
