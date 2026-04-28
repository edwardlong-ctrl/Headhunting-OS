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
        || lower.contains("com.recruitingtransactionos")
        || lower.contains("candidateprofile")
        || lower.contains("candidate profile")
        || lower.contains("sourceitem")
        || lower.contains("informationpacket")
        || lower.contains("claimledger")
        || lower.contains("reviewevent")
        || lower.contains("workflowevent")
        || lower.contains("stack trace")
        || lower.contains("stacktrace")
        || lower.contains("exception")
        || lower.contains("\tat ")
        || lower.contains("\n at ")
        || lower.contains("java.");
  }
}
