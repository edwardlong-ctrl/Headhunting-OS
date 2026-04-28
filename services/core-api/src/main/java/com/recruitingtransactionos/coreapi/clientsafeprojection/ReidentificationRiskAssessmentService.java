package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class ReidentificationRiskAssessmentService {

  private static final String PLACEHOLDER_EXPLANATION =
      "Task 7C deterministic placeholder only; not a real scorer and not a redaction pipeline.";

  public boolean placeholderOnly() {
    return true;
  }

  public ReidentificationRiskAssessment assess(
      ClientSafeCandidateCard card,
      Set<ReidentificationRiskFeature> observedUnsafeFeatures) {
    Objects.requireNonNull(card, "card must not be null");
    return assess(card.cardId(), card.redactionLevel(), observedUnsafeFeatures);
  }

  public ReidentificationRiskAssessment assess(InternalCandidateProjectionSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot must not be null");
    EnumSet<ReidentificationRiskFeature> unsafeFeatures =
        EnumSet.noneOf(ReidentificationRiskFeature.class);
    if (snapshot.exactCurrentEmployer() != null) {
      unsafeFeatures.add(ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER);
    }
    if (!snapshot.exactProjectProductOrChipNames().isEmpty()) {
      unsafeFeatures.add(ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);
    }
    if (snapshot.email() != null || snapshot.phone() != null || snapshot.linkedInUrl() != null) {
      unsafeFeatures.add(ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL);
    }
    return assess(snapshot.cardId(), snapshot.redactionLevel(), unsafeFeatures);
  }

  public ReidentificationRiskAssessment assess(
      AnonymousCandidateCardId cardId,
      RedactionLevel redactionLevel,
      Set<ReidentificationRiskFeature> observedUnsafeFeatures) {
    Objects.requireNonNull(cardId, "cardId must not be null");
    Objects.requireNonNull(redactionLevel, "redactionLevel must not be null");
    Set<ReidentificationRiskFeature> unsafeFeatures =
        copyUnsafeFeatures(observedUnsafeFeatures);
    ReidentificationRiskDecision decision = aggregateDecision(redactionLevel, unsafeFeatures);
    return new ReidentificationRiskAssessment(
        cardId,
        redactionLevel,
        riskLevelFor(redactionLevel, decision),
        unsafeFeatures,
        decision,
        PLACEHOLDER_EXPLANATION);
  }

  private static ReidentificationRiskDecision aggregateDecision(
      RedactionLevel redactionLevel,
      Set<ReidentificationRiskFeature> unsafeFeatures) {
    if (redactionLevel == RedactionLevel.L4_IDENTITY_DISCLOSED) {
      return ReidentificationRiskDecision.BLOCK;
    }
    if (unsafeFeatures.isEmpty()) {
      return ReidentificationRiskDecision.ALLOW;
    }
    if (containsRecommendation(unsafeFeatures, ReidentificationRiskDecision.BLOCK)) {
      return ReidentificationRiskDecision.BLOCK;
    }
    if (containsRecommendation(unsafeFeatures, ReidentificationRiskDecision.REVIEW)) {
      return ReidentificationRiskDecision.REVIEW;
    }
    return ReidentificationRiskDecision.GENERALIZE;
  }

  private static ReidentificationRiskLevel riskLevelFor(
      RedactionLevel redactionLevel,
      ReidentificationRiskDecision decision) {
    if (redactionLevel == RedactionLevel.L4_IDENTITY_DISCLOSED
        || decision == ReidentificationRiskDecision.BLOCK) {
      return ReidentificationRiskLevel.HIGH;
    }
    if (decision == ReidentificationRiskDecision.ALLOW) {
      return ReidentificationRiskLevel.LOW;
    }
    return ReidentificationRiskLevel.MEDIUM;
  }

  private static boolean containsRecommendation(
      Set<ReidentificationRiskFeature> unsafeFeatures,
      ReidentificationRiskDecision decision) {
    return unsafeFeatures.stream()
        .map(ReidentificationRiskFeature::recommendedDecision)
        .anyMatch(decision::equals);
  }

  private static Set<ReidentificationRiskFeature> copyUnsafeFeatures(
      Set<ReidentificationRiskFeature> features) {
    Objects.requireNonNull(features, "observedUnsafeFeatures must not be null");
    if (features.isEmpty()) {
      return Set.of();
    }
    EnumSet<ReidentificationRiskFeature> copied =
        EnumSet.noneOf(ReidentificationRiskFeature.class);
    for (ReidentificationRiskFeature feature : features) {
      copied.add(Objects.requireNonNull(
          feature,
          "observedUnsafeFeatures entry must not be null"));
    }
    return Collections.unmodifiableSet(copied);
  }
}
