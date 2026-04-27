package com.recruitingtransactionos.coreapi.truthlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CanonicalWriteGate {

  public CanonicalWriteDecision decide(CanonicalWriteRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    ClaimInput claim = request.claim();
    List<String> blockReasons = new ArrayList<>();
    List<String> reviewReasons = new ArrayList<>();
    List<String> allowReasons = new ArrayList<>();

    if (claim.verificationStatus() == VerificationStatus.SYSTEM_INFERENCE
        || claim.type() == ClaimType.INFERENCE) {
      blockReasons.add("system_inference_cannot_be_canonical_fact");
    }

    if (claim.verificationStatus() == VerificationStatus.AI_EXTRACTED) {
      blockReasons.add("ai_extracted_claim_cannot_be_canonical_fact");
    }

    if (claim.type() == ClaimType.INTENT
        && claim.assertionStrength() == AssertionStrength.WEAK_SIGNAL
        && request.targetVerificationStatus() == VerificationStatus.CANDIDATE_CONFIRMED) {
      blockReasons.add("weak_signal_intent_cannot_become_confirmed_fact");
    }

    if (claim.type() == ClaimType.INTENT
        && claim.assertionStrength() == AssertionStrength.IMPLIED
        && request.targetVerificationStatus() == VerificationStatus.CANDIDATE_CONFIRMED) {
      blockReasons.add("implied_intent_cannot_become_confirmed_fact");
    }

    if (claim.bulkApproved()
        && request.targetVerificationStatus() == VerificationStatus.CANDIDATE_CONFIRMED) {
      blockReasons.add("bulk_approve_cannot_create_candidate_confirmed");
    }

    if (claim.bulkApproved()
        && request.targetVerificationStatus() == VerificationStatus.EXTERNAL_VERIFIED) {
      blockReasons.add("bulk_approve_cannot_create_external_verified");
    }

    if (claim.bulkApproved() && request.targetRiskTier() == RiskTier.T4_TRANSACTION_LEGAL) {
      blockReasons.add("t4_transaction_legal_cannot_use_bulk_approval");
    }

    if (request.targetRiskTier() == RiskTier.T1_LOW
        && isVerifiedFactTarget(request.targetVerificationStatus())) {
      blockReasons.add("t1_low_risk_cannot_create_verified_fact");
    }

    if (request.clientVisible()) {
      if (claim.clientShareability() == ClientShareability.INTERNAL_ONLY) {
        blockReasons.add("internal_only_claim_cannot_be_client_visible");
      }
      if (claim.clientShareability() == ClientShareability.FORBIDDEN) {
        blockReasons.add("forbidden_claim_cannot_be_client_visible");
      }
      if (claim.clientShareability() == ClientShareability.CONSENT_REQUIRED
          && !request.explicitReviewApproved()) {
        reviewReasons.add("client_visible_claim_requires_consent_or_review");
      }
    }

    if (request.conflictsWithCanonical()
        || claim.verificationStatus() == VerificationStatus.CONFLICTING
        || claim.assertionStrength() == AssertionStrength.CONTRADICTION) {
      if (!request.explicitReviewApproved()) {
        reviewReasons.add("conflicting_claim_requires_explicit_review");
      }
    }

    if (requiresExplicitReview(request.targetRiskTier()) && !request.explicitReviewApproved()) {
      reviewReasons.add("high_risk_write_requires_explicit_review_approval");
    }

    if (request.targetRiskTier() == RiskTier.T1_LOW
        && request.targetVerificationStatus() == VerificationStatus.HUMAN_ACKNOWLEDGED) {
      allowReasons.add("low_risk_human_acknowledged_write_allowed");
    } else {
      allowReasons.add("canonical_write_gate_passed");
    }

    if (!blockReasons.isEmpty()) {
      return new CanonicalWriteDecision(CanonicalWriteDecisionType.BLOCK, blockReasons);
    }
    if (!reviewReasons.isEmpty()) {
      return new CanonicalWriteDecision(CanonicalWriteDecisionType.REQUIRE_REVIEW, reviewReasons);
    }
    return new CanonicalWriteDecision(CanonicalWriteDecisionType.ALLOW, allowReasons);
  }

  private static boolean isVerifiedFactTarget(VerificationStatus targetStatus) {
    return targetStatus == VerificationStatus.CANDIDATE_CONFIRMED
        || targetStatus == VerificationStatus.EXTERNAL_VERIFIED;
  }

  private static boolean requiresExplicitReview(RiskTier riskTier) {
    return riskTier == RiskTier.T3_HIGH || riskTier == RiskTier.T4_TRANSACTION_LEGAL;
  }
}
