package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ApiBoundaryContractRules {

  private static final Set<String> CLIENT_SAFE_CANDIDATE_CARD_RESPONSE_FIELDS =
      Set.of(
          "anonymousCardRef",
          "anonymousCandidateRef",
          "projectionVersion",
          "redactionLevel",
          "generalizedHeadline",
          "generalizedRoleFamily",
          "generalizedSeniorityBand",
          "generalizedLocationRegion",
          "safeSummary",
          "safeSkillSummary",
          "safeEvidenceSummaries",
          "safeMatchNarratives");

  private static final Set<String> ANONYMOUS_CLIENT_SAFE_REDACTION_LEVELS =
      Set.of(
          "l0_teaser",
          "l1_generalized",
          "l2_client_safe",
          "l3_consented_detail");

  private static final Pattern SAFE_REASON_CODE = Pattern.compile("[a-z0-9_.-]+");
  private static final Pattern UUID_PATTERN = Pattern.compile(
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern URL_PATTERN = Pattern.compile(
      "(?i)\\b(?:https?://|www\\.)\\S+");
  private static final Pattern PHONE_PATTERN = Pattern.compile(
      "(?<!\\w)\\+?\\d[\\d\\s().-]{7,}\\d(?!\\w)");
  private static final Pattern CAPITALIZED_IDENTITY_PHRASE = Pattern.compile(
      "\\b[A-Z][A-Za-z0-9]+(?:\\s+[A-Z][A-Za-z0-9]+){1,4}\\b");
  private static final Pattern CODE_NAME_PATTERN = Pattern.compile(
      "\\b[A-Z][A-Za-z]+-[A-Z0-9]+(?:\\s+[A-Z0-9]{2,})?\\b");

  private ApiBoundaryContractRules() {}

  public static boolean isAllowedClientSafeCandidateCardResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CLIENT_SAFE_CANDIDATE_CARD_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> clientSafeCandidateCardResponseFieldNames() {
    return new LinkedHashSet<>(CLIENT_SAFE_CANDIDATE_CARD_RESPONSE_FIELDS);
  }

  static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }

  static String requireAnonymousClientSafeRedactionLevel(String redactionLevel) {
    String normalized = requireNonBlank(redactionLevel, "redactionLevel").toLowerCase();
    if (!ANONYMOUS_CLIENT_SAFE_REDACTION_LEVELS.contains(normalized)) {
      throw new IllegalArgumentException(
          "redactionLevel must be an anonymous client-safe API level");
    }
    return normalized;
  }

  static List<String> copyNonBlankList(List<String> values, String fieldName) {
    Objects.requireNonNull(values, fieldName + " must not be null");
    return values.stream()
        .map(value -> requireNonBlank(value, fieldName + " item"))
        .toList();
  }

  static String requireApiSafeExternalText(String value, String fieldName) {
    String stripped = requireNonBlank(value, fieldName);
    if (containsInternalLeakage(stripped)) {
      throw new IllegalArgumentException(fieldName + " must not contain unsafe API-visible text");
    }
    return stripped;
  }

  static List<String> copyApiSafeExternalTextList(List<String> values, String fieldName) {
    Objects.requireNonNull(values, fieldName + " must not be null");
    return values.stream()
        .map(value -> requireApiSafeExternalText(value, fieldName + " item"))
        .toList();
  }

  static String sanitizeReasonCode(String reasonCode, String fallback) {
    if (reasonCode == null || reasonCode.isBlank()) {
      return fallback;
    }
    String normalized = reasonCode.strip().toLowerCase();
    if (!SAFE_REASON_CODE.matcher(normalized).matches()) {
      return fallback;
    }
    return normalized;
  }

  static String sanitizeExternalText(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String stripped = value.strip();
    if (containsInternalLeakage(stripped)) {
      return fallback;
    }
    return stripped;
  }

  private static boolean containsInternalLeakage(String value) {
    String lower = value.toLowerCase();
    return UUID_PATTERN.matcher(value).find()
        || EMAIL_PATTERN.matcher(value).find()
        || URL_PATTERN.matcher(value).find()
        || PHONE_PATTERN.matcher(value).find()
        || CAPITALIZED_IDENTITY_PHRASE.matcher(value).find()
        || CODE_NAME_PATTERN.matcher(value).find()
        || lower.contains("com.recruitingtransactionos")
        || lower.contains("candidateprofile")
        || lower.contains("candidate profile")
        || lower.contains("raw candidate")
        || lower.contains("raw profile")
        || lower.contains("sourceitem")
        || lower.contains("source item")
        || lower.contains("informationpacket")
        || lower.contains("information packet")
        || lower.contains("claimledger")
        || lower.contains("claim ledger")
        || lower.contains("reviewevent")
        || lower.contains("review event")
        || lower.contains("workflowevent")
        || lower.contains("workflow event")
        || lower.contains("consentrecord")
        || lower.contains("disclosurerecord")
        || lower.contains("linkedin")
        || lower.contains("raw source")
        || lower.contains("raw cv")
        || lower.contains("cv text")
        || lower.contains("consultant note")
        || lower.contains("consultant-only")
        || lower.contains("consultant internal")
        || lower.contains("current employer")
        || lower.contains("stack trace")
        || lower.contains("stacktrace")
        || lower.contains("exception")
        || lower.contains("\tat ")
        || lower.contains("\n at ")
        || lower.contains("java.");
  }
}
