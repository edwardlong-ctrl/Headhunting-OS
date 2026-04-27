package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record IntakeClaimLedgerBridgeResult(
    UUID organizationId,
    IntakeExtractionRunId extractionRunId,
    InformationPacketId informationPacketId,
    List<ClaimId> appendedClaimIds,
    List<ClaimId> existingClaimIds,
    List<IntakeClaimLedgerBridgeItemDecision> skippedItems,
    List<IntakeClaimLedgerBridgeItemDecision> blockedItems,
    IntakeClaimLedgerBridgeStatus bridgeStatus,
    String summary) {

  public IntakeClaimLedgerBridgeResult {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(extractionRunId, "extractionRunId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    appendedClaimIds = List.copyOf(Objects.requireNonNull(
        appendedClaimIds,
        "appendedClaimIds must not be null"));
    existingClaimIds = List.copyOf(Objects.requireNonNull(
        existingClaimIds,
        "existingClaimIds must not be null"));
    skippedItems = List.copyOf(Objects.requireNonNull(
        skippedItems,
        "skippedItems must not be null"));
    blockedItems = List.copyOf(Objects.requireNonNull(
        blockedItems,
        "blockedItems must not be null"));
    Objects.requireNonNull(bridgeStatus, "bridgeStatus must not be null");
    summary = GovernedIntakeGuards.requireNonBlank(summary, "summary");
  }
}
