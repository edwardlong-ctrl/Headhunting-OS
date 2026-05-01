package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity;

public record AuthenticityRiskAssessorInput(
    String resumeText,
    String linkedInText,
    String portfolioText,
    String interviewNotes) {

  public AuthenticityRiskAssessorInput {
    if ((resumeText == null || resumeText.isBlank())
        && (linkedInText == null || linkedInText.isBlank())
        && (portfolioText == null || portfolioText.isBlank())
        && (interviewNotes == null || interviewNotes.isBlank())) {
      throw new IllegalArgumentException("authenticity_risk_input_requires_content");
    }
  }
}
