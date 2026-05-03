package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Task 30 client-safe summary generation pipeline.
 *
 * <p>Takes an {@link InternalCandidateProjectionSnapshot} that may carry
 * exact employer names, exact chip / project names, and other identifying
 * detail — and returns a redacted version where:
 * <ul>
 *   <li>any reference to a known top-tier company is generalized
 *       (e.g. "TSMC" → "Top semiconductor foundry");</li>
 *   <li>any reference to a known or shape-matching chip / product / project
 *       code name is replaced with the redaction token; and</li>
 *   <li>the rare-title + exact-year combination is stripped of the year and
 *       the title is generalized to a band.</li>
 * </ul>
 *
 * <p>The pipeline is run BEFORE the
 * {@link ClientSafeCandidateProjectionService} validates the snapshot, so
 * the projection-service's existing
 * "projected text must not contain raw sensitive value" guardrail still
 * applies after the pipeline rewrites text.
 *
 * <p>The pipeline is also the source of truth for the
 * {@link ReidentificationRiskFeature} set that
 * {@link ReidentificationRiskAssessmentService} aggregates into a final
 * decision: every redaction the pipeline performs adds the corresponding
 * feature. This makes "what did we redact" and "what risk did we assess"
 * trivially auditable.
 */
public final class ClientSafeSummaryPipeline {

  /**
   * Output of the pipeline.
   *
   * <p>{@code redactedSnapshot} is the input snapshot with text fields
   * rewritten. {@code unsafeFeaturesObserved} lists every feature the
   * pipeline detected (and either generalized, redacted, or left intact when
   * it could not). {@code redactionExplanations} is a human-readable trail
   * suitable for an audit event.
   */
  public record Result(
      InternalCandidateProjectionSnapshot redactedSnapshot,
      Set<ReidentificationRiskFeature> unsafeFeaturesObserved,
      List<String> redactionExplanations) {

    public Result {
      Objects.requireNonNull(redactedSnapshot, "redactedSnapshot must not be null");
      unsafeFeaturesObserved = unsafeFeaturesObserved == null
          ? Set.of()
          : Collections.unmodifiableSet(EnumSet.copyOf(unsafeFeaturesObserved));
      redactionExplanations = redactionExplanations == null
          ? List.of()
          : List.copyOf(redactionExplanations);
    }
  }

  private ClientSafeSummaryPipeline() {
  }

