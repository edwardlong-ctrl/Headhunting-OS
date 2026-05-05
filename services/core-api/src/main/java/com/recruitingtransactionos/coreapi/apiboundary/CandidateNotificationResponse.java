package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.Objects;

public record CandidateNotificationResponse(
    String notificationId,
    String notificationType,
    String status,
    String title,
    String bodySummary,
    String deepLink,
    String entityType,
    String entityId,
    String sourceRef,
    String readAt,
    String dismissedAt,
    String createdAt,
    String updatedAt) implements ApiSafeResponseBody {

  public CandidateNotificationResponse {
    Objects.requireNonNull(notificationId, "notificationId must not be null");
    Objects.requireNonNull(notificationType, "notificationType must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(bodySummary, "bodySummary must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }
}
