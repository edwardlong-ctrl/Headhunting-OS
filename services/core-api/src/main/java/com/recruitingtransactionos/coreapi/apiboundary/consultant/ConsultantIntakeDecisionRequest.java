package com.recruitingtransactionos.coreapi.apiboundary.consultant;

public record ConsultantIntakeDecisionRequest(
    String decision,
    String riskTier,
    String reason,
    Boolean bulkFlag) {}