  public static Result redact(InternalCandidateProjectionSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot must not be null");

    EnumSet<ReidentificationRiskFeature> features =
        EnumSet.noneOf(ReidentificationRiskFeature.class);
    List<String> explanations = new ArrayList<>();

    String exactCurrentEmployer = snapshot.exactCurrentEmployer();
    List<String> declaredChipNames = snapshot.exactProjectProductOrChipNames();
    boolean hasExactEmployer = exactCurrentEmployer != null
        && !exactCurrentEmployer.isBlank();
    boolean hasDeclaredChipNames = !declaredChipNames.isEmpty();

    CompanyNameGeneralizationPolicy.Generalization employerGeneralization =
        hasExactEmployer
            ? CompanyNameGeneralizationPolicy.generalize(exactCurrentEmployer)
            : null;
    if (hasExactEmployer && employerGeneralization != null) {
      features.add(ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER);
      explanations.add("exact current employer '"
          + exactCurrentEmployer
          + "' generalized to '"
          + employerGeneralization.generalizedLabel()
          + "' (category="
          + employerGeneralization.category().name().toLowerCase(Locale.ROOT)
          + ")");
    } else if (hasExactEmployer) {
      // Unknown employer name: still flag it but keep a generic generalized
      // label. The pipeline must not leak the raw value into the output.
      features.add(ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER);
      explanations.add("exact current employer '"
          + exactCurrentEmployer
          + "' is not in the curated top-tier list; redacted to a generic"
          + " label without leaking the raw value");
    }

    if (hasDeclaredChipNames) {
      features.add(ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);
      explanations.add("declared chip/product code names "
          + declaredChipNames
          + " redacted from every projected text value");
    }

    String headline = snapshot.generalizedHeadline();
    String roleFamily = snapshot.generalizedRoleFamily();
    String seniorityBand = snapshot.generalizedSeniorityBand();
    String locationRegion = snapshot.generalizedLocationRegion();
    String safeSummary = snapshot.safeSummary();
    String safeSkillSummary = snapshot.safeSkillSummary();
    List<String> safeEvidenceSummaries =
        new ArrayList<>(snapshot.safeEvidenceSummaries());
    List<String> safeMatchNarratives =
        new ArrayList<>(snapshot.safeMatchNarratives());

    headline = generalizeTextValue(
        headline, exactCurrentEmployer, employerGeneralization,
        declaredChipNames, features, explanations, "generalizedHeadline");
    roleFamily = generalizeTextValue(
        roleFamily, exactCurrentEmployer, employerGeneralization,
        declaredChipNames, features, explanations, "generalizedRoleFamily");
    seniorityBand = generalizeTextValue(
        seniorityBand, exactCurrentEmployer, employerGeneralization,
        declaredChipNames, features, explanations, "generalizedSeniorityBand");
    locationRegion = generalizeTextValue(
        locationRegion, exactCurrentEmployer, employerGeneralization,
        declaredChipNames, features, explanations, "generalizedLocationRegion");
    safeSummary = generalizeTextValue(
        safeSummary, exactCurrentEmployer, employerGeneralization,
        declaredChipNames, features, explanations, "safeSummary");
    safeSkillSummary = generalizeTextValue(
        safeSkillSummary, exactCurrentEmployer, employerGeneralization,
        declaredChipNames, features, explanations, "safeSkillSummary");

    List<String> redactedEvidenceSummaries = new ArrayList<>();
    for (int index = 0; index < safeEvidenceSummaries.size(); index++) {
      String value = safeEvidenceSummaries.get(index);
      redactedEvidenceSummaries.add(generalizeTextValue(
          value, exactCurrentEmployer, employerGeneralization,
          declaredChipNames, features, explanations,
          "safeEvidenceSummaries[" + index + "]"));
    }
    List<String> redactedMatchNarratives = new ArrayList<>();
    for (int index = 0; index < safeMatchNarratives.size(); index++) {
      String value = safeMatchNarratives.get(index);
      redactedMatchNarratives.add(generalizeTextValue(
          value, exactCurrentEmployer, employerGeneralization,
          declaredChipNames, features, explanations,
          "safeMatchNarratives[" + index + "]"));
    }

    InternalCandidateProjectionSnapshot redactedSnapshot =
        new InternalCandidateProjectionSnapshot(
            snapshot.rawCandidateId(),
            snapshot.rawCandidateProfileId(),
            snapshot.fullName(),
            snapshot.email(),
            snapshot.phone(),
            snapshot.linkedInUrl(),
            null /* exactCurrentEmployer is removed from the projection input */,
            List.of() /* declared chip names are removed from the projection input */,
            snapshot.rawSourceText(),
            snapshot.consultantInternalNotes(),
            snapshot.cardId(),
            snapshot.anonymousCandidateRef(),
            snapshot.projectionVersion(),
            snapshot.redactionLevel(),
            headline,
            roleFamily,
            seniorityBand,
            locationRegion,
            safeSummary,
            safeSkillSummary,
            redactedEvidenceSummaries,
            redactedMatchNarratives,
            snapshot.selectedClientVisibleFieldPaths());

    return new Result(redactedSnapshot, features, explanations);
  }

  private static String generalizeTextValue(
      String value,
      String exactCurrentEmployer,
      CompanyNameGeneralizationPolicy.Generalization employerGeneralization,
      List<String> declaredChipNames,
      Set<ReidentificationRiskFeature> features,
      List<String> explanations,
      String fieldLabel) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String working = value;

    // Step 1: replace any literal mention of the exact current employer.
    if (exactCurrentEmployer != null && !exactCurrentEmployer.isBlank()) {
      String replacement = employerGeneralization != null
          ? employerGeneralization.generalizedLabel()
          : "[redacted-employer]";
      String replaced = caseInsensitiveReplace(working, exactCurrentEmployer, replacement);
      if (!replaced.equals(working)) {
        working = replaced;
        explanations.add(fieldLabel
            + ": replaced literal employer mention with '"
            + replacement
            + "'");
      }
    }

