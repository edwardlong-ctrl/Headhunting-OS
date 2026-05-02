package com.recruitingtransactionos.coreapi.industrypack;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record IndustryRoleFamilyTemplate(
    UUID templateId,
    IndustryPackId industryPackId,
    UUID ontologyVersionId,
    String roleFamily,
    String displayName,
    String scorecardDimensions,
    String scoringGuidance,
    List<String> interviewQuestionTemplates,
    List<String> evidenceExamples,
    List<String> antiPatterns,
    List<String> requiredSkillKeys) {

  public IndustryRoleFamilyTemplate {
    Objects.requireNonNull(templateId, "templateId must not be null");
    Objects.requireNonNull(industryPackId, "industryPackId must not be null");
    Objects.requireNonNull(ontologyVersionId, "ontologyVersionId must not be null");
    roleFamily = requireNonBlank(roleFamily, "roleFamily");
    displayName = requireNonBlank(displayName, "displayName");
    scorecardDimensions = requireNonBlank(scorecardDimensions, "scorecardDimensions");
    scoringGuidance = requireNonBlank(scoringGuidance, "scoringGuidance");
    interviewQuestionTemplates = List.copyOf(interviewQuestionTemplates == null ? List.of() : interviewQuestionTemplates);
    evidenceExamples = List.copyOf(evidenceExamples == null ? List.of() : evidenceExamples);
    antiPatterns = List.copyOf(antiPatterns == null ? List.of() : antiPatterns);
    requiredSkillKeys = List.copyOf(requiredSkillKeys == null ? List.of() : requiredSkillKeys);
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    String normalized = value.strip();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
