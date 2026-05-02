package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake;

public record JobIntakeInput(
    String sourceSummary,
    String jobDescriptionText,
    String hiringManagerBrief,
    String consultantNotes) {

  public JobIntakeInput {
    requireAtLeastOneValue(sourceSummary, jobDescriptionText, hiringManagerBrief, consultantNotes);
  }

  private static void requireAtLeastOneValue(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return;
      }
    }
    throw new IllegalArgumentException("job_intake_input_requires_content");
  }
}
