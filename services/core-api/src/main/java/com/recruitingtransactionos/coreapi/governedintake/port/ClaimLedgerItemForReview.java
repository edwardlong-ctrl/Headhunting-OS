package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.util.Objects;
import java.util.UUID;

public record ClaimLedgerItemForReview(
    ClaimId claimLedgerItemId,
    UUID organizationId,
    EntityRef targetEntity,
    String targetFieldPath,
    SourceSpanRef sourceSpanReference,
    VerificationStatus verificationStatus,
    ClientShareability clientShareability) {

  public ClaimLedgerItemForReview {
    Objects.requireNonNull(claimLedgerItemId, "claimLedgerItemId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = optionalNonBlank(targetFieldPath, "targetFieldPath");
    Objects.requireNonNull(sourceSpanReference, "sourceSpanReference must not be null");
    Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
    Objects.requireNonNull(clientShareability, "clientShareability must not be null");
  }

  private static String optionalNonBlank(String value, String name) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
