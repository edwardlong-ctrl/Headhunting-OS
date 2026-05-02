package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile;

import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.shared.AITaskClaimCandidate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record CandidateProfileParserOutput(
    String headline,
    String summary,
    List<String> primarySkills,
    List<String> projects,
    List<String> timelineHighlights,
    List<AITaskClaimCandidate> claimCandidates) {

  public CandidateProfileParserOutput {
    headline = requireNonBlank(headline, "headline");
    summary = requireNonBlank(summary, "summary");
    primarySkills = safeList(primarySkills, "primarySkills");
    projects = safeList(projects, "projects");
    timelineHighlights = safeList(timelineHighlights, "timelineHighlights");
    claimCandidates = safeClaimCandidates(claimCandidates);
  }

  private static final Set<String> ALLOWED_CLAIM_FIELDS = Set.of(
      "headline",
      "summary",
      "primary_skills",
      "projects",
      "timeline_highlights");

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static List<String> safeList(List<String> values, String name) {
    Objects.requireNonNull(values, name + " must not be null");
    List<String> copy = List.copyOf(values);
    copy.forEach(value -> {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(name + " must not contain blank values");
      }
    });
    return copy;
  }

  private static List<AITaskClaimCandidate> safeClaimCandidates(List<AITaskClaimCandidate> values) {
    Objects.requireNonNull(values, "claimCandidates must not be null");
    List<AITaskClaimCandidate> copy = List.copyOf(values);
    copy.forEach(value -> {
      Objects.requireNonNull(value, "claimCandidates must not contain null values");
      if (!ALLOWED_CLAIM_FIELDS.contains(value.fieldName())) {
        throw new IllegalArgumentException(
            "claimCandidates fieldName is not allowed: " + value.fieldName());
      }
    });
    return copy;
  }
}
