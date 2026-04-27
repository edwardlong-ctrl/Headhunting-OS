package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.util.Objects;
import java.util.UUID;

public record ReviewEventSourceReference(
    ReviewEventId reviewEventId,
    UUID organizationId,
    EntityRef targetEntity,
    String targetFieldPath,
    ClaimId claimId,
    SourceSpanRef sourceSpanReference,
    ReviewDecision decision,
    RiskTier riskTier) {

  public ReviewEventSourceReference {
    Objects.requireNonNull(reviewEventId, "reviewEventId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = requireNonBlank(targetFieldPath, "targetFieldPath");
    Objects.requireNonNull(claimId, "claimId must not be null");
    Objects.requireNonNull(sourceSpanReference, "sourceSpanReference must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    Objects.requireNonNull(riskTier, "riskTier must not be null");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
