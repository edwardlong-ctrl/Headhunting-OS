package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UnlockDecision(
    String unlockDecisionRef,
    UUID organizationId,
    String candidateRef,
    String candidateProfileRef,
    String jobRef,
    String clientRef,
    DisclosureLevel requestedDisclosureLevel,
    UnlockDecisionStatus status,
    DisclosureReviewStatus reviewStatus,
    RiskTier riskTier,
    ActorRef approvedBy,
    Instant decidedAt) {

  public UnlockDecision {
    requireNonBlank(unlockDecisionRef, "unlockDecisionRef");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    requireNonBlank(candidateRef, "candidateRef");
    requireNonBlank(candidateProfileRef, "candidateProfileRef");
    requireNonBlank(jobRef, "jobRef");
    requireNonBlank(clientRef, "clientRef");
    Objects.requireNonNull(requestedDisclosureLevel, "requestedDisclosureLevel must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(reviewStatus, "reviewStatus must not be null");
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    Objects.requireNonNull(approvedBy, "approvedBy must not be null");
    Objects.requireNonNull(decidedAt, "decidedAt must not be null");
  }

  public boolean isHumanApprovedFor(DisclosureLevel disclosureLevel) {
    Objects.requireNonNull(disclosureLevel, "disclosureLevel must not be null");
    return status == UnlockDecisionStatus.APPROVED
        && reviewStatus == DisclosureReviewStatus.HUMAN_APPROVED
        && requestedDisclosureLevel == disclosureLevel
        && isHumanActor(approvedBy.role());
  }

  private static boolean isHumanActor(ActorRole role) {
    return role == ActorRole.OWNER
        || role == ActorRole.CONSULTANT
        || role == ActorRole.CLIENT
        || role == ActorRole.CANDIDATE
        || role == ActorRole.ADMIN;
  }

  private static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
