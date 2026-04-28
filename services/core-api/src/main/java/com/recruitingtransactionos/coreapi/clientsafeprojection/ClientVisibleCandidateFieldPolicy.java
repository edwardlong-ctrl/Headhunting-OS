package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ClientVisibleCandidateFieldPolicy {

  private static final Set<String> SAFE_ALLOWLISTED_FIELD_PATHS = Set.of(
      "anonymous.card_id",
      "anonymous.candidate_ref",
      "projection.version",
      "redaction.level",
      "profile.generalized_headline",
      "profile.generalized_role_family",
      "profile.generalized_seniority_band",
      "profile.generalized_location_region",
      "summary.safe_summary",
      "summary.safe_skill_summary",
      "summary.safe_evidence_placeholders",
      "summary.safe_match_narrative_placeholders");

  private static final Map<String, ForbiddenClientCandidateFieldCategory> FORBIDDEN_FIELD_PATHS =
      Map.ofEntries(
          forbidden("full_name", ForbiddenClientCandidateFieldCategory.DIRECT_IDENTITY),
          forbidden("identity.full_name", ForbiddenClientCandidateFieldCategory.DIRECT_IDENTITY),
          forbidden("legal_name", ForbiddenClientCandidateFieldCategory.DIRECT_IDENTITY),
          forbidden("identity.legal_name", ForbiddenClientCandidateFieldCategory.DIRECT_IDENTITY),
          forbidden("preferred_name", ForbiddenClientCandidateFieldCategory.DIRECT_IDENTITY),
          forbidden(
              "identity.preferred_name",
              ForbiddenClientCandidateFieldCategory.DIRECT_IDENTITY),
          forbidden(
              "identifying_preferred_name",
              ForbiddenClientCandidateFieldCategory.DIRECT_IDENTITY),
          forbidden("email", ForbiddenClientCandidateFieldCategory.CONTACT),
          forbidden("contact.email", ForbiddenClientCandidateFieldCategory.CONTACT),
          forbidden("phone", ForbiddenClientCandidateFieldCategory.CONTACT),
          forbidden("contact.phone", ForbiddenClientCandidateFieldCategory.CONTACT),
          forbidden("wechat", ForbiddenClientCandidateFieldCategory.PERSONAL_MESSAGING),
          forbidden("contact.wechat", ForbiddenClientCandidateFieldCategory.PERSONAL_MESSAGING),
          forbidden("whatsapp", ForbiddenClientCandidateFieldCategory.PERSONAL_MESSAGING),
          forbidden("contact.whatsapp", ForbiddenClientCandidateFieldCategory.PERSONAL_MESSAGING),
          forbidden(
              "personal_messaging_handle",
              ForbiddenClientCandidateFieldCategory.PERSONAL_MESSAGING),
          forbidden("exact_address", ForbiddenClientCandidateFieldCategory.EXACT_LOCATION),
          forbidden("location.exact_address", ForbiddenClientCandidateFieldCategory.EXACT_LOCATION),
          forbidden("linkedin_url", ForbiddenClientCandidateFieldCategory.IDENTITY_REVEALING_URL),
          forbidden(
              "profile.linkedin_url",
              ForbiddenClientCandidateFieldCategory.IDENTITY_REVEALING_URL),
          forbidden("github_url", ForbiddenClientCandidateFieldCategory.IDENTITY_REVEALING_URL),
          forbidden("portfolio_url", ForbiddenClientCandidateFieldCategory.IDENTITY_REVEALING_URL),
          forbidden(
              "personal_website_url",
              ForbiddenClientCandidateFieldCategory.IDENTITY_REVEALING_URL),
          forbidden("profile_url", ForbiddenClientCandidateFieldCategory.IDENTITY_REVEALING_URL),
          forbidden(
              "contact.profile_url",
              ForbiddenClientCandidateFieldCategory.IDENTITY_REVEALING_URL),
          forbidden("resume_url", ForbiddenClientCandidateFieldCategory.RAW_DOCUMENT),
          forbidden("raw_document_url", ForbiddenClientCandidateFieldCategory.RAW_DOCUMENT),
          forbidden("raw_candidate_id", ForbiddenClientCandidateFieldCategory.RAW_BACKEND_IDENTIFIER),
          forbidden("candidate_id", ForbiddenClientCandidateFieldCategory.RAW_BACKEND_IDENTIFIER),
          forbidden(
              "raw_candidate_profile_id",
              ForbiddenClientCandidateFieldCategory.RAW_BACKEND_IDENTIFIER),
          forbidden(
              "candidate_profile_id",
              ForbiddenClientCandidateFieldCategory.RAW_BACKEND_IDENTIFIER),
          forbidden("source_item_id", ForbiddenClientCandidateFieldCategory.RAW_BACKEND_IDENTIFIER),
          forbidden(
              "information_packet_id",
              ForbiddenClientCandidateFieldCategory.RAW_BACKEND_IDENTIFIER),
          forbidden("raw_source_reference", ForbiddenClientCandidateFieldCategory.RAW_SOURCE_REFERENCE),
          forbidden("raw_source_references", ForbiddenClientCandidateFieldCategory.RAW_SOURCE_REFERENCE),
          forbidden("raw_source_text", ForbiddenClientCandidateFieldCategory.RAW_SOURCE_CONTENT),
          forbidden("raw_cv_text", ForbiddenClientCandidateFieldCategory.RAW_SOURCE_CONTENT),
          forbidden("raw_consultant_notes", ForbiddenClientCandidateFieldCategory.CONSULTANT_INTERNAL),
          forbidden(
              "consultant_internal_notes",
              ForbiddenClientCandidateFieldCategory.CONSULTANT_INTERNAL),
          forbidden(
              "other_client_interaction_history",
              ForbiddenClientCandidateFieldCategory.OTHER_CLIENT_INTERACTION),
          forbidden(
              "sensitive_compensation_bottom_line",
              ForbiddenClientCandidateFieldCategory.COMPENSATION_AND_NEGOTIATION),
          forbidden(
              "compensation.sensitive_bottom_line",
              ForbiddenClientCandidateFieldCategory.COMPENSATION_AND_NEGOTIATION),
          forbidden(
              "negotiation_notes",
              ForbiddenClientCandidateFieldCategory.COMPENSATION_AND_NEGOTIATION),
          forbidden(
              "consent_internal_audit_records",
              ForbiddenClientCandidateFieldCategory.INTERNAL_AUDIT_RECORD),
          forbidden(
              "disclosure_internal_audit_records",
              ForbiddenClientCandidateFieldCategory.INTERNAL_AUDIT_RECORD),
          forbidden("exact_location", ForbiddenClientCandidateFieldCategory.EXACT_LOCATION),
          forbidden("address", ForbiddenClientCandidateFieldCategory.EXACT_LOCATION),
          forbidden("location.address", ForbiddenClientCandidateFieldCategory.EXACT_LOCATION),
          forbidden(
              "exact_current_employer",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "experience.exact_current_employer",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "experience.current_employer",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "uniquely_identifying_project_name",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "uniquely_identifying_project_details",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "exact_project_product_chip_code_name",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "project.chip_code_name",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "project.product_code_name",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "chip_product_code_name",
              ForbiddenClientCandidateFieldCategory.EMPLOYER_OR_PROJECT_IDENTIFIER),
          forbidden(
              "patent_identifier",
              ForbiddenClientCandidateFieldCategory.PUBLIC_IDENTIFIER_BEFORE_CONSENT),
          forbidden(
              "paper_identifier",
              ForbiddenClientCandidateFieldCategory.PUBLIC_IDENTIFIER_BEFORE_CONSENT),
          forbidden(
              "public_talk_identifier",
              ForbiddenClientCandidateFieldCategory.PUBLIC_IDENTIFIER_BEFORE_CONSENT),
          forbidden(
              "open_source_identifier",
              ForbiddenClientCandidateFieldCategory.PUBLIC_IDENTIFIER_BEFORE_CONSENT),
          forbidden(
              "rare_title_exact_year_exact_company",
              ForbiddenClientCandidateFieldCategory.PRECISE_RARE_COMBINATION),
          forbidden(
              "exact_company_rare_title_exact_year",
              ForbiddenClientCandidateFieldCategory.PRECISE_RARE_COMBINATION),
          forbidden(
              "small_team_unique_ownership_claim",
              ForbiddenClientCandidateFieldCategory.SMALL_TEAM_UNIQUE_OWNERSHIP_CLAIM),
          forbidden(
              "unique_ownership_claim",
              ForbiddenClientCandidateFieldCategory.SMALL_TEAM_UNIQUE_OWNERSHIP_CLAIM),
          forbidden(
              "overly_specific_achievement_number",
              ForbiddenClientCandidateFieldCategory.IDENTIFYING_ACHIEVEMENT_NUMBER),
          forbidden(
              "achievement.exact_number",
              ForbiddenClientCandidateFieldCategory.IDENTIFYING_ACHIEVEMENT_NUMBER));

  private ClientVisibleCandidateFieldPolicy() {
  }

  public static Decision decide(String fieldPath) {
    String normalized = normalizeFieldPath(fieldPath);
    ForbiddenClientCandidateFieldCategory forbiddenCategory = FORBIDDEN_FIELD_PATHS.get(normalized);
    if (forbiddenCategory != null) {
      return new Decision(
          false,
          "forbidden_client_visible_candidate_field",
          normalized,
          forbiddenCategory);
    }
    if (SAFE_ALLOWLISTED_FIELD_PATHS.contains(normalized)) {
      return new Decision(true, "safe_allowlisted_client_projection_field", normalized, null);
    }
    return new Decision(false, "unknown_field_denied_by_default", normalized, null);
  }

  public static boolean isForbidden(String fieldPath) {
    return FORBIDDEN_FIELD_PATHS.containsKey(normalizeFieldPath(fieldPath));
  }

  public static boolean isAllowedForAnonymousClientProjection(String fieldPath) {
    return decide(fieldPath).allowed();
  }

  public static Set<String> safeAllowlistedFieldPaths() {
    return SAFE_ALLOWLISTED_FIELD_PATHS;
  }

  public static Set<String> forbiddenFieldPaths() {
    return FORBIDDEN_FIELD_PATHS.keySet();
  }

  private static Map.Entry<String, ForbiddenClientCandidateFieldCategory> forbidden(
      String fieldPath,
      ForbiddenClientCandidateFieldCategory category) {
    return Map.entry(fieldPath, category);
  }

  private static String normalizeFieldPath(String fieldPath) {
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    String normalized = fieldPath.strip().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("fieldPath must not be blank");
    }
    return normalized.replace('-', '_');
  }

  public record Decision(
      boolean allowed,
      String reason,
      String normalizedFieldPath,
      ForbiddenClientCandidateFieldCategory forbiddenCategory) {

    public Decision {
      Objects.requireNonNull(reason, "reason must not be null");
      Objects.requireNonNull(normalizedFieldPath, "normalizedFieldPath must not be null");
    }
  }
}
