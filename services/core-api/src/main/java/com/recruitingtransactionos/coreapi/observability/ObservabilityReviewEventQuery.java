package com.recruitingtransactionos.coreapi.observability;

import java.time.Instant;
import java.util.UUID;

public record ObservabilityReviewEventQuery(
    UUID organizationId,
    String targetEntityType,
    UUID targetEntityId,
    String status,
    UUID claimLedgerItemId,
    UUID reviewerUserId,
    Instant createdFrom,
    Instant createdTo,
    int limit,
    int offset) {}
