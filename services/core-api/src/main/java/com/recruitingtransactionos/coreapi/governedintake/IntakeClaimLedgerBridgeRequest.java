package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.Objects;
import java.util.UUID;

public record IntakeClaimLedgerBridgeRequest(
    UUID organizationId,
    IntakeExtractionRunId extractionRunId,
    ActorRole requestedByActorType,
    UUID requestedByActorId,
    IntakeClaimLedgerBridgePolicy bridgePolicy,
    UUID correlationId) {

  public IntakeClaimLedgerBridgeRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(extractionRunId, "extractionRunId must not be null");
    Objects.requireNonNull(requestedByActorType, "requestedByActorType must not be null");
    Objects.requireNonNull(bridgePolicy, "bridgePolicy must not be null");
  }
}
