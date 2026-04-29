package com.recruitingtransactionos.coreapi.candidateprofile;

import java.util.Set;
import java.util.regex.Pattern;

public record CandidateProfileFieldPath(String value) {

  private static final Pattern PATH_CHARACTERS = Pattern.compile("^[a-z0-9_.]+$");
  private static final Pattern STABLE_PATH =
      Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");

  public static final CandidateProfileFieldPath IDENTITY_FULL_NAME = of("identity.full_name");
  public static final CandidateProfileFieldPath IDENTITY_PREFERRED_NAME =
      of("identity.preferred_name");
  public static final CandidateProfileFieldPath CONTACT_EMAIL = of("contact.email");
  public static final CandidateProfileFieldPath CONTACT_PHONE = of("contact.phone");
  public static final CandidateProfileFieldPath LOCATION_CURRENT_LOCATION =
      of("location.current_location");
  public static final CandidateProfileFieldPath LOCATION_PREFERRED_LOCATIONS =
      of("location.preferred_locations");
  public static final CandidateProfileFieldPath COMPENSATION_CURRENT_SALARY =
      of("compensation.current_salary");
  public static final CandidateProfileFieldPath COMPENSATION_EXPECTED_SALARY =
      of("compensation.expected_salary");
  public static final CandidateProfileFieldPath AVAILABILITY_NOTICE_PERIOD =
      of("availability.notice_period");
  public static final CandidateProfileFieldPath AVAILABILITY_AVAILABLE_FROM =
      of("availability.available_from");
  public static final CandidateProfileFieldPath EXPERIENCE_CURRENT_COMPANY =
      of("experience.current_company");
  public static final CandidateProfileFieldPath EXPERIENCE_CURRENT_TITLE =
      of("experience.current_title");
  public static final CandidateProfileFieldPath EXPERIENCE_YEARS_OF_EXPERIENCE =
      of("experience.years_of_experience");
  public static final CandidateProfileFieldPath EXPERIENCE_WORK_HISTORY =
      of("experience.work_history");
  public static final CandidateProfileFieldPath SKILLS_PRIMARY_SKILLS =
      of("skills.primary_skills");
  public static final CandidateProfileFieldPath SKILLS_SECONDARY_SKILLS =
      of("skills.secondary_skills");
  public static final CandidateProfileFieldPath EDUCATION_HIGHEST_DEGREE =
      of("education.highest_degree");
  public static final CandidateProfileFieldPath EDUCATION_SCHOOLS = of("education.schools");
  public static final CandidateProfileFieldPath INTENT_OPEN_TO_OPPORTUNITIES =
      of("intent.open_to_opportunities");
  public static final CandidateProfileFieldPath INTENT_INTEREST_LEVEL =
      of("intent.interest_level");
  public static final CandidateProfileFieldPath CONSENT_LATEST_PROFILE_VERSION =
      of("consent.latest_profile_version");
  public static final CandidateProfileFieldPath METADATA_NOTES = of("metadata.notes");
  public static final CandidateProfileFieldPath IDENTITY_CITIZENSHIP =
      of("identity.citizenship");
  public static final CandidateProfileFieldPath EXPERIENCE_PROJECTS =
      of("experience.projects");
  public static final CandidateProfileFieldPath EXPERIENCE_PORTFOLIO =
      of("experience.portfolio");
  public static final CandidateProfileFieldPath EXPERIENCE_INDUSTRY =
      of("experience.industry");
  public static final CandidateProfileFieldPath INTENT_MOTIVATION_TOWARD_OPPORTUNITY =
      of("intent.motivation_toward_opportunity");

  private static final Set<String> INITIAL_PATH_VALUES = Set.of(
      IDENTITY_FULL_NAME.value(),
      IDENTITY_PREFERRED_NAME.value(),
      IDENTITY_CITIZENSHIP.value(),
      CONTACT_EMAIL.value(),
      CONTACT_PHONE.value(),
      LOCATION_CURRENT_LOCATION.value(),
      LOCATION_PREFERRED_LOCATIONS.value(),
      COMPENSATION_CURRENT_SALARY.value(),
      COMPENSATION_EXPECTED_SALARY.value(),
      AVAILABILITY_NOTICE_PERIOD.value(),
      AVAILABILITY_AVAILABLE_FROM.value(),
      EXPERIENCE_CURRENT_COMPANY.value(),
      EXPERIENCE_CURRENT_TITLE.value(),
      EXPERIENCE_YEARS_OF_EXPERIENCE.value(),
      EXPERIENCE_WORK_HISTORY.value(),
      EXPERIENCE_PROJECTS.value(),
      EXPERIENCE_PORTFOLIO.value(),
      EXPERIENCE_INDUSTRY.value(),
      SKILLS_PRIMARY_SKILLS.value(),
      SKILLS_SECONDARY_SKILLS.value(),
      EDUCATION_HIGHEST_DEGREE.value(),
      EDUCATION_SCHOOLS.value(),
      INTENT_OPEN_TO_OPPORTUNITIES.value(),
      INTENT_INTEREST_LEVEL.value(),
      INTENT_MOTIVATION_TOWARD_OPPORTUNITY.value(),
      CONSENT_LATEST_PROFILE_VERSION.value(),
      METADATA_NOTES.value());

  public CandidateProfileFieldPath {
    value = CandidateProfileGuards.requireNonBlank(value, "fieldPath");
    if (!PATH_CHARACTERS.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "fieldPath must be a stable dotted lower_snake_case path");
    }
    if (!value.contains(".")) {
      throw new IllegalArgumentException("fieldPath must include at least one namespace segment");
    }
    if (!STABLE_PATH.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "fieldPath must be a stable dotted lower_snake_case path");
    }
  }

  public static CandidateProfileFieldPath of(String value) {
    return new CandidateProfileFieldPath(value);
  }

  public static Set<String> initialPathValues() {
    return INITIAL_PATH_VALUES;
  }
}
