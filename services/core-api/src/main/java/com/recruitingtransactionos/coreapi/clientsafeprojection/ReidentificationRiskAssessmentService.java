package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ReidentificationRiskAssessmentService {

  private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
  private static final Pattern ADDRESS_PATTERN =
      Pattern.compile("\\b\\d{1,5}\\s+[a-z]+(?:\\s+[a-z]+){0,3}\\s+(street|st|road|rd|avenue|ave|lane|ln|building|tower)\\b");
  private static final Pattern DIRECT_CONTACT_PATTERN =
      Pattern.compile("@|https?://|www\\.|\\+?\\d[\\d\\s()\\-]{7,}");
  private static final Pattern PRECISE_NUMBER_PATTERN =
      Pattern.compile("\\b\\d{2,}(?:\\.\\d+)?(?:%|x|nm|mm|mhz|ghz|tb|gb|million|billion)?\\b");

  public ReidentificationRiskAssessment assess(
      ClientSafeCandidateCard card,
      Set<ReidentificationRiskFeature> observedUnsafeFeatures) {
    Objects.requireNonNull(card, "card must not be null");
    return assess(card.cardId(), card.redactionLevel(), observedUnsafeFeatures);
  }

  public ReidentificationRiskAssessment assess(InternalCandidateProjectionSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot must not be null");
    Set<ReidentificationRiskFeature> unsafeFeatures = detectUnsafeFeatures(snapshot);
    return new ReidentificationRiskAssessment(
        snapshot.cardId(),
        snapshot.redactionLevel(),
        riskLevelFor(snapshot.redactionLevel(), aggregateDecision(snapshot.redactionLevel(), unsafeFeatures)),
        unsafeFeatures,
        aggregateDecision(snapshot.redactionLevel(), unsafeFeatures),
        explanation(snapshot.redactionLevel(), unsafeFeatures));
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
        explanation(redactionLevel, unsafeFeatures));
  }

  private static Set<ReidentificationRiskFeature> detectUnsafeFeatures(
      InternalCandidateProjectionSnapshot snapshot) {
    EnumSet<ReidentificationRiskFeature> unsafeFeatures =
        EnumSet.noneOf(ReidentificationRiskFeature.class);
    List<String> projectedTexts = snapshot.projectedTextValues();
    if (!normalize(snapshot.exactCurrentEmployer()).isBlank()) {
      unsafeFeatures.add(ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER);
    }
    if (!snapshot.exactProjectProductOrChipNames().isEmpty()) {
      unsafeFeatures.add(ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);
    }
    if (!normalize(snapshot.email()).isBlank()
        || !normalize(snapshot.phone()).isBlank()
        || !normalize(snapshot.linkedInUrl()).isBlank()) {
      unsafeFeatures.add(ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL);
    }

    detectProjectedLeak(snapshot.fullName(), projectedTexts, unsafeFeatures,
        ReidentificationRiskFeature.PUBLIC_IDENTIFIER_BEFORE_CONSENT);
    detectProjectedLeak(snapshot.rawCandidateId(), projectedTexts, unsafeFeatures,
        ReidentificationRiskFeature.PUBLIC_IDENTIFIER_BEFORE_CONSENT);
    detectProjectedLeak(snapshot.rawCandidateProfileId(), projectedTexts, unsafeFeatures,
        ReidentificationRiskFeature.PUBLIC_IDENTIFIER_BEFORE_CONSENT);
    detectProjectedLeak(snapshot.email(), projectedTexts, unsafeFeatures,
        ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL);
    detectProjectedLeak(snapshot.phone(), projectedTexts, unsafeFeatures,
        ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL);
    detectProjectedLeak(snapshot.linkedInUrl(), projectedTexts, unsafeFeatures,
        ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL);
    detectProjectedLeak(snapshot.exactCurrentEmployer(), projectedTexts, unsafeFeatures,
        ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER);
    for (String projectName : snapshot.exactProjectProductOrChipNames()) {
      detectProjectedLeak(projectName, projectedTexts, unsafeFeatures,
          ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);
    }

    for (String text : projectedTexts) {
      String normalized = normalize(text);
      if (normalized.isBlank()) {
        continue;
      }
      if (DIRECT_CONTACT_PATTERN.matcher(normalized).find()) {
        unsafeFeatures.add(ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL);
      }
      if (ADDRESS_PATTERN.matcher(normalized).find()) {
        unsafeFeatures.add(ReidentificationRiskFeature.EXACT_LOCATION_OR_ADDRESS);
      }
      if (containsUniqueOwnershipClaim(normalized)) {
        unsafeFeatures.add(ReidentificationRiskFeature.SMALL_TEAM_UNIQUE_OWNERSHIP_CLAIM);
      }
      if (containsPreciseIdentifyingNumber(normalized)) {
        unsafeFeatures.add(ReidentificationRiskFeature.OVERLY_SPECIFIC_IDENTIFYING_ACHIEVEMENT_NUMBER);
      }
      if (containsEmployerWithYear(normalized, snapshot.exactCurrentEmployer())) {
        unsafeFeatures.add(ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR);
      }
    }
    return Collections.unmodifiableSet(unsafeFeatures);
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

  private static String explanation(
      RedactionLevel redactionLevel,
      Set<ReidentificationRiskFeature> unsafeFeatures) {
    if (redactionLevel == RedactionLevel.L4_IDENTITY_DISCLOSED) {
      return "identity-disclosed output is never eligible for anonymous client delivery";
    }
    if (unsafeFeatures.isEmpty()) {
      return "no direct re-identification signals were detected in the projected client-visible payload";
    }
    return "projected payload triggers re-identification safeguards for: "
        + unsafeFeatures.stream()
            .map(ReidentificationRiskFeature::wireValue)
            .sorted()
            .reduce((left, right) -> left + ", " + right)
            .orElseThrow();
  }

  private static void detectProjectedLeak(
      String rawValue,
      List<String> projectedTexts,
      Set<ReidentificationRiskFeature> unsafeFeatures,
      ReidentificationRiskFeature feature) {
    String normalizedRawValue = normalize(rawValue);
    if (normalizedRawValue.isBlank()) {
      return;
    }
    boolean leaked = projectedTexts.stream()
        .map(ReidentificationRiskAssessmentService::normalize)
        .anyMatch(text -> !text.isBlank() && text.contains(normalizedRawValue));
    if (leaked) {
      unsafeFeatures.add(feature);
    }
  }

  private static boolean containsUniqueOwnershipClaim(String normalizedText) {
    return normalizedText.contains("sole owner")
        || normalizedText.contains("only engineer")
        || normalizedText.contains("single handedly")
        || normalizedText.contains("single-handedly")
        || normalizedText.contains("founded and led")
        || normalizedText.contains("person team")
        || normalizedText.contains("唯一");
  }

  private static boolean containsPreciseIdentifyingNumber(String normalizedText) {
    return PRECISE_NUMBER_PATTERN.matcher(normalizedText).find()
        && (normalizedText.contains("%")
            || normalizedText.contains("x")
            || normalizedText.contains("nm")
            || normalizedText.contains("ghz")
            || normalizedText.contains("mhz"));
  }

  private static boolean containsEmployerWithYear(String normalizedText, String exactCurrentEmployer) {
    String normalizedEmployer = normalize(exactCurrentEmployer);
    return !normalizedEmployer.isBlank()
        && normalizedText.contains(normalizedEmployer)
        && YEAR_PATTERN.matcher(normalizedText).find();
  }

  private static String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.strip().toLowerCase(Locale.ROOT);
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
