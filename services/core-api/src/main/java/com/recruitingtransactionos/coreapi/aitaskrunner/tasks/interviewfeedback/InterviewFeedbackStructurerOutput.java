package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback;

import java.util.List;
import java.util.Map;

public record InterviewFeedbackStructurerOutput(
    String structuredSummary,
    String outcomeLabel,
    String rejectReasonTaxonomy,
    String confidence,
    List<Suggestion> suggestions,
    List<String> evidence,
    Map<String, Object> calibrationSignal) {

  public record Suggestion(
      String scope,
      String suggestionType,
      String title,
      String rationale,
      String outcomeLabel,
      String rejectReasonTaxonomy,
      Map<String, Object> payload) {
  }
}
