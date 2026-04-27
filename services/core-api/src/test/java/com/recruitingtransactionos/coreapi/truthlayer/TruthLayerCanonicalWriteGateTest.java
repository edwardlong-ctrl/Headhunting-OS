package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TruthLayerCanonicalWriteGateTest {

  private final CanonicalWriteGate gate = new CanonicalWriteGate();

  @Test
  void aiExtractedWeakSignalCandidateIntentIsBlockedFromCanonicalConfirmedWrite() {
    CanonicalWriteDecision decision = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.INTENT, AssertionStrength.WEAK_SIGNAL, VerificationStatus.AI_EXTRACTED,
            ClientShareability.INTERNAL_ONLY, false),
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T3_HIGH,
        false,
        false,
        true));

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(decision.reasons()).contains("weak_signal_intent_cannot_become_confirmed_fact");
  }

  @Test
  void bulkApprovedHumanAcknowledgedClaimMustNotBecomeCandidateConfirmed() {
    CanonicalWriteDecision decision = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE, true),
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T1_LOW,
        false,
        false,
        true));

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(decision.reasons()).contains("bulk_approve_cannot_create_candidate_confirmed");
  }

  @Test
  void bulkApprovedHumanAcknowledgedClaimMustNotBecomeExternalVerified() {
    CanonicalWriteDecision decision = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE, true),
        VerificationStatus.EXTERNAL_VERIFIED,
        RiskTier.T1_LOW,
        false,
        false,
        true));

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(decision.reasons()).contains("bulk_approve_cannot_create_external_verified");
  }

  @Test
  void conflictingClaimRequiresReviewBeforeCanonicalOverwrite() {
    CanonicalWriteDecision decision = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.CONTRADICTION, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE, false),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T2_MEDIUM,
        false,
        true,
        false));

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(decision.reasons()).contains("conflicting_claim_requires_explicit_review");
  }

  @Test
  void internalOnlyClaimIsBlockedFromClientVisibleWrite() {
    CanonicalWriteDecision decision = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.INTERNAL_ONLY, false),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T1_LOW,
        true,
        false,
        true));

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(decision.reasons()).contains("internal_only_claim_cannot_be_client_visible");
  }

  @Test
  void t1LowRiskNormalizationMayBeAllowedOnlyAsHumanAcknowledged() {
    CanonicalWriteDecision allowed = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE, true),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T1_LOW,
        false,
        false,
        false));

    CanonicalWriteDecision verified = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE, true),
        VerificationStatus.EXTERNAL_VERIFIED,
        RiskTier.T1_LOW,
        false,
        false,
        true));

    assertThat(allowed.type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(allowed.reasons()).contains("low_risk_human_acknowledged_write_allowed");
    assertThat(verified.type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(verified.reasons()).contains("t1_low_risk_cannot_create_verified_fact");
  }

  @Test
  void t3AndT4HighRiskWritesRequireExplicitReviewApproval() {
    CanonicalWriteDecision highRisk = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.CANDIDATE_CONFIRMED,
            ClientShareability.CLIENT_SAFE, false),
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T3_HIGH,
        false,
        false,
        false));

    CanonicalWriteDecision transactionRisk = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.EXTERNAL_VERIFIED,
            ClientShareability.CLIENT_SAFE, false),
        VerificationStatus.EXTERNAL_VERIFIED,
        RiskTier.T4_TRANSACTION_LEGAL,
        false,
        false,
        false));

    assertThat(highRisk.type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(highRisk.reasons()).contains("high_risk_write_requires_explicit_review_approval");
    assertThat(transactionRisk.type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(transactionRisk.reasons()).contains("high_risk_write_requires_explicit_review_approval");
  }

  @Test
  void systemInferenceIsNeverAllowedAsCanonicalFact() {
    CanonicalWriteDecision decision = gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.INFERENCE, AssertionStrength.IMPLIED, VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY, false),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T1_LOW,
        false,
        false,
        true));

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(decision.reasons()).contains("system_inference_cannot_be_canonical_fact");
  }

  private static ClaimInput claim(
      ClaimType type,
      AssertionStrength strength,
      VerificationStatus verificationStatus,
      ClientShareability shareability,
      boolean bulkApproved) {
    return new ClaimInput(type, strength, verificationStatus, shareability, bulkApproved);
  }
}
