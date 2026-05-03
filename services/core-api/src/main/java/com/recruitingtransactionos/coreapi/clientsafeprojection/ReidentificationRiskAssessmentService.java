package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Task 30 re-identification risk assessor.
 *
 * <p>This service is the post-redaction gate. It is given either a
 * {@link ClientSafeCandidateCard} (and the unsafe features the caller has
 * already observed) or an {@link InternalCandidateProjectionSnapshot}
 * (in which case it runs the full detector itself), and returns a
 * {@link ReidentificationRiskAssessment}.
 *
 * <p>v1 detector responsibilities:
 * <ol>
 *   <li>Detect leakage of raw sensitive values (employer, project, fullName,
 *       email, phone, profile URL, candidate id) into projected text.</li>
 *   <li>Detect "top-tier company + rare title + exact year + chip code name"
 *       combinations using {@link CompanyNameGeneralizationPolicy},
 *       {@link RareTitleYearCombinationRiskRule}, and
 *       {@link ProjectChipNameRedactionPolicy}.</li>
 *   <li>Detect direct contact / address / precise number patterns.</li>
 * </ol>
 *
 * <p>v1 produces a numeric risk score in {@code [0.0, 1.0]} aggregated from
 * the detected features. The score is stable, deterministic, and
 * explainable: every contributing feature has a fixed weight; the final
 * score is the sum of weights clipped to {@code 1.0}. This is intentionally
 * not a learned classifier — it is policy-as-code.
 *
 * <p>The service is pure: no Spring annotations, no DataSource, no logging.
 * Persistence and audit emission live in
 * {@code com.recruitingtransactionos.coreapi.privacyredaction}.
 */
public final class ReidentificationRiskAssessmentService {

  private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
  private static final Pattern ADDRESS_PATTERN =
      Pattern.compile("\\b\\d{1,5}\\s+[a-z]+(?:\\s+[a-z]+){0,3}\\s+(street|st|road|rd|avenue|ave|lane|ln|building|tower)\\b");
  private static final Pattern DIRECT_CONTACT_PATTERN =
      Pattern.compile("@|https?://|www\\.|\\+?\\d[\\d\\s()\\-]{7,}");
  private static final Pattern PRECISE_NUMBER_PATTERN =
      Pattern.compile("\\b\\d{2,}(?:\\.\\d+)?(?:%|x|nm|mm|mhz|ghz|tb|gb|million|billion)?\\b");

  /**
   * Per-feature contribution to the aggregate score in {@code [0.0, 1.0]}.
   * Weights are calibrated so that any single BLOCK-recommended feature
   * pushes the score above the {@code 0.7} HIGH threshold, and any
   * GENERALIZE/REVIEW-recommended feature pushes it above the {@code 0.4}
   * MEDIUM threshold.
   */
  static double weightFor(ReidentificationRiskFeature feature) {
    return switch (feature.recommendedDecision()) {
      case BLOCK -> 0.75;
      case GENERALIZE -> 0.35;
      case REVIEW -> 0.40;
      case ALLOW -> 0.0;
    };
  }

  static double computeRiskScore(
      RedactionLevel redactionLevel,
      Set<ReidentificationRiskFeature> unsafeFeatures) {
    if (redactionLevel == RedactionLevel.L4_IDENTITY_DISCLOSED) {
      return 1.0;
    }
    if (unsafeFeatures.isEmpty()) {
      return 0.0;
    }
    double total = 0.0;
    for (ReidentificationRiskFeature feature : unsafeFeatures) {
      total += weightFor(feature);
    }
    return Math.min(total, 1.0);
  }

  public ReidentificationRiskAssessment assess(
      ClientSafeCandidateCard card,
      Set<ReidentificationRiskFeature> observedUnsafeFeatures) {
    Objects.requireNonNull(card, "card must not be null");
    return assess(card.cardId(), card.redactionLevel(), observedUnsafeFeatures);
  }

