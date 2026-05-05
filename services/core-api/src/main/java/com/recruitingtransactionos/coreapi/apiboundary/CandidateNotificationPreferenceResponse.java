package com.recruitingtransactionos.coreapi.apiboundary;

public record CandidateNotificationPreferenceResponse(
    boolean inAppEnabled,
    boolean emailEnabled,
    boolean smsEnabled,
    boolean reminderEnabled,
    boolean unsubscribed,
    String updatedAt) implements ApiSafeResponseBody {}
