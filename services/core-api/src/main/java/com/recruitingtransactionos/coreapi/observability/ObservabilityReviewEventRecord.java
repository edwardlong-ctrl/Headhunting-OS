package com.recruitingtransactionos.coreapi.observability;

import java.time.Instant;
import java.util.UUID;

public record ObservabilityReviewEventRecord(
    UUID reviewEventId,
    UUID reviewerUserId,
    String targetEntityType,
    UUID targetEntityId,
    String fieldPath,
    String riskTier,
    String decision,
    String status,
    UUID claimLedgerItemId,
    String sourceSpanRef,
    String reason,
    Instant createdAt) {}
