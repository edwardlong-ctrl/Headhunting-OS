package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile;

import java.util.List;
import java.util.Objects;

public record CandidateProfileParserOutput(
    String headline,
    String summary,
    List<String> primarySkills,
    List<String> projects,
    List<String> timelineHighlights) {

  public CandidateProfileParserOutput {
    headline = requireNonBlank(headline, "headline");
    summary = requireNonBlank(summary, "summary");
    primarySkills = safeList(primarySkills, "primarySkills");
    projects = safeList(projects, "projects");
    timelineHighlights = safeList(timelineHighlights, "timelineHighlights");
  }

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
}
