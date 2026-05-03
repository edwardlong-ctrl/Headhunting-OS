package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Result of running the Task 30 re-identification risk pipeline against an
 * internal candidate projection snapshot or a previously-built
 * {@link ClientSafeCandidateCard}.
 *
 * <p>This is a pure value object. Persistence of the assessment lives outside
 * the {@code clientsafeprojection} package (see
 * {@code com.recruitingtransactionos.coreapi.privacyredaction}) so that the
 * domain remains free of Spring, DataSource, and JDBC concerns.
 */
public record ReidentificationRiskAssessment(
    AnonymousCandidateCardId cardId,
    RedactionLevel redactionLevel,
    ReidentificationRiskLevel riskLevel,
    Set<ReidentificationRiskFeature> unsafeFeatures,
    ReidentificationRiskDecision decision,
    double riskScore,
    String explanation) {

  public ReidentificationRiskAssessment {
    Objects.requireNonNull(cardId, "cardId must not be null");
    Objects.requireNonNull(redactionLevel, "redactionLevel must not be null");
    Objects.requireNonNull(riskLevel, "riskLevel must not be null");
    unsafeFeatures = copyUnsafeFeatures(unsafeFeatures);
    Objects.requireNonNull(decision, "decision must not be null");
    if (Double.isNaN(riskScore) || riskScore < 0.0 || riskScore > 1.0) {
      throw new IllegalArgumentException(
          "riskScore must be a finite value within [0.0, 1.0]; got " + riskScore);
    }
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
