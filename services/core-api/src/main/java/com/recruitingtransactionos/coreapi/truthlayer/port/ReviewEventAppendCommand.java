package com.recruitingtransactionos.coreapi.truthlayer.port;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public record ReviewEventAppendCommand(
    UUID organizationId,
    UUID reviewerId,
    EntityRef targetEntity,
    String targetFieldPath,
    RiskTier riskTier,
    ReviewDecision decision,
    boolean bulkApproval,
    String reason,
    Duration reviewDuration,
    ClaimId claimId,
    SourceSpanRef sourceSpanReference) {

  public ReviewEventAppendCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(reviewerId, "reviewerId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = PortContractGuards.requireNonBlank(targetFieldPath, "targetFieldPath");
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    reason = PortContractGuards.requireNonBlank(reason, "reason");
    reviewDuration = PortContractGuards.requireNonNegative(reviewDuration, "reviewDuration");
  }
}
