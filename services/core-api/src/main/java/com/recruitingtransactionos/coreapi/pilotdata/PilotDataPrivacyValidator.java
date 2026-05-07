package com.recruitingtransactionos.coreapi.pilotdata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class PilotDataPrivacyValidator {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Set<String> ALLOWED_EMAIL_SUFFIXES = Set.of(
      ".example.test",
      "@candidate.example.test");
  private static final List<String> FORBIDDEN_REAL_COMPANY_NAMES = List.of(
      "intel",
      "nvidia",
      "amd",
      "qualcomm",
      "apple",
      "google",
      "meta",
      "tsmc",
      "samsung");
  private static final Pattern PUBLIC_PROFILE_URL = Pattern.compile(
      "https?://(www\\.)?(linkedin\\.com|github\\.com|x\\.com|twitter\\.com)/",
      Pattern.CASE_INSENSITIVE);

  public PilotDataValidationResult validate(PilotDataset dataset) {
    List<PilotDataValidationResult.Issue> issues = new ArrayList<>();
    dataset.accounts().forEach(account -> validateEmail(account.email(), account.userAccountId().toString(), issues));
    dataset.candidates().forEach(candidate -> {
      validateEmail(candidate.email(), candidate.candidateId(), issues);
      validateBody(candidate.summary(), candidate.candidateId(), issues);
      validateBody(String.join(" ", candidate.skills()), candidate.candidateId(), issues);
    });
    dataset.sourceDocuments().forEach(sourceDocument ->
        validateBody(sourceDocument.body(), sourceDocument.documentRef(), issues));
    return new PilotDataValidationResult(issues.isEmpty(), issues);
  }

  public List<PilotDataValidationResult.Issue> validateCandidateProfileFields(
      String ref,
      List<CandidateProfileField> fields) {
    Objects.requireNonNull(ref, "ref must not be null");
    Objects.requireNonNull(fields, "fields must not be null");
    List<PilotDataValidationResult.Issue> issues = new ArrayList<>();
    for (CandidateProfileField field : fields) {
      String fieldRef = ref + ":" + field.fieldPath().value();
      for (String value : textValues(field.value().jsonValue())) {
        if (CandidateProfileFieldPath.CONTACT_EMAIL.equals(field.fieldPath())) {
          validateEmail(value, fieldRef, issues, "candidate_profile_unsupported_email_domain");
        }
        validateBody(
            value,
            fieldRef,
            issues,
            "candidate_profile_public_profile_url_forbidden",
            "candidate_profile_real_company_name_forbidden");
      }
    }
    return List.copyOf(issues);
  }

  private static void validateEmail(
      String email,
      String ref,
      List<PilotDataValidationResult.Issue> issues) {
    validateEmail(email, ref, issues, "unsupported_email_domain");
  }

  private static void validateEmail(
      String email,
      String ref,
      List<PilotDataValidationResult.Issue> issues,
      String code) {
    boolean allowed = ALLOWED_EMAIL_SUFFIXES.stream().anyMatch(email::endsWith);
    if (!allowed) {
      issues.add(new PilotDataValidationResult.Issue(
          code,
          "Pilot data must use reserved .example.test email domains.",
          ref));
    }
  }

  private static void validateBody(
      String body,
      String ref,
      List<PilotDataValidationResult.Issue> issues) {
    validateBody(
        body,
        ref,
        issues,
        "public_profile_url_forbidden",
        "real_company_name_forbidden");
  }

  private static void validateBody(
      String body,
      String ref,
      List<PilotDataValidationResult.Issue> issues,
      String publicProfileUrlCode,
      String realCompanyNameCode) {
    String normalized = body.toLowerCase(Locale.ROOT);
    if (PUBLIC_PROFILE_URL.matcher(body).find()) {
      issues.add(new PilotDataValidationResult.Issue(
          publicProfileUrlCode,
          "Pilot data must not include public profile URLs.",
          ref));
    }
    FORBIDDEN_REAL_COMPANY_NAMES.stream()
        .filter(company -> normalized.contains(company))
        .findFirst()
        .ifPresent(company -> issues.add(new PilotDataValidationResult.Issue(
            realCompanyNameCode,
            "Pilot data must not include real high-signal company names.",
            ref)));
  }

  private static List<String> textValues(String jsonValue) {
    List<String> values = new ArrayList<>();
    try {
      collectTextValues(OBJECT_MAPPER.readTree(jsonValue), values);
    } catch (JsonProcessingException exception) {
      values.add(jsonValue);
    }
    return List.copyOf(values);
  }

  private static void collectTextValues(JsonNode node, List<String> values) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isTextual()) {
      values.add(node.asText());
      return;
    }
    if (node.isArray() || node.isObject()) {
      node.elements().forEachRemaining(child -> collectTextValues(child, values));
    }
  }
}