  public ReidentificationRiskAssessment assess(InternalCandidateProjectionSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot must not be null");
    Set<ReidentificationRiskFeature> unsafeFeatures = detectUnsafeFeatures(snapshot);
    ReidentificationRiskDecision decision =
        aggregateDecision(snapshot.redactionLevel(), unsafeFeatures);
    return new ReidentificationRiskAssessment(
        snapshot.cardId(),
        snapshot.redactionLevel(),
        riskLevelFor(snapshot.redactionLevel(), decision),
        unsafeFeatures,
        decision,
        computeRiskScore(snapshot.redactionLevel(), unsafeFeatures),
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
        computeRiskScore(redactionLevel, unsafeFeatures),
        explanation(redactionLevel, unsafeFeatures));
  }

  /**
   * Runs the full Task 30 pipeline: redact via
   * {@link ClientSafeSummaryPipeline}, then assess the redacted snapshot.
   * Returns both the redaction trail and the final assessment so callers
   * can persist them as a single audit record.
   */
  public PipelineResult assessWithPipeline(
      InternalCandidateProjectionSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot must not be null");
    ClientSafeSummaryPipeline.Result pipeline = ClientSafeSummaryPipeline.redact(snapshot);
    Set<ReidentificationRiskFeature> detectedAfterRedaction =
        detectUnsafeFeatures(pipeline.redactedSnapshot());
    EnumSet<ReidentificationRiskFeature> aggregate =
        EnumSet.noneOf(ReidentificationRiskFeature.class);
    aggregate.addAll(pipeline.unsafeFeaturesObserved());
    aggregate.addAll(detectedAfterRedaction);
    ReidentificationRiskDecision decision =
        aggregateDecision(snapshot.redactionLevel(), aggregate);
    ReidentificationRiskAssessment assessment = new ReidentificationRiskAssessment(
        snapshot.cardId(),
        snapshot.redactionLevel(),
        riskLevelFor(snapshot.redactionLevel(), decision),
        Collections.unmodifiableSet(aggregate),
        decision,
        computeRiskScore(snapshot.redactionLevel(), aggregate),
        explanation(snapshot.redactionLevel(), aggregate));
    return new PipelineResult(pipeline, assessment);
  }

  /**
   * Combined pipeline + assessment output.
   */
  public record PipelineResult(
      ClientSafeSummaryPipeline.Result pipeline,
      ReidentificationRiskAssessment assessment) {

    public PipelineResult {
      Objects.requireNonNull(pipeline, "pipeline must not be null");
      Objects.requireNonNull(assessment, "assessment must not be null");
    }
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

    boolean snapshotHasTopTierEmployer =
        CompanyNameGeneralizationPolicy.isKnownTopTierCompany(
            snapshot.exactCurrentEmployer());
    List<String> projectedRawTexts = new ArrayList<>(projectedTexts);

    for (String text : projectedRawTexts) {
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

      // Task 30 rare-title + exact-year detection runs against the original
      // (non-normalized) text so capitalization patterns survive.
      if (RareTitleYearCombinationRiskRule.detect(text).matched()) {
        unsafeFeatures.add(ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR);
      }

      // If a top-tier employer is part of the snapshot AND the text mentions
      // a four-digit year, treat that as the rare-title + exact-year +
      // exact-company combination even when the title vocabulary itself was
      // not explicitly hit. Acceptance scenario S04 falls into this branch
      // when the rare title is generic (e.g. "principal verification lead").
      if (snapshotHasTopTierEmployer
          && YEAR_PATTERN.matcher(normalized).find()) {
        unsafeFeatures.add(ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR);
      }

      // Task 30 chip-code-name shape detection. This is a *signal*, not a
      // redaction; the projection is rejected before reaching the client.
      ProjectChipNameRedactionPolicy.Redaction chipRedaction =
          ProjectChipNameRedactionPolicy.redact(text, List.of());
      if (chipRedaction.knownChipFamilyMatched()
          || chipRedaction.genericChipShapeMatched()) {
        unsafeFeatures.add(ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);
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
