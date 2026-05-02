package com.recruitingtransactionos.coreapi.job.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class JobActivationGateService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  public JobActivationGateResult evaluate(
      Job job,
      List<JobRequirement> requirements,
      Optional<JobScorecard> scorecard) {
    Objects.requireNonNull(job, "job must not be null");
    Objects.requireNonNull(requirements, "requirements must not be null");
    Objects.requireNonNull(scorecard, "scorecard must not be null");

    List<String> clarificationQuestions = new ArrayList<>();
    if (isBlank(job.description())) {
      clarificationQuestions.add("What business problem is this hire solving?");
    }
    if (isBlank(job.location())) {
      clarificationQuestions.add("Which location or hiring region should be used for this role?");
    }
    if (isBlank(job.compensation())) {
      clarificationQuestions.add("What compensation range can be shared safely at intake?");
    }
    if (isBlank(job.seniorityBand())) {
      clarificationQuestions.add("What seniority band should the consultant confirm?");
    }

    clarificationQuestions.addAll(savedClarificationQuestions(job.metadata()));

    boolean hasRequirements = !requirements.isEmpty();
    boolean hasScorecard = scorecard.isPresent()
        && !isBlank(scorecard.get().dimensions());
    boolean hasCommercialTermsPlaceholder = hasCommercialTermsPlaceholder(job.commercialTerms());

    List<String> blockerReasons = new ArrayList<>();
    if (isBlank(job.description())) {
      blockerReasons.add("job_description_missing");
    }
    if (!hasRequirements) {
      blockerReasons.add("job_requirements_missing");
    }
    if (!hasScorecard) {
      blockerReasons.add("job_scorecard_missing");
    }
    if (!hasCommercialTermsPlaceholder) {
      blockerReasons.add("job_commercial_terms_placeholder_missing");
    }
    if (isBlank(job.location())) {
      blockerReasons.add("job_location_missing");
    }

    return new JobActivationGateResult(
        blockerReasons.isEmpty(),
        clarificationQuestions.stream().distinct().toList(),
        blockerReasons,
        hasScorecard,
        hasRequirements,
        hasCommercialTermsPlaceholder);
  }

  private static List<String> savedClarificationQuestions(String metadataJson) {
    if (isBlank(metadataJson)) {
      return List.of();
    }
    try {
      Map<String, Object> metadata = OBJECT_MAPPER.readValue(metadataJson, MAP_TYPE);
      Object value = metadata.get("clarificationQuestions");
      if (!(value instanceof List<?> list)) {
        return List.of();
      }
      List<String> result = new ArrayList<>();
      for (Object item : list) {
        if (item instanceof String text && !text.isBlank()) {
          result.add(text.strip());
        }
      }
      return result;
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private static boolean hasCommercialTermsPlaceholder(String commercialTerms) {
    if (isBlank(commercialTerms)) {
      return false;
    }
    try {
      Map<String, Object> payload = OBJECT_MAPPER.readValue(commercialTerms, MAP_TYPE);
      return hasText(payload.get("feeModel"))
          && hasText(payload.get("feeRangeOrRate"))
          && hasText(payload.get("paymentTerms"))
          && hasText(payload.get("contractStatus"));
    } catch (Exception ignored) {
      return !commercialTerms.isBlank();
    }
  }

  private static boolean hasText(Object value) {
    return value instanceof String text && !text.isBlank();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
