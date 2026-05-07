package com.recruitingtransactionos.coreapi.pilotdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class PilotDataPrivacyValidator {

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

  private static void validateEmail(
      String email,
      String ref,
      List<PilotDataValidationResult.Issue> issues) {
    boolean allowed = ALLOWED_EMAIL_SUFFIXES.stream().anyMatch(email::endsWith);
    if (!allowed) {
      issues.add(new PilotDataValidationResult.Issue(
          "unsupported_email_domain",
          "Pilot data must use reserved .example.test email domains.",
          ref));
    }
  }

  private static void validateBody(
      String body,
      String ref,
      List<PilotDataValidationResult.Issue> issues) {
    String normalized = body.toLowerCase(Locale.ROOT);
    if (PUBLIC_PROFILE_URL.matcher(body).find()) {
      issues.add(new PilotDataValidationResult.Issue(
          "public_profile_url_forbidden",
          "Pilot data must not include public profile URLs.",
          ref));
    }
    FORBIDDEN_REAL_COMPANY_NAMES.stream()
        .filter(company -> normalized.contains(company))
        .findFirst()
        .ifPresent(company -> issues.add(new PilotDataValidationResult.Issue(
            "real_company_name_forbidden",
            "Pilot data must not include real high-signal company names.",
            ref)));
  }
}
