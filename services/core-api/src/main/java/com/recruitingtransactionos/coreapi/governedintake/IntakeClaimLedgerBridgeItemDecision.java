package com.recruitingtransactionos.coreapi.governedintake;

public record IntakeClaimLedgerBridgeItemDecision(
    String fieldName,
    SourceItemId sourceItemId,
    String reason) {

  public IntakeClaimLedgerBridgeItemDecision {
    fieldName = GovernedIntakeGuards.requireNonBlank(fieldName, "fieldName");
    reason = GovernedIntakeGuards.requireNonBlank(reason, "reason");
  }
}
