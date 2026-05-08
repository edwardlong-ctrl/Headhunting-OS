package com.recruitingtransactionos.coreapi.observability;

public record ObservabilityReviewEventResponse(
    String reviewEventId,
    String reviewerUserId,
    String targetEntityType,
    String targetEntityId,
    String fieldPath,
    String riskTier,
    String decision,
    String status,
    String claimLedgerItemId,
    String sourceSpanRef,
    String reason,
    String createdAt) {}
