package com.recruitingtransactionos.coreapi.apiboundary;

public record GovernanceItemResponse(
    String primaryText,
    String secondaryText,
    String status,
    String detail,
    String route) {}
