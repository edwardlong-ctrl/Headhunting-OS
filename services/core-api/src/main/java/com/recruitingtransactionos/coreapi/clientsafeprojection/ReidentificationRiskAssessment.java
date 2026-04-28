package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record ReidentificationRiskAssessment(
    AnonymousCandidateCardId cardId,
    RedactionLevel redactionLevel,
    ReidentificationRiskLevel riskLevel,
    Set<ReidentificationRiskFeature> unsafeFeatures,
    ReidentificationRiskDecision decision,
    String explanation) {

  public ReidentificationRiskAssessment {
    Objects.requireNonNull(cardId, "cardId must not be null");
    Objects.requireNonNull(redactionLevel, "redactionLevel must not be null");
    Objects.requireNonNull(riskLevel, "riskLevel must not be null");
    unsafeFeatures = copyUnsafeFeatures(unsafeFeatures);
    Objects.requireNonNull(decision, "decision must not be null");
    explanation = ClientSafeProjectionGuards.requireNonBlank(explanation, "explanation");
  }

  public boolean isSafeAnonymousClientOutput() {
    return redactionLevel.isAnonymousClientSafeLevel()
        && riskLevel == ReidentificationRiskLevel.LOW
        && decision == ReidentificationRiskDecision.ALLOW
        && unsafeFeatures.isEmpty();
  }

  public void requireSafeAnonymousClientOutput() {
    if (!isSafeAnonymousClientOutput()) {
      throw new IllegalStateException(
          "re-identification assessment is not safe for anonymous client output");
    }
  }

  private static Set<ReidentificationRiskFeature> copyUnsafeFeatures(
      Set<ReidentificationRiskFeature> features) {
    Objects.requireNonNull(features, "unsafeFeatures must not be null");
    if (features.isEmpty()) {
      return Set.of();
    }
    EnumSet<ReidentificationRiskFeature> copied =
        EnumSet.noneOf(ReidentificationRiskFeature.class);
    for (ReidentificationRiskFeature feature : features) {
      copied.add(Objects.requireNonNull(feature, "unsafeFeatures entry must not be null"));
    }
    return Collections.unmodifiableSet(copied);
  }
}
