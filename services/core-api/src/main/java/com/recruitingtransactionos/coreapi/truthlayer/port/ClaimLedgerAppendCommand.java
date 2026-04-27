package com.recruitingtransactionos.coreapi.truthlayer.port;

import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import java.util.Objects;
import java.util.UUID;

public record ClaimLedgerAppendCommand(
    UUID organizationId,
    EntityRef targetEntity,
    String targetFieldPath,
    ClaimType claimType,
    AssertionStrength assertionStrength,
    SourceSpanRef sourceSpanReference,
    ActorRole speaker,
    VerificationStatus verificationStatus,
    ClientShareability clientShareability,
    UUID sourceItemId,
    AITaskRunId aiTaskRunId) {

  public ClaimLedgerAppendCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = PortContractGuards.requireNonBlank(targetFieldPath, "targetFieldPath");
    Objects.requireNonNull(claimType, "claimType must not be null");
    Objects.requireNonNull(assertionStrength, "assertionStrength must not be null");
    Objects.requireNonNull(sourceSpanReference, "sourceSpanReference must not be null");
    Objects.requireNonNull(speaker, "speaker must not be null");
    Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
    Objects.requireNonNull(clientShareability, "clientShareability must not be null");
  }
}