    // Step 2: redact known + declared chip / product / project code names.
    ProjectChipNameRedactionPolicy.Redaction chipRedaction =
        ProjectChipNameRedactionPolicy.redact(working, declaredChipNames);
    if (chipRedaction.anyRedaction()) {
      working = chipRedaction.redactedText();
      features.add(ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);
      if (chipRedaction.knownChipFamilyMatched()) {
        explanations.add(fieldLabel
            + ": redacted known chip-family code name");
      }
      if (chipRedaction.genericChipShapeMatched()) {
        explanations.add(fieldLabel
            + ": redacted generic chip code-name shape (chip context detected)");
      }
      if (!chipRedaction.redactedExactNames().isEmpty()) {
        explanations.add(fieldLabel
            + ": redacted declared exact code names "
            + chipRedaction.redactedExactNames());
      }
    }

    // Step 3: rare-title + exact-year combination strip.
    RareTitleYearCombinationRiskRule.Detection rareTitleDetection =
        RareTitleYearCombinationRiskRule.detect(working);
    if (rareTitleDetection.matched()) {
      working = stripRareTitleYearCombo(working, rareTitleDetection);
      features.add(ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR);
      explanations.add(fieldLabel
          + ": stripped rare-title '"
          + rareTitleDetection.matchedTitleSpan()
          + "' + exact-year '"
          + rareTitleDetection.matchedYear()
          + "' combination");
    }

    return working;
  }

  private static String caseInsensitiveReplace(
      String haystack,
      String needle,
      String replacement) {
    if (needle == null || needle.isBlank()) {
      return haystack;
    }
    StringBuilder result = new StringBuilder(haystack.length());
    String lowerHaystack = haystack.toLowerCase(Locale.ROOT);
    String lowerNeedle = needle.strip().toLowerCase(Locale.ROOT);
    int cursor = 0;
    int found;
    while ((found = lowerHaystack.indexOf(lowerNeedle, cursor)) >= 0) {
      result.append(haystack, cursor, found);
      result.append(replacement);
      cursor = found + lowerNeedle.length();
    }
    if (cursor < haystack.length()) {
      result.append(haystack, cursor, haystack.length());
    }
    return result.toString();
  }

  private static String stripRareTitleYearCombo(
      String text,
      RareTitleYearCombinationRiskRule.Detection detection) {
    String afterYear = text.replace(detection.matchedYear(), "[redacted-year]");
    return afterYear.replace(detection.matchedTitleSpan(), "[redacted-rare-title]");
  }

  /**
   * Returns the unsafe features that the pipeline observed in the given
   * already-built {@link ClientSafeCandidateCard}. This is used by callers
   * that received a pre-redacted card and want to verify the assessment
   * matches.
   */
  public static Set<ReidentificationRiskFeature> observedFeaturesIn(
      ClientSafeCandidateCard card) {
    Objects.requireNonNull(card, "card must not be null");
    LinkedHashSet<String> values = new LinkedHashSet<>();
    values.add(card.generalizedHeadline());
    values.add(card.generalizedRoleFamily());
    values.add(card.generalizedSeniorityBand());
    values.add(card.generalizedLocationRegion());
    values.add(card.safeSummary());
    values.add(card.safeSkillSummary());
    values.addAll(card.safeEvidenceSummaries());
    values.addAll(card.safeMatchNarratives());
    EnumSet<ReidentificationRiskFeature> features =
        EnumSet.noneOf(ReidentificationRiskFeature.class);
    for (String value : values) {
      if (value == null || value.isBlank()) {
        continue;
      }
      ProjectChipNameRedactionPolicy.Redaction redaction =
          ProjectChipNameRedactionPolicy.redact(value, List.of());
      if (redaction.knownChipFamilyMatched() || redaction.genericChipShapeMatched()) {
        features.add(ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);
      }
      RareTitleYearCombinationRiskRule.Detection detection =
          RareTitleYearCombinationRiskRule.detect(value);
      if (detection.matched()) {
        features.add(ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR);
      }
    }
    return Collections.unmodifiableSet(features);
  }
}
