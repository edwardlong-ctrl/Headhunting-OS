package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.util.Objects;
import java.util.UUID;

public record ClaimLedgerSourceReference(
    ClaimId claimId,
    UUID organizationId,
    EntityRef targetEntity,
    String targetFieldPath,
    SourceSpanRef sourceSpanReference) {

  public ClaimLedgerSourceReference {
    Objects.requireNonNull(claimId, "claimId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = requireNonBlank(targetFieldPath, "targetFieldPath");
    Objects.requireNonNull(sourceSpanReference, "sourceSpanReference must not be null");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
