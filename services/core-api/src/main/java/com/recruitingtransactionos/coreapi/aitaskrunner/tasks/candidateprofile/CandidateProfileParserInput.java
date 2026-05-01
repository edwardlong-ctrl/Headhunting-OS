package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile;

import java.util.Objects;

public record CandidateProfileParserInput(
    String sourceSummary,
    String resumeText,
    String linkedInText,
    String portfolioText,
    String consultantNotes) {

  public CandidateProfileParserInput {
    requireAtLeastOneValue(sourceSummary, resumeText, linkedInText, portfolioText, consultantNotes);
  }

  private static void requireAtLeastOneValue(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return;
      }
    }
    throw new IllegalArgumentException("candidate_profile_parser_input_requires_content");
  }
}
