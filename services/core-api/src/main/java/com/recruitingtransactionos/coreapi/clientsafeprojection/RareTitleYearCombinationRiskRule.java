package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task 30 rare-title + exact-year combination detector.
 *
 * <p>The acceptance scenario for the task is:
 * "Top chip company + unique title + exact year + chip code name"
 * must be generalized or blocked before client display.
 *
 * <p>This rule isolates the "unique title + exact year" half of that
 * conjunction and lets the surrounding pipeline combine its result with
 * the company-name and chip-code-name detectors to reach the final
 * decision.
 *
 * <p>"Rare titles" are those that, in a recruiting market, narrow a person
 * to a small handful of identifiable individuals at any single employer
 * — chief architect, principal staff, founding engineer, head of, lead
 * of, etc. Being the "Senior Software Engineer" of 2024 is not rare; being
 * the "Chief Verification Architect" of 2024 typically is.
 */
public final class RareTitleYearCombinationRiskRule {

  /**
   * Output of the rule. {@code matchedTitleSpan} is the text region that
   * matched the rare-title vocabulary; {@code matchedYear} is the four-digit
   * year string that matched in the same value.
   */
  public record Detection(
      boolean matched,
      String matchedTitleSpan,
      String matchedYear) {

    public Detection {
      if (matched) {
        Objects.requireNonNull(matchedTitleSpan, "matchedTitleSpan must not be null when matched");
        Objects.requireNonNull(matchedYear, "matchedYear must not be null when matched");
        if (matchedTitleSpan.isBlank() || matchedYear.isBlank()) {
          throw new IllegalArgumentException(
              "matched detection requires non-blank title span and year");
        }
      }
    }

    public static Detection notMatched() {
      return new Detection(false, "", "");
    }
  }

  private static final List<Pattern> RARE_TITLE_PATTERNS = List.of(
      // Top-of-org leadership patterns.
      Pattern.compile(
          "\\bChief\\s+[A-Z][A-Za-z\\-]+(?:\\s+[A-Z][A-Za-z\\-]+){0,3}\\b"),
      Pattern.compile(
          "\\b(?:Head|Lead|Director)\\s+of\\s+[A-Z][A-Za-z\\-]+"
              + "(?:\\s+[A-Z][A-Za-z\\-]+){0,3}\\b"),
      // Senior individual contributor / staff levels typical of small,
      // identifiable populations.
      Pattern.compile("\\bDistinguished\\s+[A-Z][A-Za-z\\-]+\\b"),
      Pattern.compile("\\bPrincipal(?:\\s+Staff)?\\s+[A-Z][A-Za-z\\-]+\\b"),
      Pattern.compile("\\bStaff(?:\\s+Engineer|\\s+Architect)?\\b"),
      Pattern.compile("\\bFellow\\b"),
      Pattern.compile("\\bChief\\s+[A-Z][A-Za-z\\-]+\\s+Officer\\b"),
      Pattern.compile("\\b(?:CTO|CEO|CFO|COO|CIO|CISO|CPO)\\b"),
      // Founding / founder titles.
      Pattern.compile("\\bFounding\\s+[A-Z][A-Za-z\\-]+\\b"),
      Pattern.compile("\\bFounder\\b|\\bCo[-\\s]?[Ff]ounder\\b"),
      // Verification / silicon-specific rare titles that the v1 industry
      // pack cares about.
      Pattern.compile("\\bChief\\s+Verification\\s+Architect\\b",
          Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bHead\\s+of\\s+Verification\\b",
          Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bDirector\\s+of\\s+Silicon\\b",
          Pattern.CASE_INSENSITIVE));

  private static final Pattern YEAR_PATTERN =
      Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");

  private RareTitleYearCombinationRiskRule() {
  }

  /**
   * Returns whether the given text contains both a rare title vocabulary
   * match and an exact four-digit year. Both must be present in the same
   * text value (i.e. the same sentence/paragraph the consultant emitted).
   */
  public static Detection detect(String text) {
    if (text == null || text.isBlank()) {
      return Detection.notMatched();
    }
    String matchedTitleSpan = findFirstRareTitleSpan(text);
    if (matchedTitleSpan == null) {
      return Detection.notMatched();
    }
    Matcher yearMatcher = YEAR_PATTERN.matcher(text);
    if (!yearMatcher.find()) {
      return Detection.notMatched();
    }
    return new Detection(true, matchedTitleSpan, yearMatcher.group(1));
  }

  /**
   * Convenience overload that runs the detector across multiple texts and
   * returns the first match. The pipeline iterates a candidate's projected
   * text values and runs this once per value.
   */
  public static Detection detect(List<String> texts) {
    if (texts == null || texts.isEmpty()) {
      return Detection.notMatched();
    }
    for (String text : texts) {
      Detection detection = detect(text);
      if (detection.matched()) {
        return detection;
      }
    }
    return Detection.notMatched();
  }

  static String findFirstRareTitleSpan(String text) {
    for (Pattern pattern : RARE_TITLE_PATTERNS) {
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) {
        return matcher.group();
      }
    }
    return null;
  }

  static String normalizeForLogging(String text) {
    if (text == null) {
      return "";
    }
    return text.strip().toLowerCase(Locale.ROOT);
  }
}
