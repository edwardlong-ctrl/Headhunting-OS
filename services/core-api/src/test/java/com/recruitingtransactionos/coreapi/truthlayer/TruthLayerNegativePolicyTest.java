package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TruthLayerNegativePolicyTest {

  private final CanonicalWriteGate gate = new CanonicalWriteGate();

  @Test
  void aiExtractedClaimCannotBecomeCanonicalVerifiedFact() {
    CanonicalWriteDecision decision = decide(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.AI_EXTRACTED,
            ClientShareability.CLIENT_SAFE, false),
        VerificationStatus.EXTERNAL_VERIFIED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        true);

    assertUnsafe(decision);
    assertThat(decision.reasons()).contains("ai_extracted_claim_cannot_be_canonical_fact");
  }

  @Test
  void systemInferenceCannotBecomeCanonicalFact() {
    ClaimInput inference = claim(ClaimType.INFERENCE, AssertionStrength.IMPLIED,
        VerificationStatus.SYSTEM_INFERENCE, ClientShareability.INTERNAL_ONLY, false);

    CanonicalWriteDecision candidateConfirmed = decide(
        inference,
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        true);
    CanonicalWriteDecision externalVerified = decide(
        inference,
        VerificationStatus.EXTERNAL_VERIFIED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        true);

    assertBlocked(candidateConfirmed);
    assertBlocked(externalVerified);
    assertThat(candidateConfirmed.reasons()).contains("system_inference_cannot_be_canonical_fact");
    assertThat(externalVerified.reasons()).contains("system_inference_cannot_be_canonical_fact");
  }

  @Test
  void bulkApprovalCannotProduceCandidateConfirmed() {
    CanonicalWriteDecision decision = decide(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE, true),
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        true);

    assertBlocked(decision);
    assertThat(decision.reasons()).contains("bulk_approve_cannot_create_candidate_confirmed");
  }

  @Test
  void bulkApprovalCannotProduceExternalVerified() {
    CanonicalWriteDecision decision = decide(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE, true),
        VerificationStatus.EXTERNAL_VERIFIED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        true);

    assertBlocked(decision);
    assertThat(decision.reasons()).contains("bulk_approve_cannot_create_external_verified");
  }

  @Test
  void weakSignalIntentCannotBecomeConfirmedIntent() {
    CanonicalWriteDecision weakSignal = decide(
        claim(ClaimType.INTENT, AssertionStrength.WEAK_SIGNAL,
            VerificationStatus.HUMAN_ACKNOWLEDGED, ClientShareability.INTERNAL_ONLY, false),
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        true);
    CanonicalWriteDecision implied = decide(
        claim(ClaimType.INTENT, AssertionStrength.IMPLIED,
            VerificationStatus.HUMAN_ACKNOWLEDGED, ClientShareability.INTERNAL_ONLY, false),
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        true);

    assertBlocked(weakSignal);
    assertBlocked(implied);
    assertThat(weakSignal.reasons()).contains("weak_signal_intent_cannot_become_confirmed_fact");
    assertThat(implied.reasons()).contains("implied_intent_cannot_become_confirmed_fact");
  }

  @Test
  void contradictionOrConflictingClaimCannotOverwriteCanonical() {
    CanonicalWriteDecision contradiction = decide(
        claim(ClaimType.FACT, AssertionStrength.CONTRADICTION,
            VerificationStatus.HUMAN_ACKNOWLEDGED, ClientShareability.CLIENT_SAFE, false),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        false);
    CanonicalWriteDecision conflicting = decide(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.CONFLICTING,
            ClientShareability.CLIENT_SAFE, false),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        false,
        false);

    assertUnsafe(contradiction);
    assertUnsafe(conflicting);
    assertThat(contradiction.reasons()).contains("conflicting_claim_requires_explicit_review");
    assertThat(conflicting.reasons()).contains("conflicting_claim_requires_explicit_review");
  }

  @Test
  void internalOnlyClaimCannotBeClientVisible() {
    CanonicalWriteDecision decision = clientVisibleDecision(ClientShareability.INTERNAL_ONLY);

    assertBlocked(decision);
    assertThat(decision.reasons()).contains("internal_only_claim_cannot_be_client_visible");
  }

  @Test
  void forbiddenClaimCannotBeClientVisible() {
    CanonicalWriteDecision decision = clientVisibleDecision(ClientShareability.FORBIDDEN);

    assertBlocked(decision);
    assertThat(decision.reasons()).contains("forbidden_claim_cannot_be_client_visible");
  }

  @Test
  void consentRequiredIsNotClientSafeWithoutConsentWorkflow() {
    CanonicalWriteDecision decision = clientVisibleDecision(ClientShareability.CONSENT_REQUIRED);

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(decision.reasons()).contains("client_visible_claim_requires_consent_or_review");
  }

  @Test
  void highRiskT3RequiresExplicitReview() {
    CanonicalWriteDecision decision = decide(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT,
            VerificationStatus.CANDIDATE_CONFIRMED, ClientShareability.CLIENT_SAFE, false),
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T3_HIGH_RISK,
        false,
        false,
        false);

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(decision.reasons()).contains("high_risk_write_requires_explicit_review_approval");
  }

  @Test
  void transactionLegalT4RequiresStrongGate() {
    CanonicalWriteDecision decision = decide(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT,
            VerificationStatus.HUMAN_ACKNOWLEDGED, ClientShareability.CLIENT_SAFE, true),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        false,
        false,
        true);

    assertUnsafe(decision);
    assertThat(decision.reasons()).contains("t4_transaction_legal_cannot_use_bulk_approval");
  }

  @Test
  void bareConfirmedStatusMustNotExist() {
    List<String> verificationWireValues = Arrays.stream(VerificationStatus.values())
        .map(VerificationStatus::wireValue)
        .toList();
    List<String> verificationEnumNames = Arrays.stream(VerificationStatus.values())
        .map(Enum::name)
        .map(String::toLowerCase)
        .toList();

    assertThat(verificationWireValues)
        .contains("candidate_confirmed", "consultant_attested", "external_verified")
        .doesNotContain("confirmed");
    assertThat(verificationEnumNames)
        .contains("candidate_confirmed", "consultant_attested", "external_verified")
        .doesNotContain("confirmed");
  }

  @Test
  void unsafeDecisionMustExposeReason() {
    List<CanonicalWriteDecision> unsafeDecisions = List.of(
        decide(
            claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.AI_EXTRACTED,
                ClientShareability.CLIENT_SAFE, false),
            VerificationStatus.HUMAN_ACKNOWLEDGED,
            RiskTier.T1_LOW_RISK,
            false,
            false,
            true),
        clientVisibleDecision(ClientShareability.FORBIDDEN),
        decide(
            claim(ClaimType.FACT, AssertionStrength.EXPLICIT,
                VerificationStatus.HUMAN_ACKNOWLEDGED, ClientShareability.CLIENT_SAFE, false),
            VerificationStatus.HUMAN_ACKNOWLEDGED,
            RiskTier.T3_HIGH_RISK,
            false,
            false,
            false));

    assertThat(unsafeDecisions)
        .allSatisfy(decision -> {
          assertUnsafe(decision);
          assertThat(decision.reasons()).isNotEmpty().allSatisfy(reason ->
              assertThat(reason).isNotBlank());
        });
  }

  @Test
  void clientShareabilityIsGateNotMetadata() {
    CanonicalWriteDecision clientSafe = clientVisibleDecision(ClientShareability.CLIENT_SAFE);
    CanonicalWriteDecision internalOnly = clientVisibleDecision(ClientShareability.INTERNAL_ONLY);
    CanonicalWriteDecision forbidden = clientVisibleDecision(ClientShareability.FORBIDDEN);

    assertThat(clientSafe.type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertBlocked(internalOnly);
    assertBlocked(forbidden);
    assertThat(internalOnly.type()).isNotEqualTo(clientSafe.type());
    assertThat(forbidden.type()).isNotEqualTo(clientSafe.type());
  }

  private CanonicalWriteDecision clientVisibleDecision(ClientShareability shareability) {
    return decide(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            shareability, false),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T1_LOW_RISK,
        true,
        false,
        false);
  }

  private CanonicalWriteDecision decide(
      ClaimInput claim,
      VerificationStatus targetVerificationStatus,
      RiskTier targetRiskTier,
      boolean clientVisible,
      boolean conflictsWithCanonical,
      boolean explicitReviewApproved) {
    return gate.decide(new CanonicalWriteRequest(
        claim,
        true,
        targetVerificationStatus,
        targetRiskTier,
        clientVisible,
        conflictsWithCanonical,
        explicitReviewApproved));
  }

  private static ClaimInput claim(
      ClaimType type,
      AssertionStrength strength,
      VerificationStatus verificationStatus,
      ClientShareability shareability,
      boolean bulkApproved) {
    return new ClaimInput(type, strength, verificationStatus, shareability, bulkApproved);
  }

  private static void assertBlocked(CanonicalWriteDecision decision) {
    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
  }

  private static void assertUnsafe(CanonicalWriteDecision decision) {
    assertThat(decision.type())
        .isIn(CanonicalWriteDecisionType.BLOCK, CanonicalWriteDecisionType.REQUIRE_REVIEW);
  }
}
