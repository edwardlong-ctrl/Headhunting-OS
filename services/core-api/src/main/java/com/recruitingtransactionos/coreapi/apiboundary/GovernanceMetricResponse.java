package com.recruitingtransactionos.coreapi.apiboundary;

public record GovernanceMetricResponse(
    String key,
    String label,
    String value,
    String severity,
    String helperText) {}
